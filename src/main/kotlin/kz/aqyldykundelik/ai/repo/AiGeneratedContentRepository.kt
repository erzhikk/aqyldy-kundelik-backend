package kz.aqyldykundelik.ai.repo

import kz.aqyldykundelik.ai.domain.AiGeneratedContentEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface AiGeneratedContentRepository : JpaRepository<AiGeneratedContentEntity, UUID> {
    fun findTop1ByStudentIdAndTypeAndPromptHashOrderByCreatedAtDesc(
        studentId: UUID,
        type: String,
        promptHash: String
    ): AiGeneratedContentEntity?

    fun countByStudentIdAndCreatedAtAfter(studentId: UUID, createdAfter: OffsetDateTime): Long
}
