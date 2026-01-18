package kz.aqyldykundelik.curriculum.repo

import kz.aqyldykundelik.curriculum.domain.CurriculumSubjectHoursEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface CurriculumSubjectHoursRepository : JpaRepository<CurriculumSubjectHoursEntity, UUID> {
    fun findByClassLevelId(classLevelId: UUID): List<CurriculumSubjectHoursEntity>
    fun findByClassLevelIdAndSubjectId(classLevelId: UUID, subjectId: UUID): CurriculumSubjectHoursEntity?
    fun findBySubjectId(subjectId: UUID): CurriculumSubjectHoursEntity?
}
