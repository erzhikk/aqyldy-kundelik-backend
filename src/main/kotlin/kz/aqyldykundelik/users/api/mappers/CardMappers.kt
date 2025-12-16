package kz.aqyldykundelik.users.api.mappers

import kz.aqyldykundelik.classes.api.dto.ClassDto
import kz.aqyldykundelik.classes.api.mappers.toDto
import kz.aqyldykundelik.classes.domain.ClassEntity
import kz.aqyldykundelik.users.api.dto.AttendanceStatsDto
import kz.aqyldykundelik.users.api.dto.StaffCardDto
import kz.aqyldykundelik.users.api.dto.StudentCardDto
import kz.aqyldykundelik.users.api.dto.TaughtSubjectDto
import kz.aqyldykundelik.users.domain.UserEntity

fun UserEntity.toStudentCard(
    classEntity: ClassEntity?,
    attendanceStats: AttendanceStatsDto?
) = StudentCardDto(
    id = this.id!!,
    email = this.email!!,
    fullName = this.fullName!!,
    dateOfBirth = this.dateOfBirth,
    isActive = this.isActive,
    status = this.status,
    schoolClass = classEntity?.toDto(),
    attendanceStats = attendanceStats
)

fun UserEntity.toStaffCard(
    classAsTeacher: ClassDto?,
    taughtSubjects: List<TaughtSubjectDto>
) = StaffCardDto(
    id = this.id!!,
    email = this.email!!,
    fullName = this.fullName!!,
    role = this.role!!,
    isActive = this.isActive,
    status = this.status,
    classAsTeacher = classAsTeacher,
    taughtSubjects = taughtSubjects
)
