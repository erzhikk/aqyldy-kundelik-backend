package kz.aqyldykundelik.schedule.repo

import kz.aqyldykundelik.schedule.domain.ClassScheduleEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ClassScheduleRepository : JpaRepository<ClassScheduleEntity, UUID> {
    fun findByClassId(classId: UUID): ClassScheduleEntity?
    fun findByStatus(status: String): List<ClassScheduleEntity>
}
