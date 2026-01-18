package kz.aqyldykundelik.curriculum.api.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.util.*

data class ClassLevelDto(
    val id: UUID,
    val level: Int,
    val nameRu: String,
    val nameKk: String,
    val maxLessonsPerDay: Int,
    val daysPerWeek: Int
)

data class CurriculumSubjectDto(
    val subjectId: UUID,
    val nameRu: String,
    val nameKk: String,
    val nameEn: String,
    val hoursPerWeek: Int
)

data class CurriculumLevelSubjectsDto(
    val classLevelId: UUID,
    val maxLessonsPerDay: Int,
    val daysPerWeek: Int,
    val maxHoursPerWeek: Int,
    val totalHoursPerWeek: Int,
    val subjects: List<CurriculumSubjectDto>,
    val warnings: List<String>
)

data class UpdateSubjectHoursItemDto(
    @field:NotNull val subjectId: UUID,
    @field:Min(0) @field:Max(12) val hoursPerWeek: Int
)

data class UpdateCurriculumSubjectsDto(
    @field:Valid val items: List<UpdateSubjectHoursItemDto>,
    @field:Min(1) @field:Max(10) val maxLessonsPerDay: Int? = null,
    @field:Min(5) @field:Max(6) val daysPerWeek: Int? = null
)

data class UpdateClassLevelSettingsDto(
    @field:Min(1) @field:Max(10) val maxLessonsPerDay: Int? = null,
    @field:Min(5) @field:Max(6) val daysPerWeek: Int? = null
)
