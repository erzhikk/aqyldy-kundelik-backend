package kz.aqyldykundelik.users.api.dto

import kz.aqyldykundelik.classes.api.dto.ClassDto
import java.time.LocalDate
import java.util.*

/**
 * DTO для карточки студента с расширенной информацией
 */
data class StudentCardDto(
    val id: UUID,
    val email: String,
    val fullName: String,
    val dateOfBirth: LocalDate?,
    val isActive: Boolean,
    val status: String,
    val photoUrl: String? = null,  // Presigned URL для фото профиля
    val schoolClass: ClassDto?,
    val attendanceStats: AttendanceStatsDto?
)

/**
 * Статистика посещаемости студента
 */
data class AttendanceStatsDto(
    val totalLessons: Long,
    val present: Long,
    val late: Long,
    val absent: Long,
    val excused: Long,
    val attendanceRate: Double // процент присутствия
)

/**
 * DTO для карточки сотрудника с расширенной информацией
 */
data class StaffCardDto(
    val id: UUID,
    val email: String,
    val fullName: String,
    val role: String,
    val isActive: Boolean,
    val status: String,
    val photoUrl: String? = null,  // Presigned URL для фото профиля
    val classAsTeacher: ClassDto?, // класс, в котором является классным руководителем
    val taughtSubjects: List<TaughtSubjectDto> // предметы, которые преподает
)

/**
 * Информация о предмете, который преподает учитель
 */
data class TaughtSubjectDto(
    val subjectId: UUID,
    val subjectName: String,
    val groups: List<String> // коды групп
)
