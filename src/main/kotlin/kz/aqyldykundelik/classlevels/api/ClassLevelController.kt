package kz.aqyldykundelik.classlevels.api

import jakarta.validation.Valid
import kz.aqyldykundelik.classlevels.api.dto.*
import kz.aqyldykundelik.classlevels.service.ClassLevelService
import kz.aqyldykundelik.common.PageDto
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/class-levels")
class ClassLevelController(private val classLevelService: ClassLevelService) {

    @GetMapping("/all")
    fun listAll(): List<ClassLevelDto> = classLevelService.findAll()

    @GetMapping
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): PageDto<ClassLevelDto> = classLevelService.findAllPaged(page, size)

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): ClassLevelDto = classLevelService.findById(id)

    @GetMapping("/by-level/{level}")
    fun getByLevel(@PathVariable level: Int): ClassLevelDto = classLevelService.findByLevel(level)

    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody body: CreateClassLevelDto): ClassLevelDto =
        classLevelService.create(body)

    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}")
    fun update(@PathVariable id: UUID, @Valid @RequestBody body: UpdateClassLevelDto): ClassLevelDto =
        classLevelService.update(id, body)

    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID) = classLevelService.delete(id)
}
