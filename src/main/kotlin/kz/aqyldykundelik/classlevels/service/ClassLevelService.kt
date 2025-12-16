package kz.aqyldykundelik.classlevels.service

import kz.aqyldykundelik.classlevels.api.dto.ClassLevelDto
import kz.aqyldykundelik.classlevels.api.dto.CreateClassLevelDto
import kz.aqyldykundelik.classlevels.api.dto.UpdateClassLevelDto
import kz.aqyldykundelik.classlevels.api.mappers.applyUpdate
import kz.aqyldykundelik.classlevels.api.mappers.toDto
import kz.aqyldykundelik.classlevels.api.mappers.toEntity
import kz.aqyldykundelik.classlevels.repo.ClassLevelRepository
import kz.aqyldykundelik.common.PageDto
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.*

@Service
class ClassLevelService(private val classLevelRepository: ClassLevelRepository) {

    fun findAll(page: Int = 0, size: Int = 20): PageDto<ClassLevelDto> {
        val pageable = PageRequest.of(page, size, Sort.by("level"))
        val result = classLevelRepository.findAll(pageable)
        return PageDto(
            content = result.content.map { it.toDto() },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }

    fun findById(id: UUID): ClassLevelDto =
        classLevelRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Class level not found") }
            .toDto()

    fun findByLevel(level: Int): ClassLevelDto =
        classLevelRepository.findByLevel(level)
            ?.toDto()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Class level not found")

    fun create(createDto: CreateClassLevelDto): ClassLevelDto {
        if (classLevelRepository.existsByLevel(createDto.level)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Class level ${createDto.level} already exists")
        }
        return classLevelRepository.save(createDto.toEntity()).toDto()
    }

    fun update(id: UUID, updateDto: UpdateClassLevelDto): ClassLevelDto {
        val entity = classLevelRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Class level not found") }

        // Check if level is being changed and if new level already exists
        if (updateDto.level != null && updateDto.level != entity.level && classLevelRepository.existsByLevel(updateDto.level)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Class level ${updateDto.level} already exists")
        }

        entity.applyUpdate(updateDto)
        return classLevelRepository.save(entity).toDto()
    }

    fun delete(id: UUID) {
        if (!classLevelRepository.existsById(id)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Class level not found")
        }
        classLevelRepository.deleteById(id)
    }
}
