package kz.aqyldykundelik.assessment.service

import kz.aqyldykundelik.assessment.domain.AuditLogEntity
import kz.aqyldykundelik.assessment.repo.AuditLogRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class AuditService(
    private val auditLogRepository: AuditLogRepository
) {
    private val logger = LoggerFactory.getLogger(AuditService::class.java)

    /**
     * Log an audit event
     */
    fun log(
        eventType: String,
        entityType: String,
        entityId: UUID,
        userId: UUID? = null,
        metadata: Map<String, Any>? = null
    ) {
        try {
            val auditLog = AuditLogEntity(
                eventType = eventType,
                entityType = entityType,
                entityId = entityId,
                userId = userId,
                metadata = metadata
            )
            auditLogRepository.save(auditLog)

            // Also log to application logs
            logger.info(
                "AUDIT: {} | entity={}:{} | user={} | metadata={}",
                eventType,
                entityType,
                entityId,
                userId ?: "system",
                metadata ?: emptyMap<String, Any>()
            )
        } catch (e: Exception) {
            // Don't fail the main operation if audit logging fails
            logger.error("Failed to save audit log: eventType=$eventType, entityId=$entityId", e)
        }
    }

    companion object {
        // Event types
        const val TEST_CREATED = "TEST_CREATED"
        const val TEST_UPDATED = "TEST_UPDATED"
        const val TEST_DELETED = "TEST_DELETED"
        const val TEST_PUBLISHED = "TEST_PUBLISHED"
        const val TEST_QUESTIONS_ADDED = "TEST_QUESTIONS_ADDED"
        const val TEST_QUESTIONS_REORDERED = "TEST_QUESTIONS_REORDERED"

        const val QUESTION_CREATED = "QUESTION_CREATED"
        const val QUESTION_UPDATED = "QUESTION_UPDATED"
        const val QUESTION_DELETED = "QUESTION_DELETED"

        const val TOPIC_CREATED = "TOPIC_CREATED"

        const val ATTEMPT_STARTED = "ATTEMPT_STARTED"
        const val ATTEMPT_COMPLETED = "ATTEMPT_COMPLETED"

        // Entity types
        const val ENTITY_TEST = "TEST"
        const val ENTITY_QUESTION = "QUESTION"
        const val ENTITY_TOPIC = "TOPIC"
        const val ENTITY_ATTEMPT = "ATTEMPT"
    }
}
