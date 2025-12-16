package kz.aqyldykundelik.timetable.domain
import jakarta.persistence.*
import java.time.OffsetDateTime
import java.time.LocalTime
import java.util.*

@Entity @Table(name = "timetable_lesson")
class TimetableLessonEntity(
    @Id @GeneratedValue(strategy = GenerationType.UUID) var id: UUID? = null,
    @Column(name="group_id", nullable=false) var groupId: UUID? = null,
    @Column(name="subject_id", nullable=false) var subjectId: UUID? = null,
    @Column(name="teacher_id", nullable=false) var teacherId: UUID? = null,
    @Column(name="room_id") var roomId: UUID? = null,
    @Column(nullable=false) var weekday: Short? = null,
    @Column(name="start_time", nullable=false) var startTime: LocalTime? = null,
    @Column(name="end_time", nullable=false) var endTime: LocalTime? = null,
    @Column(name="created_at") var createdAt: OffsetDateTime? = null,
    @Column(name="updated_at") var updatedAt: OffsetDateTime? = null,
)
