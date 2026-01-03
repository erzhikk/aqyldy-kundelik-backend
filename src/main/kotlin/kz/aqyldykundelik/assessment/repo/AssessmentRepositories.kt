package kz.aqyldykundelik.assessment.repo

import kz.aqyldykundelik.assessment.domain.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.*

@Repository
interface TopicRepository : JpaRepository<TopicEntity, UUID> {
    fun findBySubjectId(subjectId: UUID): List<TopicEntity>
    fun findBySubjectIdAndNameContainingIgnoreCase(subjectId: UUID, name: String): List<TopicEntity>
}

@Repository
interface TestRepository : JpaRepository<TestEntity, UUID> {
    fun findBySubjectId(subjectId: UUID): List<TestEntity>
    fun findBySubjectIdAndStatus(subjectId: UUID, status: kz.aqyldykundelik.assessment.domain.TestStatus): List<TestEntity>
    fun findByClassLevelId(classLevelId: UUID): List<TestEntity>
    fun findByStatus(status: kz.aqyldykundelik.assessment.domain.TestStatus): List<TestEntity>
}

@Repository
interface QuestionRepository : JpaRepository<QuestionEntity, UUID> {
    fun findByTopicId(topicId: UUID): List<QuestionEntity>
    fun findByDifficulty(difficulty: Difficulty): List<QuestionEntity>

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
        SELECT ta FROM TestAttemptEntity ta
        JOIN TestEntity t ON ta.testId = t.id
        WHERE ta.studentId = :studentId
        AND ta.status = 'GRADED'
        AND (:subjectId IS NULL OR t.subjectId = :subjectId)
        AND (:from IS NULL OR ta.finishedAt >= :from)
        AND (:to IS NULL OR ta.finishedAt <= :to)
    """)
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
