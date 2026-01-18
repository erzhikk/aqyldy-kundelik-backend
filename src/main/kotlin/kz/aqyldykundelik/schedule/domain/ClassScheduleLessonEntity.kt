package kz.aqyldykundelik.schedule.domain

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "class_schedule_lesson")
class ClassScheduleLessonEntity(
    @Id @GeneratedValue(strategy = GenerationType.UUID) var id: UUID? = null,
    @Column(name = "schedule_id", nullable = false) var scheduleId: UUID? = null,
    @Column(name = "day_of_week", nullable = false) var dayOfWeek: Int = 1,
    @Column(name = "lesson_number", nullable = false) var lessonNumber: Int = 1,
    @Column(name = "subject_id") var subjectId: UUID? = null,
    @Column(name = "teacher_id") var teacherId: UUID? = null
)
