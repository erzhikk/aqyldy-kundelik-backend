package kz.aqyldykundelik.classlevels.api.mappers

import kz.aqyldykundelik.classlevels.api.dto.*
import kz.aqyldykundelik.classlevels.domain.ClassLevelEntity

fun ClassLevelEntity.toDto() = ClassLevelDto(
    id = this.id!!,
    level = this.level!!,
    nameRu = this.nameRu!!,
    nameKk = this.nameKk!!
)

fun CreateClassLevelDto.toEntity() = ClassLevelEntity(
    id = null,
    level = this.level,
    nameRu = this.nameRu,
    nameKk = this.nameKk
)

fun ClassLevelEntity.applyUpdate(update: UpdateClassLevelDto): ClassLevelEntity = this.apply {
    update.level?.let { level = it }
    update.nameRu?.let { nameRu = it }
    update.nameKk?.let { nameKk = it }
}
