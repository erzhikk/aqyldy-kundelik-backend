package kz.aqyldykundelik.schedule.repo

import kz.aqyldykundelik.schedule.domain.ClassScheduleLessonEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.*

interface ClassScheduleLessonRepository : JpaRepository<ClassScheduleLessonEntity, UUID> {
    fun findByScheduleId(scheduleId: UUID): List<ClassScheduleLessonEntity>
    fun findByScheduleIdAndDayOfWeekAndLessonNumber(
        scheduleId: UUID,
        dayOfWeek: Int,
        lessonNumber: Int
    ): ClassScheduleLessonEntity?

    @Modifying
    @Query("DELETE FROM ClassScheduleLessonEntity l WHERE l.scheduleId = :scheduleId")
    fun deleteByScheduleId(scheduleId: UUID)

    @Query("""
        SELECT l FROM ClassScheduleLessonEntity l
        JOIN ClassScheduleEntity s ON l.scheduleId = s.id
        WHERE l.teacherId = :teacherId
        AND l.dayOfWeek = :dayOfWeek
        AND l.lessonNumber = :lessonNumber
        AND s.classId != :excludeClassId
    """)
    fun findConflictingLessons(
        teacherId: UUID,
        dayOfWeek: Int,
        lessonNumber: Int,
        excludeClassId: UUID
    ): List<ClassScheduleLessonEntity>
}
