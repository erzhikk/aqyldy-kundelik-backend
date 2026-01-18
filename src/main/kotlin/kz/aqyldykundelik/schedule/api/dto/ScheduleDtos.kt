package kz.aqyldykundelik.schedule.api.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.util.*

data class ScheduleLessonDto(
    val dayOfWeek: Int,
    val lessonNumber: Int,
    val subjectId: UUID?,
    val subjectNameRu: String?,
    val teacherId: UUID?,
    val teacherFullName: String?
)

data class ScheduleConflictDto(
    val type: String,
    val teacherId: UUID?,
    val dayOfWeek: Int,
    val lessonNumber: Int,
    val message: String
)

data class ClassScheduleDto(
    val scheduleId: UUID,
    val classId: UUID,
    val status: String,
    val daysPerWeek: Int,
    val lessonsPerDay: Int,
    val grid: List<ScheduleLessonDto>,
    val conflicts: List<ScheduleConflictDto>
)

data class UpdateScheduleLessonDto(
    @field:Min(1) @field:Max(7) val dayOfWeek: Int,
    @field:Min(1) val lessonNumber: Int,
    val subjectId: UUID?
)

data class UpdateClassScheduleDto(
    @field:Min(5) @field:Max(6) val daysPerWeek: Int = 5,
    @field:Min(1) @field:Max(10) val lessonsPerDay: Int = 7,
    @field:Valid val grid: List<UpdateScheduleLessonDto>
)

object ConflictType {
    const val TEACHER_BUSY = "TEACHER_BUSY"
    const val HOURS_MISMATCH = "HOURS_MISMATCH"
    const val MAX_LESSONS_EXCEEDED = "MAX_LESSONS_EXCEEDED"
}
