package kz.aqyldykundelik.classes.repo

import kz.aqyldykundelik.classes.domain.ClassEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ClassRepository : JpaRepository<ClassEntity, UUID> {
    fun findByCode(code: String): ClassEntity?
    fun findByLangType(langType: String, pageable: Pageable): Page<ClassEntity>
    fun findByClassTeacherId(classTeacherId: UUID, pageable: Pageable): Page<ClassEntity>
}
