package kz.aqyldykundelik.assessment.repo

import kz.aqyldykundelik.assessment.domain.AuditLogEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.*

@Repository
interface AuditLogRepository : JpaRepository<AuditLogEntity, UUID> {
    fun findByEventType(eventType: String): List<AuditLogEntity>
    fun findByEntityTypeAndEntityId(entityType: String, entityId: UUID): List<AuditLogEntity>
    fun findByUserId(userId: UUID): List<AuditLogEntity>
    fun findByCreatedAtBetween(from: OffsetDateTime, to: OffsetDateTime): List<AuditLogEntity>
}
