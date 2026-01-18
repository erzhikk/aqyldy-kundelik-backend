package kz.aqyldykundelik.assignments.service

import kz.aqyldykundelik.assignments.api.dto.*
import kz.aqyldykundelik.assignments.domain.ClassSubjectTeacherEntity
import kz.aqyldykundelik.assignments.repo.ClassSubjectTeacherRepository
import kz.aqyldykundelik.classes.repo.ClassRepository
import kz.aqyldykundelik.timetable.repo.SubjectRepository
import kz.aqyldykundelik.users.repo.UserRepository
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.*

@Service
class AssignmentsService(
    private val classRepository: ClassRepository,
    private val subjectRepository: SubjectRepository,
    private val userRepository: UserRepository,
    private val classSubjectTeacherRepository: ClassSubjectTeacherRepository
) {

    fun getClassAssignments(classId: UUID): ClassAssignmentsDto {
        val schoolClass = classRepository.findById(classId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found") }

        val classLevelId = schoolClass.classLevelId
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Class has no level assigned")

        val subjects = subjectRepository.findAllByClassLevelId(classLevelId)
        val assignments = classSubjectTeacherRepository.findByClassId(classId)
            .associateBy { it.subjectId }

        val teacherIds = assignments.values.mapNotNull { it.teacherId }.toSet()
        val teachersMap = if (teacherIds.isNotEmpty()) {
            userRepository.findAllById(teacherIds).associateBy { it.id }
        } else {
            emptyMap()
        }

        val items = subjects.map { subject ->
            val assignment = assignments[subject.id]
            val teacher = assignment?.teacherId?.let { teachersMap[it] }

            ClassSubjectTeacherItemDto(
                subjectId = subject.id!!,
                subjectNameRu = subject.nameRu!!,
                teacherId = teacher?.id,
                teacherFullName = teacher?.fullName,
                teacherEmail = teacher?.email
            )
        }

        return ClassAssignmentsDto(
            classId = classId,
            classCode = schoolClass.code!!,
            items = items
        )
    }

    @Transactional
    fun updateClassAssignments(classId: UUID, updateDto: UpdateClassAssignmentsDto): ClassAssignmentsDto {
        if (!classRepository.existsById(classId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found")
        }

        for (item in updateDto.items) {
            val existing = classSubjectTeacherRepository.findByClassIdAndSubjectId(classId, item.subjectId)
            if (existing != null) {
                existing.teacherId = item.teacherId
                classSubjectTeacherRepository.save(existing)
            } else {
                val entity = ClassSubjectTeacherEntity(
                    classId = classId,
                    subjectId = item.subjectId,
                    teacherId = item.teacherId
                )
                classSubjectTeacherRepository.save(entity)
            }
        }

        return getClassAssignments(classId)
    }

    fun getTeachers(query: String?): List<TeacherDto> {
        val teachers = userRepository.findAllByRoleAndIsDeletedFalse("TEACHER", Sort.by("fullName"))
            .filter { it.status == "ACTIVE" }

        val filtered = if (!query.isNullOrBlank()) {
            val q = query.lowercase()
            teachers.filter {
                it.fullName?.lowercase()?.contains(q) == true ||
                        it.email?.lowercase()?.contains(q) == true
            }
        } else {
            teachers
        }

        return filtered.map {
            TeacherDto(
                id = it.id!!,
                fullName = it.fullName!!,
                email = it.email!!
            )
        }
    }
}
