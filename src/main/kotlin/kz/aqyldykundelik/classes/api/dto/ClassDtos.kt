package kz.aqyldykundelik.classes.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.*

data class ClassDto(
    val id: UUID,
    val code: String,
    val classTeacherId: UUID?,
    val classTeacherFullName: String? = null,
    val classLevelId: UUID?,
    val langType: String
)

data class CreateClassDto(
    @field:NotBlank @field:Size(min = 1, max = 3) val code: String,
    val classTeacherId: UUID?,
    val classLevelId: UUID?,
    @field:NotBlank @field:Size(min = 1, max = 3) val langType: String
)

data class UpdateClassDto(
    @field:Size(min = 1, max = 3) val code: String?,
    val classTeacherId: UUID?,
    val classLevelId: UUID?,
    @field:Size(min = 1, max = 3) val langType: String?
)
