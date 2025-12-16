package kz.aqyldykundelik.classes.service

import kz.aqyldykundelik.classes.api.dto.ClassDto
import kz.aqyldykundelik.classes.api.dto.CreateClassDto
import kz.aqyldykundelik.classes.api.dto.UpdateClassDto
import kz.aqyldykundelik.classes.api.mappers.applyUpdate
import kz.aqyldykundelik.classes.api.mappers.toDto
import kz.aqyldykundelik.classes.api.mappers.toEntity
import kz.aqyldykundelik.classes.repo.ClassRepository
import kz.aqyldykundelik.common.PageDto
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.*

@Service
class ClassService(private val classRepository: ClassRepository) {

    fun findAll(page: Int = 0, size: Int = 20): PageDto<ClassDto> {
        val pageable = PageRequest.of(page, size, Sort.by("code"))
        val result = classRepository.findAll(pageable)
        return PageDto(
            content = result.content.map { it.toDto() },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }

    fun findAllNoPagination(): List<ClassDto> {
        return classRepository.findAll(Sort.by("code")).map { it.toDto() }
    }

    fun findById(id: UUID): ClassDto =
        classRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found") }
            .toDto()

    fun findByCode(code: String): ClassDto =
        classRepository.findByCode(code)
            ?.toDto()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found")

    fun findByLangType(langType: String, page: Int = 0, size: Int = 20): PageDto<ClassDto> {
        val pageable = PageRequest.of(page, size, Sort.by("code"))
        val result = classRepository.findByLangType(langType, pageable)
        return PageDto(
            content = result.content.map { it.toDto() },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }

    fun findByTeacher(teacherId: UUID, page: Int = 0, size: Int = 20): PageDto<ClassDto> {
        val pageable = PageRequest.of(page, size, Sort.by("code"))
        val result = classRepository.findByClassTeacherId(teacherId, pageable)
        return PageDto(
            content = result.content.map { it.toDto() },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }

    fun create(createDto: CreateClassDto): ClassDto =
        classRepository.save(createDto.toEntity()).toDto()

    fun update(id: UUID, updateDto: UpdateClassDto): ClassDto {
        val entity = classRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found") }
        entity.applyUpdate(updateDto)
        return classRepository.save(entity).toDto()
    }

    fun removeTeacher(id: UUID) {
        val entity = classRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found") }
        entity.classTeacherId = null
        classRepository.save(entity)
    }

    fun delete(id: UUID) {
        if (!classRepository.existsById(id)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found")
        }
        classRepository.deleteById(id)
    }
}
