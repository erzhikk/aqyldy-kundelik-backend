package kz.aqyldykundelik.schedule.api.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
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
    val critical: Boolean,
    val teacherId: UUID?,
    val subjectId: UUID?,
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
    val conflicts: List<ScheduleConflictDto>,
    val hasCriticalConflicts: Boolean
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
    // Critical - block activation
    const val TEACHER_BUSY = "TEACHER_BUSY"
    const val PLAN_EXCEEDS_SLOTS = "PLAN_EXCEEDS_SLOTS"
    const val INVALID_SLOT_RANGE = "INVALID_SLOT_RANGE"

    // Non-critical - warnings only
    const val HOURS_MISMATCH = "HOURS_MISMATCH"
    const val MAX_LESSONS_EXCEEDED = "MAX_LESSONS_EXCEEDED"

    private val criticalTypes = setOf(TEACHER_BUSY, PLAN_EXCEEDS_SLOTS, INVALID_SLOT_RANGE)

    fun isCritical(type: String): Boolean = type in criticalTypes
}
