package kz.aqyldykundelik.curriculum.api

import jakarta.validation.Valid
import kz.aqyldykundelik.curriculum.api.dto.*
import kz.aqyldykundelik.curriculum.service.CurriculumService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/admin/curriculum")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ADMIN_SCHEDULE')")
class CurriculumController(private val curriculumService: CurriculumService) {

    @GetMapping("/levels")
    fun getLevels(): List<ClassLevelDto> = curriculumService.getAllLevels()

    @GetMapping("/levels/{classLevelId}/subjects")
    fun getSubjectsByLevel(@PathVariable classLevelId: UUID): CurriculumLevelSubjectsDto =
        curriculumService.getSubjectsByLevel(classLevelId)

    @PutMapping("/levels/{classLevelId}/subjects")
    fun updateSubjectHours(
        @PathVariable classLevelId: UUID,
        @Valid @RequestBody updateDto: UpdateCurriculumSubjectsDto
    ): CurriculumLevelSubjectsDto = curriculumService.updateSubjectHours(classLevelId, updateDto)

    @PutMapping("/levels/{classLevelId}/settings")
    fun updateLevelSettings(
        @PathVariable classLevelId: UUID,
        @Valid @RequestBody updateDto: UpdateClassLevelSettingsDto
    ): ClassLevelDto = curriculumService.updateLevelSettings(classLevelId, updateDto)
}
