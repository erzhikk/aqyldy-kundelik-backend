package kz.aqyldykundelik.schedule.service

import kz.aqyldykundelik.assignments.repo.ClassSubjectTeacherRepository
import kz.aqyldykundelik.classes.repo.ClassRepository
import kz.aqyldykundelik.classlevels.repo.ClassLevelRepository
import kz.aqyldykundelik.curriculum.repo.CurriculumSubjectHoursRepository
import kz.aqyldykundelik.schedule.api.dto.*
import kz.aqyldykundelik.schedule.domain.ClassScheduleEntity
import kz.aqyldykundelik.schedule.domain.ClassScheduleLessonEntity
import kz.aqyldykundelik.schedule.repo.ClassScheduleLessonRepository
import kz.aqyldykundelik.schedule.repo.ClassScheduleRepository
import kz.aqyldykundelik.timetable.repo.SubjectRepository
import kz.aqyldykundelik.users.repo.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.*

@Service
class ScheduleService(
    private val classRepository: ClassRepository,
    private val classLevelRepository: ClassLevelRepository,
    private val classScheduleRepository: ClassScheduleRepository,
    private val classScheduleLessonRepository: ClassScheduleLessonRepository,
    private val classSubjectTeacherRepository: ClassSubjectTeacherRepository,
    private val curriculumSubjectHoursRepository: CurriculumSubjectHoursRepository,
    private val subjectRepository: SubjectRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun getClassSchedule(classId: UUID): ClassScheduleDto {
        if (!classRepository.existsById(classId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found")
        }

        var schedule = classScheduleRepository.findByClassId(classId)
        if (schedule == null) {
            schedule = ClassScheduleEntity(classId = classId)
            schedule = classScheduleRepository.save(schedule)
        }

        return buildScheduleDto(classId, schedule)
    }

    @Transactional
    fun updateClassSchedule(classId: UUID, updateDto: UpdateClassScheduleDto): ClassScheduleDto {
        if (!classRepository.existsById(classId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found")
        }

        var schedule = classScheduleRepository.findByClassId(classId)
        if (schedule == null) {
            schedule = ClassScheduleEntity(classId = classId)
        }

        schedule.daysPerWeek = updateDto.daysPerWeek
        schedule.lessonsPerDay = updateDto.lessonsPerDay
        schedule = classScheduleRepository.save(schedule)

        classScheduleLessonRepository.deleteByScheduleId(schedule.id!!)

        val teacherAssignments = classSubjectTeacherRepository.findByClassId(classId)
            .associateBy { it.subjectId }

        for (lessonDto in updateDto.grid) {
            if (lessonDto.subjectId == null) continue

            val teacherId = teacherAssignments[lessonDto.subjectId]?.teacherId

            val lesson = ClassScheduleLessonEntity(
                scheduleId = schedule.id,
                dayOfWeek = lessonDto.dayOfWeek,
                lessonNumber = lessonDto.lessonNumber,
                subjectId = lessonDto.subjectId,
                teacherId = teacherId
            )
            classScheduleLessonRepository.save(lesson)
        }

        return buildScheduleDto(classId, schedule)
    }

    @Transactional
    fun activateSchedule(classId: UUID): ClassScheduleDto {
        val schedule = classScheduleRepository.findByClassId(classId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found")

        schedule.status = ClassScheduleEntity.STATUS_ACTIVE
        classScheduleRepository.save(schedule)

        return buildScheduleDto(classId, schedule)
    }

    private fun buildScheduleDto(classId: UUID, schedule: ClassScheduleEntity): ClassScheduleDto {
        val lessons = classScheduleLessonRepository.findByScheduleId(schedule.id!!)

        val subjectIds = lessons.mapNotNull { it.subjectId }.toSet()
        val teacherIds = lessons.mapNotNull { it.teacherId }.toSet()

        val subjectsMap = if (subjectIds.isNotEmpty()) {
            subjectRepository.findAllById(subjectIds).associateBy { it.id }
        } else {
            emptyMap()
        }

        val teachersMap = if (teacherIds.isNotEmpty()) {
            userRepository.findAllById(teacherIds).associateBy { it.id }
        } else {
            emptyMap()
        }

        val grid = lessons.map { lesson ->
            val subject = lesson.subjectId?.let { subjectsMap[it] }
            val teacher = lesson.teacherId?.let { teachersMap[it] }

            ScheduleLessonDto(
                dayOfWeek = lesson.dayOfWeek,
                lessonNumber = lesson.lessonNumber,
                subjectId = lesson.subjectId,
                subjectNameRu = subject?.nameRu,
                teacherId = lesson.teacherId,
                teacherFullName = teacher?.fullName
            )
        }.sortedWith(compareBy({ it.dayOfWeek }, { it.lessonNumber }))

        val conflicts = detectConflicts(classId, schedule, lessons, subjectsMap, teachersMap)

        return ClassScheduleDto(
            scheduleId = schedule.id!!,
            classId = classId,
            status = schedule.status,
            daysPerWeek = schedule.daysPerWeek,
            lessonsPerDay = schedule.lessonsPerDay,
            grid = grid,
            conflicts = conflicts
        )
    }

    @Suppress("UNUSED_PARAMETER")
    private fun detectConflicts(
        classId: UUID,
        schedule: ClassScheduleEntity,
        lessons: List<ClassScheduleLessonEntity>,
        subjectsMap: Map<UUID?, kz.aqyldykundelik.timetable.domain.SubjectEntity>,
        teachersMap: Map<UUID?, kz.aqyldykundelik.users.domain.UserEntity>
    ): List<ScheduleConflictDto> {
        val conflicts = mutableListOf<ScheduleConflictDto>()

        for (lesson in lessons) {
            if (lesson.teacherId != null) {
                val conflicting = classScheduleLessonRepository.findConflictingLessons(
                    teacherId = lesson.teacherId!!,
                    dayOfWeek = lesson.dayOfWeek,
                    lessonNumber = lesson.lessonNumber,
                    excludeClassId = classId
                )

                if (conflicting.isNotEmpty()) {
                    val teacher = teachersMap[lesson.teacherId]
                    conflicts.add(
                        ScheduleConflictDto(
                            type = ConflictType.TEACHER_BUSY,
                            teacherId = lesson.teacherId,
                            dayOfWeek = lesson.dayOfWeek,
                            lessonNumber = lesson.lessonNumber,
                            message = "Учитель ${teacher?.fullName ?: ""} занят в другом классе"
                        )
                    )
                }
            }
        }

        val schoolClass = classRepository.findById(classId).orElse(null)
        val classLevelId = schoolClass?.classLevelId
        if (classLevelId != null) {
            val classLevel = classLevelRepository.findById(classLevelId).orElse(null)
            val maxLessonsPerDay = classLevel?.maxLessonsPerDay ?: 7

            // Check max lessons per day
            val lessonsPerDay = lessons
                .filter { it.subjectId != null }
                .groupBy { it.dayOfWeek }
                .mapValues { it.value.size }

            for ((dayOfWeek, count) in lessonsPerDay) {
                if (count > maxLessonsPerDay) {
                    val dayName = when (dayOfWeek) {
                        1 -> "Понедельник"
                        2 -> "Вторник"
                        3 -> "Среда"
                        4 -> "Четверг"
                        5 -> "Пятница"
                        6 -> "Суббота"
                        7 -> "Воскресенье"
                        else -> "День $dayOfWeek"
                    }
                    conflicts.add(
                        ScheduleConflictDto(
                            type = ConflictType.MAX_LESSONS_EXCEEDED,
                            teacherId = null,
                            dayOfWeek = dayOfWeek,
                            lessonNumber = 0,
                            message = "$dayName: $count уроков (максимум $maxLessonsPerDay для ${classLevel?.level ?: ""} класса)"
                        )
                    )
                }
            }

            // Check hours mismatch
            val curriculumHours = curriculumSubjectHoursRepository.findByClassLevelId(classLevelId)
                .associateBy { it.subjectId }

            val actualHours = lessons
                .filter { it.subjectId != null }
                .groupBy { it.subjectId }
                .mapValues { it.value.size }

            for ((subjectId, expected) in curriculumHours) {
                val actual = actualHours[subjectId] ?: 0
                if (actual != expected.hoursPerWeek && expected.hoursPerWeek > 0) {
                    val subject = subjectsMap[subjectId]
                    conflicts.add(
                        ScheduleConflictDto(
                            type = ConflictType.HOURS_MISMATCH,
                            teacherId = null,
                            dayOfWeek = 0,
                            lessonNumber = 0,
                            message = "${subject?.nameRu ?: "Предмет"}: ожидается ${expected.hoursPerWeek} ч/нед, фактически $actual"
                        )
                    )
                }
            }
        }

        return conflicts
    }
}
