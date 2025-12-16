package kz.aqyldykundelik.timetable.api

import kz.aqyldykundelik.classlevels.api.mappers.toDto
import kz.aqyldykundelik.classlevels.domain.ClassLevelEntity
import kz.aqyldykundelik.timetable.domain.SubjectEntity

fun SubjectEntity.toDto() = SubjectDto(
    id = id!!,
    nameRu = nameRu!!,
    nameKk = nameKk!!,
    nameEn = nameEn!!,
    classLevel = classLevel!!.toDto()
)

fun CreateSubjectDto.toEntity(classLevelEntity: ClassLevelEntity) = SubjectEntity(
    id = null,
    nameRu = this.nameRu,
    nameKk = this.nameKk,
    nameEn = this.nameEn,
    classLevel = classLevelEntity
)

fun SubjectEntity.applyUpdate(update: UpdateSubjectDto, classLevelEntity: ClassLevelEntity?): SubjectEntity = this.apply {
    update.nameRu?.let { nameRu = it }
    update.nameKk?.let { nameKk = it }
    update.nameEn?.let { nameEn = it }
    classLevelEntity?.let { classLevel = it }
}
