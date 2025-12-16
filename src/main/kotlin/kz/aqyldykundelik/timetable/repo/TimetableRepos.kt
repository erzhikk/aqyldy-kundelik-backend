package kz.aqyldykundelik.timetable.repo
import kz.aqyldykundelik.timetable.domain.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface SubjectRepository : JpaRepository<SubjectEntity, UUID> {
    @Query("""
        SELECT s.* FROM subject s
        WHERE (:q IS NULL OR :q = '')
           OR LOWER(s.name_ru) LIKE LOWER(CONCAT('%', CAST(:q AS TEXT), '%'))
           OR LOWER(s.name_kk) LIKE LOWER(CONCAT('%', CAST(:q AS TEXT), '%'))
           OR LOWER(s.name_en) LIKE LOWER(CONCAT('%', CAST(:q AS TEXT), '%'))
        ORDER BY s.name_ru
    """,
    countQuery = """
        SELECT COUNT(*) FROM subject s
        WHERE (:q IS NULL OR :q = '')
           OR LOWER(s.name_ru) LIKE LOWER(CONCAT('%', CAST(:q AS TEXT), '%'))
           OR LOWER(s.name_kk) LIKE LOWER(CONCAT('%', CAST(:q AS TEXT), '%'))
           OR LOWER(s.name_en) LIKE LOWER(CONCAT('%', CAST(:q AS TEXT), '%'))
    """,
    nativeQuery = true)
    fun search(q: String?, pageable: Pageable): Page<SubjectEntity>

    fun findByClassLevel_Id(classLevelId: UUID, pageable: Pageable): Page<SubjectEntity>
}

interface RoomRepository : JpaRepository<RoomEntity, UUID>

interface TimetableLessonRepository : JpaRepository<TimetableLessonEntity, UUID> {
    fun findByGroupIdOrderByWeekdayAscStartTimeAsc(groupId: UUID, pageable: Pageable): Page<TimetableLessonEntity>
    fun findByTeacherIdOrderByWeekdayAscStartTimeAsc(teacherId: UUID, pageable: Pageable): Page<TimetableLessonEntity>
}
