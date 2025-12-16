package kz.aqyldykundelik.timetable.api

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import kz.aqyldykundelik.classlevels.api.dto.ClassLevelDto
import java.util.*

data class SubjectDto(
    val id: UUID,
    val nameRu: String,
    val nameKk: String,
    val nameEn: String,
    val classLevel: ClassLevelDto
)

data class CreateSubjectDto(
    @field:NotBlank val nameRu: String,
    @field:NotBlank val nameKk: String,
    @field:NotBlank val nameEn: String,
    @field:NotNull val classLevelId: UUID
)

data class UpdateSubjectDto(
    @field:NotBlank val nameRu: String?,
    @field:NotBlank val nameKk: String?,
    @field:NotBlank val nameEn: String?,
    val classLevelId: UUID?
)
