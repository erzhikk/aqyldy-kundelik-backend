package kz.aqyldykundelik.users.api.mappers

import kz.aqyldykundelik.classes.api.mappers.toDto
import kz.aqyldykundelik.classes.domain.ClassEntity
import kz.aqyldykundelik.users.api.dto.*
import kz.aqyldykundelik.users.domain.UserEntity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.*

fun UserEntity.toDto() = UserDto(
    id = this.id!!,
    email = this.email!!,
    fullName = this.fullName!!,
    role = this.role!!,
    status = this.status,
    isActive = this.isActive,
    isDeleted = this.isDeleted,
    classId = this.classId,
    dateOfBirth = this.dateOfBirth,
    classDto = null
)

fun UserEntity.toDtoWithClass(classEntity: ClassEntity?) = UserDto(
    id = this.id!!,
    email = this.email!!,
    fullName = this.fullName!!,
    role = this.role!!,
    status = this.status,
    isActive = this.isActive,
    isDeleted = this.isDeleted,
    classId = this.classId,
    dateOfBirth = this.dateOfBirth,
    classDto = classEntity?.toDto()
)

fun CreateUserDto.toEntity(): UserEntity = UserEntity(
    id = null,
    email = this.email,
    fullName = this.fullName,
    role = this.role,
    status = "ACTIVE",
    passwordHash = BCryptPasswordEncoder().encode(this.password),
    classId = this.classId,
    dateOfBirth = this.dateOfBirth
)

fun UserEntity.applyUpdate(u: UpdateUserDto): UserEntity = this.apply {
    u.fullName?.let { fullName = it }
    u.role?.let { role = it }
    u.status?.let { status = it }
    // classId обновляется всегда, можно установить в null
    classId = u.classId
    // dateOfBirth обновляется всегда, можно установить в null
    dateOfBirth = u.dateOfBirth
}
