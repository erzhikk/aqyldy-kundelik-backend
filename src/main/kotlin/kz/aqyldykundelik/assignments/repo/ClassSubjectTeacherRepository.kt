package kz.aqyldykundelik.assignments.repo

import kz.aqyldykundelik.assignments.domain.ClassSubjectTeacherEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ClassSubjectTeacherRepository : JpaRepository<ClassSubjectTeacherEntity, UUID> {
    fun findByClassId(classId: UUID): List<ClassSubjectTeacherEntity>
    fun findByClassIdAndSubjectId(classId: UUID, subjectId: UUID): ClassSubjectTeacherEntity?
    fun findByTeacherId(teacherId: UUID): List<ClassSubjectTeacherEntity>
    fun deleteByClassId(classId: UUID)
}
