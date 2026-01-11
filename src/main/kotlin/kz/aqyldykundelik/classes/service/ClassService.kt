package kz.aqyldykundelik.classes.service

import kz.aqyldykundelik.classes.api.dto.ClassDetailDto
import kz.aqyldykundelik.classes.api.dto.ClassDto
import kz.aqyldykundelik.classes.api.dto.CreateClassDto
import kz.aqyldykundelik.classes.api.dto.UpdateClassDto
import kz.aqyldykundelik.classes.api.mappers.applyUpdate
import kz.aqyldykundelik.classes.api.mappers.toDto
import kz.aqyldykundelik.classes.api.mappers.toEntity
import kz.aqyldykundelik.classes.repo.ClassRepository
import kz.aqyldykundelik.common.PageDto
import kz.aqyldykundelik.users.api.dto.UserDto
import kz.aqyldykundelik.users.api.mappers.toDto
import kz.aqyldykundelik.users.repo.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.*

@Service
class ClassService(
    private val classRepository: ClassRepository,
    private val userRepository: UserRepository
) {

    fun findAll(page: Int = 0, size: Int = 20): PageDto<ClassDto> {
        val pageable = PageRequest.of(page, size)
        val result = classRepository.findAllOrderByGradeAndLetter(pageable)
        return PageDto(
            content = mapToDtosWithTeacherNames(result.content),
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }

    fun findAllNoPagination(): List<ClassDto> {
        return mapToDtosWithTeacherNames(classRepository.findAllOrderByGradeAndLetter())
    }

    fun findById(id: UUID): ClassDto =
        classRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found") }
            .let { mapToDtosWithTeacherNames(listOf(it)).first() }

    fun findDetailById(id: UUID): ClassDetailDto {
        val classEntity = classRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found") }
        val classDto = mapToDtosWithTeacherNames(listOf(classEntity)).first()
        val students = userRepository.findAllByRoleAndClassIdAndIsDeletedFalse(
            "STUDENT",
            id,
            Sort.by("fullName")
        ).map { it.toDto() }
        val teacher = classEntity.classTeacherId?.let { teacherId ->
            userRepository.findById(teacherId).orElse(null)
        }?.takeUnless { it.isDeleted }?.toDto()

        return ClassDetailDto(
            classInfo = classDto,
            students = students,
            teacher = teacher
        )
    }

    fun findByCode(code: String): ClassDto =
        classRepository.findByCode(code)
            ?.let { mapToDtosWithTeacherNames(listOf(it)).first() }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found")

    fun findByLangType(langType: String, page: Int = 0, size: Int = 20): PageDto<ClassDto> {
        val pageable = PageRequest.of(page, size)
        val result = classRepository.findByLangTypeOrderByGradeAndLetter(langType, pageable)
        return PageDto(
            content = mapToDtosWithTeacherNames(result.content),
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }

    fun findByTeacher(teacherId: UUID, page: Int = 0, size: Int = 20): PageDto<ClassDto> {
        val pageable = PageRequest.of(page, size)
        val result = classRepository.findByClassTeacherIdOrderByGradeAndLetter(teacherId, pageable)
        return PageDto(
            content = mapToDtosWithTeacherNames(result.content),
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }

    fun findStudentsByClassId(classId: UUID): List<UserDto> {
        if (!classRepository.existsById(classId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found")
        }

        return userRepository.findAllByRoleAndClassIdAndIsDeletedFalse(
            "STUDENT",
            classId,
            Sort.by("fullName")
        ).map { it.toDto() }
    }

    fun create(createDto: CreateClassDto): ClassDto =
        mapToDtosWithTeacherNames(listOf(classRepository.save(createDto.toEntity()))).first()

    fun update(id: UUID, updateDto: UpdateClassDto): ClassDto {
        val entity = classRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found") }
        entity.applyUpdate(updateDto)
        return mapToDtosWithTeacherNames(listOf(classRepository.save(entity))).first()
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

    private fun mapToDtosWithTeacherNames(classes: List<kz.aqyldykundelik.classes.domain.ClassEntity>): List<ClassDto> {
        val teacherIds = classes.mapNotNull { it.classTeacherId }.distinct()
        val teachersById = if (teacherIds.isEmpty()) {
            emptyMap()
        } else {
            userRepository.findAllById(teacherIds).associateBy { it.id }
        }

        return classes.map { classEntity ->
            val teacherFullName = classEntity.classTeacherId?.let { teachersById[it]?.fullName }
            classEntity.toDto(teacherFullName)
        }
    }
}
