package kz.aqyldykundelik.curriculum.service

import kz.aqyldykundelik.classlevels.repo.ClassLevelRepository
import kz.aqyldykundelik.curriculum.api.dto.*
import kz.aqyldykundelik.curriculum.domain.CurriculumSubjectHoursEntity
import kz.aqyldykundelik.curriculum.repo.CurriculumSubjectHoursRepository
import kz.aqyldykundelik.timetable.repo.SubjectRepository
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.*

@Service
class CurriculumService(
    private val classLevelRepository: ClassLevelRepository,
    private val subjectRepository: SubjectRepository,
    private val curriculumSubjectHoursRepository: CurriculumSubjectHoursRepository
) {

    fun getAllLevels(): List<ClassLevelDto> {
        return classLevelRepository.findAll(Sort.by("level"))
            .filter { it.level in 1..11 }
            .map { ClassLevelDto(it.id!!, it.level!!, it.nameRu!!, it.nameKk!!, it.maxLessonsPerDay, it.daysPerWeek) }
    }

    fun getSubjectsByLevel(classLevelId: UUID): CurriculumLevelSubjectsDto {
        val classLevel = classLevelRepository.findById(classLevelId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Class level not found") }

        val subjects = subjectRepository.findAllByClassLevelId(classLevelId)
        val hoursMap = curriculumSubjectHoursRepository.findByClassLevelId(classLevelId)
            .associateBy { it.subjectId }

        val subjectDtos = subjects.map { subject ->
            val hours = hoursMap[subject.id]?.hoursPerWeek ?: 0
            CurriculumSubjectDto(
                subjectId = subject.id!!,
                nameRu = subject.nameRu!!,
                nameKk = subject.nameKk!!,
                nameEn = subject.nameEn!!,
                hoursPerWeek = hours
            )
        }

        val totalHours = subjectDtos.sumOf { it.hoursPerWeek }
        val maxHoursPerWeek = classLevel.maxLessonsPerDay * classLevel.daysPerWeek
        val warnings = mutableListOf<String>()

        if (totalHours > maxHoursPerWeek) {
            warnings.add("Сумма часов ($totalHours) превышает максимум ($maxHoursPerWeek = ${classLevel.maxLessonsPerDay} уроков/день × ${classLevel.daysPerWeek} дней)")
        }

        return CurriculumLevelSubjectsDto(
            classLevelId = classLevelId,
            maxLessonsPerDay = classLevel.maxLessonsPerDay,
            daysPerWeek = classLevel.daysPerWeek,
            maxHoursPerWeek = maxHoursPerWeek,
            totalHoursPerWeek = totalHours,
            subjects = subjectDtos,
            warnings = warnings
        )
    }

    @Transactional
    fun updateSubjectHours(classLevelId: UUID, updateDto: UpdateCurriculumSubjectsDto): CurriculumLevelSubjectsDto {
        val classLevel = classLevelRepository.findById(classLevelId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Class level not found") }

        // Update level settings if provided
        updateDto.maxLessonsPerDay?.let { classLevel.maxLessonsPerDay = it }
        updateDto.daysPerWeek?.let { classLevel.daysPerWeek = it }
        classLevelRepository.save(classLevel)

        for (item in updateDto.items) {
            val existing = curriculumSubjectHoursRepository.findByClassLevelIdAndSubjectId(classLevelId, item.subjectId)
            if (existing != null) {
                existing.hoursPerWeek = item.hoursPerWeek
                curriculumSubjectHoursRepository.save(existing)
            } else {
                val entity = CurriculumSubjectHoursEntity(
                    classLevelId = classLevelId,
                    subjectId = item.subjectId,
                    hoursPerWeek = item.hoursPerWeek
                )
                curriculumSubjectHoursRepository.save(entity)
            }
        }

        return getSubjectsByLevel(classLevelId)
    }

    @Transactional
    fun updateLevelSettings(classLevelId: UUID, updateDto: UpdateClassLevelSettingsDto): ClassLevelDto {
        val classLevel = classLevelRepository.findById(classLevelId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Class level not found") }

        updateDto.maxLessonsPerDay?.let { classLevel.maxLessonsPerDay = it }
        updateDto.daysPerWeek?.let { classLevel.daysPerWeek = it }

        val saved = classLevelRepository.save(classLevel)

        return ClassLevelDto(saved.id!!, saved.level!!, saved.nameRu!!, saved.nameKk!!, saved.maxLessonsPerDay, saved.daysPerWeek)
    }
}
