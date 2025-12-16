package kz.aqyldykundelik.classlevels.api.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.*

data class ClassLevelDto(
    val id: UUID,
    val level: Int,
    val nameRu: String,
    val nameKk: String
)

data class CreateClassLevelDto(
    @field:NotNull @field:Min(1) @field:Max(11) val level: Int,
    @field:NotBlank val nameRu: String,
    @field:NotBlank val nameKk: String
)

data class UpdateClassLevelDto(
    @field:Min(1) @field:Max(11) val level: Int?,
    @field:NotBlank val nameRu: String?,
    @field:NotBlank val nameKk: String?
)
