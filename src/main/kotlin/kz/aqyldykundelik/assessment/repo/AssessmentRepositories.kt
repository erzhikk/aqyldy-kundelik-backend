package kz.aqyldykundelik.assessment.repo

import kz.aqyldykundelik.assessment.domain.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

interface TopicListProjection {
    fun getId(): UUID
    fun getSubjectId(): UUID
    fun getName(): String
    fun getDescription(): String?
    fun getCreatedAt(): Instant?
    fun getCreatedByFullName(): String?
    fun getQuestionsCount(): Long
}

@Repository
interface TopicRepository : JpaRepository<TopicEntity, UUID> {
    fun findBySubjectId(subjectId: UUID): List<TopicEntity>
    fun findBySubjectIdAndNameContainingIgnoreCase(subjectId: UUID, name: String): List<TopicEntity>

    @Query("""
        SELECT
            t.id as id,
            t.subject_id as subjectId,
            t.name as name,
            t.description as description,
            t.created_at as createdAt,
            u.full_name as createdByFullName,
            COUNT(q.id) as questionsCount
        FROM topic t
        LEFT JOIN app_user u ON u.id = t.created_by_user_id
        LEFT JOIN question q ON q.topic_id = t.id
        WHERE (CAST(:subjectId AS uuid) IS NULL OR t.subject_id = CAST(:subjectId AS uuid))
            AND (:q IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :q, '%')))
        GROUP BY t.id, t.subject_id, t.name, t.description, t.created_at, u.full_name
        ORDER BY t.name
    """, nativeQuery = true)
    fun findTopicList(@Param("subjectId") subjectId: UUID?, @Param("q") q: String?): List<TopicListProjection>
}

@Repository
interface TestRepository : JpaRepository<TestEntity, UUID> {
    fun findBySubjectId(subjectId: UUID): List<TestEntity>
    fun findBySubjectIdAndStatus(subjectId: UUID, status: kz.aqyldykundelik.assessment.domain.TestStatus): List<TestEntity>
    fun findByStatus(status: kz.aqyldykundelik.assessment.domain.TestStatus): List<TestEntity>
}

@Repository
interface TestSchoolClassRepository : JpaRepository<TestSchoolClassEntity, UUID> {
    fun findByTestId(testId: UUID): List<TestSchoolClassEntity>
    fun findBySchoolClassId(schoolClassId: UUID): List<TestSchoolClassEntity>
    fun deleteByTestId(testId: UUID)
    fun deleteByTestIdAndSchoolClassId(testId: UUID, schoolClassId: UUID)
}

@Repository
interface QuestionRepository : JpaRepository<QuestionEntity, UUID> {
    fun findByTopicId(topicId: UUID): List<QuestionEntity>
    fun findByTopicId(topicId: UUID, pageable: Pageable): Page<QuestionEntity>
    fun findByTopicIdAndTextContainingIgnoreCase(topicId: UUID, text: String, pageable: Pageable): Page<QuestionEntity>
    fun findByDifficulty(difficulty: Difficulty): List<QuestionEntity>
    fun countByTopicId(topicId: UUID): Long

    @Query("""
        SELECT q FROM QuestionEntity q
        JOIN TopicEntity t ON q.topicId = t.id
        WHERE (:subjectId IS NULL OR t.subjectId = :subjectId)
        AND (:topicId IS NULL OR q.topicId = :topicId)
        AND (:difficulty IS NULL OR q.difficulty = :difficulty)
        ORDER BY q.topicId, q.difficulty
    """)
    fun findWithFilters(
        @Param("subjectId") subjectId: UUID?,
        @Param("topicId") topicId: UUID?,
        @Param("difficulty") difficulty: Difficulty?
    ): List<QuestionEntity>
}

@Repository
interface ChoiceRepository : JpaRepository<ChoiceEntity, UUID> {
    fun findByQuestionId(questionId: UUID): List<ChoiceEntity>
    fun findByQuestionIdIn(questionIds: List<UUID>): List<ChoiceEntity>
    fun deleteByQuestionId(questionId: UUID)
}

@Repository
interface TestQuestionRepository : JpaRepository<TestQuestionEntity, TestQuestionId> {
    fun findByTestIdOrderByOrderAsc(testId: UUID): List<TestQuestionEntity>
    fun findByQuestionId(questionId: UUID): List<TestQuestionEntity>
    fun deleteByTestId(testId: UUID)

    @Query("SELECT COUNT(tq) FROM TestQuestionEntity tq WHERE tq.testId = :testId")
    fun countByTestId(@Param("testId") testId: UUID): Long
}

@Repository
interface TestAttemptRepository : JpaRepository<TestAttemptEntity, UUID> {
    fun findByTestId(testId: UUID): List<TestAttemptEntity>
    fun findByStudentId(studentId: UUID): List<TestAttemptEntity>
    fun findByStudentIdAndTestId(studentId: UUID, testId: UUID): List<TestAttemptEntity>
    fun findByStatus(status: AttemptStatus): List<TestAttemptEntity>

    @Query("SELECT COUNT(ta) FROM TestAttemptEntity ta WHERE ta.testId = :testId")
    fun countByTestId(@Param("testId") testId: UUID): Long

    // Analytics: Find graded attempts for a student with optional filters
    @Query("""
        SELECT ta.* FROM test_attempt ta
        JOIN test t ON ta.test_id = t.id
        WHERE ta.student_id = :studentId
        AND ta.status = 'GRADED'
        AND (CAST(:subjectId AS uuid) IS NULL OR t.subject_id = CAST(:subjectId AS uuid))
        AND (CAST(:from AS timestamptz) IS NULL OR ta.finished_at >= CAST(:from AS timestamptz))
        AND (CAST(:to AS timestamptz) IS NULL OR ta.finished_at <= CAST(:to AS timestamptz))
    """, nativeQuery = true)
    fun findGradedAttemptsForStudent(
        @Param("studentId") studentId: UUID,
        @Param("subjectId") subjectId: UUID?,
        @Param("from") from: OffsetDateTime?,
        @Param("to") to: OffsetDateTime?
    ): List<TestAttemptEntity>

    // Analytics: Find graded attempts for a class on a specific test
    @Query("""
        SELECT ta.* FROM test_attempt ta
        JOIN app_user u ON ta.student_id = u.id
        WHERE ta.test_id = :testId
        AND ta.status = 'GRADED'
        AND u.class_id = :classId
    """, nativeQuery = true)
    fun findGradedAttemptsByClassAndTest(
        @Param("classId") classId: UUID,
        @Param("testId") testId: UUID
    ): List<TestAttemptEntity>
}

@Repository
interface AttemptAnswerRepository : JpaRepository<AttemptAnswerEntity, UUID> {
    fun findByAttemptId(attemptId: UUID): List<AttemptAnswerEntity>
    fun findByQuestionId(questionId: UUID): List<AttemptAnswerEntity>
}
