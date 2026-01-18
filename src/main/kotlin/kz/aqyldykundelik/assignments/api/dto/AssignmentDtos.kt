package kz.aqyldykundelik.assignments.api.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import java.util.*

data class ClassSubjectTeacherItemDto(
    val subjectId: UUID,
    val subjectNameRu: String,
    val teacherId: UUID?,
    val teacherFullName: String?,
    val teacherEmail: String?
)

data class ClassAssignmentsDto(
    val classId: UUID,
    val classCode: String,
    val items: List<ClassSubjectTeacherItemDto>
)

data class UpdateAssignmentItemDto(
    @field:NotNull val subjectId: UUID,
    @field:NotNull val teacherId: UUID
)

data class UpdateClassAssignmentsDto(
    @field:Valid val items: List<UpdateAssignmentItemDto>
)

data class TeacherDto(
    val id: UUID,
    val fullName: String,
    val email: String
)
