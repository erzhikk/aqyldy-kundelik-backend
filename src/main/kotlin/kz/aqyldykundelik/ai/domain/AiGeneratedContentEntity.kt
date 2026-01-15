package kz.aqyldykundelik.ai.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "ai_generated_content")
class AiGeneratedContentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "student_id", nullable = false)
    var studentId: UUID,

    @Column(name = "attempt_id")
    var attemptId: UUID? = null,

    @Column(name = "topic_id")
    var topicId: UUID? = null,

    @Column(name = "type", nullable = false, length = 32)
    var type: String,

    @Column(name = "prompt_hash", nullable = false, length = 64)
    var promptHash: String,

    @Column(name = "content", nullable = false, columnDefinition = "text")
    var content: String,

    @Column(name = "model", length = 128)
    var model: String? = null,

    @Column(name = "provider", length = 64)
    var provider: String? = null,

    @Column(name = "cached_until")
    var cachedUntil: OffsetDateTime? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "input_tokens")
    var inputTokens: Int? = null,

    @Column(name = "output_tokens")
    var outputTokens: Int? = null
)
