package kz.aqyldykundelik.classlevels.repo

import kz.aqyldykundelik.classlevels.domain.ClassLevelEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ClassLevelRepository : JpaRepository<ClassLevelEntity, UUID> {
    fun findByLevel(level: Int): ClassLevelEntity?
    fun existsByLevel(level: Int): Boolean
}
