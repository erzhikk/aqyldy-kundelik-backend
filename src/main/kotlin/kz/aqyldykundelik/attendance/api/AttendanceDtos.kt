package kz.aqyldykundelik.attendance.api

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.util.*

data class AttendanceSheetDto(
    val id: UUID,
    val lessonDate: String,
    val groupId: UUID,
    val subjectId: UUID
)

data class CreateSheetDto(
    @field:NotNull val lessonDate: String, // YYYY-MM-DD
    @field:NotNull val groupId: UUID,
    @field:NotNull val subjectId: UUID
)

data class MarkItemDto(
    @field:NotNull val studentId: UUID,
    @field:NotBlank val status: String, // PRESENT/LATE/ABSENT/EXCUSED
    val reason: String?
)

data class BulkMarkDto(
    @field:NotNull val markedBy: UUID,
    val items: List<MarkItemDto>
)
