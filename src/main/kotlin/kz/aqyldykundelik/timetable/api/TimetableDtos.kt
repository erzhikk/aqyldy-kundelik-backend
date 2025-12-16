package kz.aqyldykundelik.timetable.api
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.LocalTime
import java.util.*

data class LessonDto(
    val id: UUID,
    val groupId: UUID,
    val subjectId: UUID,
    val teacherId: UUID,
    val roomId: UUID?,
    val weekday: Short,
    val startTime: String,
    val endTime: String
)

data class CreateLessonDto(
    @field:NotNull val groupId: UUID,
    @field:NotNull val subjectId: UUID,
    @field:NotNull val teacherId: UUID,
    val roomId: UUID?,
    @field:Min(1) @field:Max(7) val weekday: Short,
    @field:NotNull val startTime: String, // "08:30"
    @field:NotNull val endTime: String
)
