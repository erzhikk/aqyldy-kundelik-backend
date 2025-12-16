package kz.aqyldykundelik.users.api.mappers

import kz.aqyldykundelik.classes.api.mappers.toDto
import kz.aqyldykundelik.classes.domain.ClassEntity
import kz.aqyldykundelik.users.api.dto.*
import kz.aqyldykundelik.users.domain.UserEntity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.*

fun UserEntity.toDto(photoUrl: String? = null) = UserDto(
    id = this.id!!,
    email = this.email!!,
    fullName = this.fullName!!,
    role = this.role!!,
    status = this.status,
    isActive = this.isActive,
    isDeleted = this.isDeleted,
    classId = this.classId,
    dateOfBirth = this.dateOfBirth,
    photoUrl = photoUrl,
    classDto = null
)

fun UserEntity.toDtoWithClass(classEntity: ClassEntity?, photoUrl: String? = null) = UserDto(
    id = this.id!!,
    email = this.email!!,
    fullName = this.fullName!!,
    role = this.role!!,
    status = this.status,
    isActive = this.isActive,
    isDeleted = this.isDeleted,
    classId = this.classId,
    dateOfBirth = this.dateOfBirth,
    photoUrl = photoUrl,
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
    dateOfBirth = this.dateOfBirth,
    photoMediaId = this.photoMediaId
)

fun UserEntity.applyUpdate(u: UpdateUserDto): UserEntity = this.apply {
    println("=== UserMappers.applyUpdate() START ===")
    println("Current photoMediaId: ${this.photoMediaId}")
    println("UpdateDto.photoMediaId: ${u.photoMediaId}")

    u.fullName?.let { fullName = it }
    u.role?.let { role = it }
    u.status?.let { status = it }
    // classId обновляется всегда, можно установить в null
    classId = u.classId
    // dateOfBirth обновляется всегда, можно установить в null
    dateOfBirth = u.dateOfBirth
    // photoMediaId обновляется только если явно передан (не null)
    // Для удаления фото нужно передать специальный маркер или использовать отдельный endpoint
    u.photoMediaId?.let {
        println("Updating photoMediaId to: $it")
        photoMediaId = it
    }

    println("After update photoMediaId: ${this.photoMediaId}")
    println("=== UserMappers.applyUpdate() END ===")
}
