package kz.aqyldykundelik.schedule.service

import kz.aqyldykundelik.assignments.repo.ClassSubjectTeacherRepository
import kz.aqyldykundelik.classes.repo.ClassRepository
import kz.aqyldykundelik.classlevels.domain.ClassLevelEntity
import kz.aqyldykundelik.classlevels.repo.ClassLevelRepository
import kz.aqyldykundelik.curriculum.repo.CurriculumSubjectHoursRepository
import kz.aqyldykundelik.schedule.api.dto.*
import kz.aqyldykundelik.schedule.domain.ClassScheduleEntity
import kz.aqyldykundelik.schedule.domain.ClassScheduleLessonEntity
import kz.aqyldykundelik.schedule.repo.ClassScheduleLessonRepository
import kz.aqyldykundelik.schedule.repo.ClassScheduleRepository
import kz.aqyldykundelik.timetable.repo.SubjectRepository
import kz.aqyldykundelik.users.repo.UserRepository
import kz.aqyldykundelik.schedule.api.ScheduleActivationException
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
        val schoolClass = classRepository.findById(classId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found") }

        val classLevel = schoolClass.classLevelId?.let { classLevelRepository.findById(it).orElse(null) }

        var schedule = classScheduleRepository.findByClassId(classId)
        if (schedule == null) {
            schedule = ClassScheduleEntity(classId = classId)
        }

        // Sync policy from class_level
        syncPolicyFromClassLevel(schedule, classLevel)
        schedule = classScheduleRepository.save(schedule)

        return buildScheduleDto(classId, schedule, classLevel)
    }

    @Transactional
    fun updateClassSchedule(classId: UUID, updateDto: UpdateClassScheduleDto): ClassScheduleDto {
        val schoolClass = classRepository.findById(classId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found") }

        val classLevel = schoolClass.classLevelId?.let { classLevelRepository.findById(it).orElse(null) }

        var schedule = classScheduleRepository.findByClassId(classId)
        if (schedule == null) {
            schedule = ClassScheduleEntity(classId = classId)
        }

        // Sync policy from class_level (ignore client-provided values)
        syncPolicyFromClassLevel(schedule, classLevel)
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

        return buildScheduleDto(classId, schedule, classLevel)
    }

    @Transactional
    fun activateSchedule(classId: UUID): ClassScheduleDto {
        val schoolClass = classRepository.findById(classId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found") }

        val classLevel = schoolClass.classLevelId?.let { classLevelRepository.findById(it).orElse(null) }

        val schedule = classScheduleRepository.findByClassId(classId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found")

        // Sync policy from class_level
        syncPolicyFromClassLevel(schedule, classLevel)
        classScheduleRepository.save(schedule)

        val dto = buildScheduleDto(classId, schedule, classLevel)

        // Block activation if there are critical conflicts
        if (dto.hasCriticalConflicts) {
            throw ScheduleActivationException(dto)
        }

        schedule.status = ClassScheduleEntity.STATUS_ACTIVE
        classScheduleRepository.save(schedule)

        return dto.copy(status = ClassScheduleEntity.STATUS_ACTIVE)
    }

    private fun syncPolicyFromClassLevel(schedule: ClassScheduleEntity, classLevel: ClassLevelEntity?) {
        if (classLevel != null) {
            schedule.daysPerWeek = classLevel.daysPerWeek
            schedule.lessonsPerDay = classLevel.maxLessonsPerDay
        }
    }

    private fun buildScheduleDto(classId: UUID, schedule: ClassScheduleEntity, classLevel: ClassLevelEntity?): ClassScheduleDto {
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

        val conflicts = detectConflicts(classId, schedule, lessons, subjectsMap, teachersMap, classLevel)
        val hasCriticalConflicts = conflicts.any { it.critical }

        return ClassScheduleDto(
            scheduleId = schedule.id!!,
            classId = classId,
            status = schedule.status,
            daysPerWeek = schedule.daysPerWeek,
            lessonsPerDay = schedule.lessonsPerDay,
            grid = grid,
            conflicts = conflicts,
            hasCriticalConflicts = hasCriticalConflicts
        )
    }

    private fun detectConflicts(
        classId: UUID,
        schedule: ClassScheduleEntity,
        lessons: List<ClassScheduleLessonEntity>,
        subjectsMap: Map<UUID?, kz.aqyldykundelik.timetable.domain.SubjectEntity>,
        teachersMap: Map<UUID?, kz.aqyldykundelik.users.domain.UserEntity>,
        classLevel: ClassLevelEntity?
    ): List<ScheduleConflictDto> {
        val conflicts = mutableListOf<ScheduleConflictDto>()
        val classLevelId = classLevel?.id

        // 1. TEACHER_BUSY - Critical
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
                            critical = true,
                            teacherId = lesson.teacherId,
                            subjectId = lesson.subjectId,
                            dayOfWeek = lesson.dayOfWeek,
                            lessonNumber = lesson.lessonNumber,
                            message = "Учитель ${teacher?.fullName ?: ""} занят в другом классе"
                        )
                    )
                }
            }
        }

        // 2. INVALID_SLOT_RANGE - Critical
        val maxDays = classLevel?.daysPerWeek ?: schedule.daysPerWeek
        val maxLessons = classLevel?.maxLessonsPerDay ?: schedule.lessonsPerDay

        for (lesson in lessons) {
            if (lesson.subjectId == null) continue

            if (lesson.dayOfWeek > maxDays || lesson.lessonNumber > maxLessons) {
                val subject = subjectsMap[lesson.subjectId]
                conflicts.add(
                    ScheduleConflictDto(
                        type = ConflictType.INVALID_SLOT_RANGE,
                        critical = true,
                        teacherId = lesson.teacherId,
                        subjectId = lesson.subjectId,
                        dayOfWeek = lesson.dayOfWeek,
                        lessonNumber = lesson.lessonNumber,
                        message = "Урок ${subject?.nameRu ?: ""} вне допустимого диапазона (день ${lesson.dayOfWeek}/${maxDays}, урок ${lesson.lessonNumber}/${maxLessons})"
                    )
                )
            }
        }

        // 3. PLAN_EXCEEDS_SLOTS - Critical
        if (classLevelId != null) {
            val totalHoursPerWeek = curriculumSubjectHoursRepository.findByClassLevelId(classLevelId)
                .sumOf { it.hoursPerWeek }
            val availableSlotsPerWeek = maxDays * maxLessons

            if (totalHoursPerWeek > availableSlotsPerWeek) {
                conflicts.add(
                    ScheduleConflictDto(
                        type = ConflictType.PLAN_EXCEEDS_SLOTS,
                        critical = true,
                        teacherId = null,
                        subjectId = null,
                        dayOfWeek = 0,
                        lessonNumber = 0,
                        message = "Учебный план: $totalHoursPerWeek ч/нед, доступно слотов: $availableSlotsPerWeek. План не помещается."
                    )
                )
            }

            // 4. MAX_LESSONS_EXCEEDED - Non-critical
            val lessonsPerDay = lessons
                .filter { it.subjectId != null }
                .groupBy { it.dayOfWeek }
                .mapValues { it.value.size }

            for ((dayOfWeek, count) in lessonsPerDay) {
                if (count > maxLessons) {
                    val dayName = getDayName(dayOfWeek)
                    conflicts.add(
                        ScheduleConflictDto(
                            type = ConflictType.MAX_LESSONS_EXCEEDED,
                            critical = false,
                            teacherId = null,
                            subjectId = null,
                            dayOfWeek = dayOfWeek,
                            lessonNumber = 0,
                            message = "$dayName: $count уроков (максимум $maxLessons для ${classLevel!!.level} класса)"
                        )
                    )
                }
            }

            // 5. HOURS_MISMATCH - Non-critical
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
                            critical = false,
                            teacherId = null,
                            subjectId = subjectId,
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

    private fun getDayName(dayOfWeek: Int): String = when (dayOfWeek) {
        1 -> "Понедельник"
        2 -> "Вторник"
        3 -> "Среда"
        4 -> "Четверг"
        5 -> "Пятница"
        6 -> "Суббота"
        7 -> "Воскресенье"
        else -> "День $dayOfWeek"
    }
}
