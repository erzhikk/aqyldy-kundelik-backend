package kz.aqyldykundelik.attendance.domain

import jakarta.persistence.*
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

@Entity @Table(name = "attendance_sheet")
class AttendanceSheetEntity(
    @Id @GeneratedValue(strategy = GenerationType.UUID) var id: UUID? = null,
    @Column(name="lesson_date", nullable=false) var lessonDate: LocalDate? = null,
    @Column(name="group_id", nullable=false) var groupId: UUID? = null,
    @Column(name="subject_id", nullable=false) var subjectId: UUID? = null,
    @Column(name="lesson_ref_id") var lessonRefId: UUID? = null,
    @Column(name="created_at") var createdAt: OffsetDateTime? = null
)

@Entity @Table(name = "attendance_record")
class AttendanceRecordEntity(
    @Id @GeneratedValue(strategy = GenerationType.UUID) var id: UUID? = null,
    @Column(name="sheet_id", nullable=false) var sheetId: UUID? = null,
    @Column(name="student_id", nullable=false) var studentId: UUID? = null,
    @Column(name="status", nullable=false) var status: String? = null, // PRESENT/LATE/ABSENT/EXCUSED
    var reason: String? = null,
    @Column(name="marked_at") var markedAt: OffsetDateTime? = null,
    @Column(name="marked_by") var markedBy: UUID? = null
)
