package kz.aqyldykundelik.users.api.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import kz.aqyldykundelik.classes.api.dto.ClassDto
import java.time.LocalDate
import java.util.*

data class UserDto(
    val id: UUID,
    val email: String,
    val fullName: String,
    val role: String,
    val status: String,
    val isActive: Boolean,
    val isDeleted: Boolean,
    val classId: UUID?,
    val dateOfBirth: LocalDate?,
    val photoUrl: String? = null,
    val classDto: ClassDto? = null
)

data class CreateUserDto(
    @field:NotBlank @field:Email val email: String,
    @field:NotBlank @field:Size(min = 2, max = 100) val fullName: String,
    @field:NotBlank @field:Pattern(regexp = "STUDENT|PARENT|TEACHER|ADMIN|ADMIN_SCHEDULE|ADMIN_ASSESSMENT|SUPER_ADMIN")
    val role: String,
    @field:NotBlank @field:Size(min = 6, max = 100) val password: String,
    val classId: UUID?,
    val dateOfBirth: LocalDate?,
    val photoMediaId: UUID? // ID медиа объекта для аватарки
)

data class UpdateUserDto(
    @field:Size(min = 2, max = 100) val fullName: String?,
    @field:Pattern(regexp = "STUDENT|PARENT|TEACHER|ADMIN|ADMIN_SCHEDULE|ADMIN_ASSESSMENT|SUPER_ADMIN")
    val role: String?,
    val status: String?, // ACTIVE/INACTIVE
    val classId: UUID?,
    val dateOfBirth: LocalDate?,
    val photoMediaId: UUID? // ID медиа объекта для аватарки
)
