package kz.aqyldykundelik.classes.api

import jakarta.validation.Valid
import kz.aqyldykundelik.classes.api.dto.*
import kz.aqyldykundelik.classes.service.ClassService
import kz.aqyldykundelik.common.PageDto
import kz.aqyldykundelik.users.api.dto.UserDto
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/classes")
class ClassController(private val classService: ClassService) {

    @GetMapping
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): PageDto<ClassDto> = classService.findAll(page, size)

    @GetMapping("/all")
    fun listAll(): List<ClassDto> = classService.findAllNoPagination()

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): ClassDetailDto = classService.findDetailById(id)

    @GetMapping("/{id}/students")
    fun getStudents(@PathVariable id: UUID): List<UserDto> = classService.findStudentsByClassId(id)

    @GetMapping("/by-code/{code}")
    fun getByCode(@PathVariable code: String): ClassDto = classService.findByCode(code)

    @GetMapping("/by-lang/{langType}")
    fun getByLangType(
        @PathVariable langType: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): PageDto<ClassDto> = classService.findByLangType(langType, page, size)

    @GetMapping("/by-teacher/{teacherId}")
    fun getByTeacher(
        @PathVariable teacherId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): PageDto<ClassDto> = classService.findByTeacher(teacherId, page, size)

    @PreAuthorize("hasRole('ADMIN') or hasRole('ADMIN_SCHEDULE') or hasRole('SUPER_ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody body: CreateClassDto): ClassDto = classService.create(body)

    @PreAuthorize("hasRole('ADMIN') or hasRole('ADMIN_SCHEDULE') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}")
    fun update(@PathVariable id: UUID, @Valid @RequestBody body: UpdateClassDto): ClassDto =
        classService.update(id, body)

    @PreAuthorize("hasRole('ADMIN') or hasRole('ADMIN_SCHEDULE') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}/teacher")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeTeacher(@PathVariable id: UUID) = classService.removeTeacher(id)

    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID) = classService.delete(id)
}
