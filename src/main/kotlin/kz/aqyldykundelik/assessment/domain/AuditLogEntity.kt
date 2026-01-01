package kz.aqyldykundelik.assessment.domain

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "audit_log")
class AuditLogEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "event_type", nullable = false, length = 50)
    var eventType: String,

    @Column(name = "entity_type", nullable = false, length = 50)
    var entityType: String,

    @Column(name = "entity_id", nullable = false)
    var entityId: UUID,

    @Column(name = "user_id")
    var userId: UUID? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    var metadata: Map<String, Any>? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now()
)
