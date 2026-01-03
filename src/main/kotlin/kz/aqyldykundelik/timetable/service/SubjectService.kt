package kz.aqyldykundelik.timetable.service

import kz.aqyldykundelik.classlevels.repo.ClassLevelRepository
import kz.aqyldykundelik.common.PageDto
import kz.aqyldykundelik.timetable.api.CreateSubjectDto
import kz.aqyldykundelik.timetable.api.SubjectDto
import kz.aqyldykundelik.timetable.api.UpdateSubjectDto
import kz.aqyldykundelik.timetable.api.applyUpdate
import kz.aqyldykundelik.timetable.api.toDto
import kz.aqyldykundelik.timetable.api.toEntity
import kz.aqyldykundelik.timetable.repo.SubjectRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.*

@Service
class SubjectService(
    private val subjectRepository: SubjectRepository,
    private val classLevelRepository: ClassLevelRepository
) {

    fun search(query: String?, page: Int = 0, size: Int = 20): PageDto<SubjectDto> {
        val pageable = PageRequest.of(page, size)
        val result = subjectRepository.search(query, pageable)
        return PageDto(
            content = result.content.map { it.toDto() },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }

    fun findById(id: UUID): SubjectDto =
        subjectRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found") }
            .toDto()

    fun findByClassLevelId(classLevelId: UUID): List<SubjectDto> {
        return subjectRepository.findAllByClassLevelId(classLevelId).map { it.toDto() }
    }

    fun findByClassLevel(classLevelId: UUID, page: Int = 0, size: Int = 20): PageDto<SubjectDto> {
        val pageable = PageRequest.of(page, size, Sort.by("nameRu"))
        val result = subjectRepository.findByClassLevel_Id(classLevelId, pageable)
        return PageDto(
            content = result.content.map { it.toDto() },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }

    fun create(createDto: CreateSubjectDto): SubjectDto {
        val classLevel = classLevelRepository.findById(createDto.classLevelId)
            .orElseThrow { ResponseStatusException(HttpStatus.BAD_REQUEST, "Class level not found") }
        return subjectRepository.save(createDto.toEntity(classLevel)).toDto()
    }

    fun update(id: UUID, updateDto: UpdateSubjectDto): SubjectDto {
        val entity = subjectRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found") }

        val classLevel = updateDto.classLevelId?.let {
            classLevelRepository.findById(it)
                .orElseThrow { ResponseStatusException(HttpStatus.BAD_REQUEST, "Class level not found") }
        }

        entity.applyUpdate(updateDto, classLevel)
        return subjectRepository.save(entity).toDto()
    }

    fun delete(id: UUID) {
        if (!subjectRepository.existsById(id)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found")
        }
        subjectRepository.deleteById(id)
    }
}
