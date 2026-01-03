package kz.aqyldykundelik.timetable.api

import jakarta.validation.Valid
import kz.aqyldykundelik.common.PageDto
import kz.aqyldykundelik.timetable.service.SubjectService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/subjects")
class SubjectController(private val subjectService: SubjectService) {

    @GetMapping
    fun list(
        @RequestParam(required = false) q: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): PageDto<SubjectDto> = subjectService.search(q, page, size)

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): SubjectDto = subjectService.findById(id)

    @GetMapping("/by-class-level/{classLevelId}")
    fun getByClassLevel(@PathVariable classLevelId: UUID): List<SubjectDto> {
        return subjectService.findByClassLevelId(classLevelId)
    }

    @GetMapping("/by-class-level/{classLevelId}/paged")
    fun getByClassLevelPaged(
        @PathVariable classLevelId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): PageDto<SubjectDto> = subjectService.findByClassLevel(classLevelId, page, size)

    @PreAuthorize("hasRole('ADMIN') or hasRole('ADMIN_SCHEDULE') or hasRole('SUPER_ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody body: CreateSubjectDto): SubjectDto =
        subjectService.create(body)

    @PreAuthorize("hasRole('ADMIN') or hasRole('ADMIN_SCHEDULE') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}")
    fun update(@PathVariable id: UUID, @Valid @RequestBody body: UpdateSubjectDto): SubjectDto =
        subjectService.update(id, body)

    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID) = subjectService.delete(id)
}
