package kz.aqyldykundelik.attendance.repo

import kz.aqyldykundelik.attendance.domain.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate
import java.util.*

interface AttendanceSheetRepository : JpaRepository<AttendanceSheetEntity, UUID> {
    fun findByGroupIdAndLessonDate(groupId: UUID, lessonDate: LocalDate, pageable: Pageable): Page<AttendanceSheetEntity>
}

interface AttendanceRecordRepository : JpaRepository<AttendanceRecordEntity, UUID> {
    fun findBySheetId(sheetId: UUID): List<AttendanceRecordEntity>

    @Query("""
        SELECT COUNT(*) FROM AttendanceRecordEntity r
        WHERE r.studentId = :studentId
    """)
    fun countTotalRecordsByStudent(studentId: UUID): Long

    @Query("""
        SELECT COUNT(*) FROM AttendanceRecordEntity r
        WHERE r.studentId = :studentId AND r.status = :status
    """)
    fun countByStudentAndStatus(studentId: UUID, status: String): Long
}
