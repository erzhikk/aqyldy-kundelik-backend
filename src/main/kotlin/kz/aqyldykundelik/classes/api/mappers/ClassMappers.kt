package kz.aqyldykundelik.classes.api.mappers

import kz.aqyldykundelik.classes.api.dto.*
import kz.aqyldykundelik.classes.domain.ClassEntity

fun ClassEntity.toDto() = ClassDto(
    id = this.id!!,
    code = this.code!!,
    classTeacherId = this.classTeacherId,
    classLevelId = this.classLevelId,
    langType = this.langType!!
)

fun CreateClassDto.toEntity() = ClassEntity(
    id = null,
    code = this.code,
    classTeacherId = this.classTeacherId,
    classLevelId = this.classLevelId,
    langType = this.langType
)

fun ClassEntity.applyUpdate(update: UpdateClassDto): ClassEntity = this.apply {
    update.code?.let { code = it }
    update.langType?.let { langType = it }
    // classTeacherId обновляется ВСЕГДА, включая установку в null
    // Чтобы удалить учителя, передайте: {"classTeacherId": null}
    // ВАЖНО: если не передать поле вообще, оно тоже станет null!
    classTeacherId = update.classTeacherId
    // classLevelId также обновляется ВСЕГДА
    update.classLevelId?.let { classLevelId = it }
}
