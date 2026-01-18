package kz.aqyldykundelik.assignments.api

import jakarta.validation.Valid
import kz.aqyldykundelik.assignments.api.dto.*
import kz.aqyldykundelik.assignments.service.AssignmentsService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/admin/assignments")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ADMIN_SCHEDULE')")
class AssignmentsController(private val assignmentsService: AssignmentsService) {

    @GetMapping("/classes/{classId}")
    fun getClassAssignments(@PathVariable classId: UUID): ClassAssignmentsDto =
        assignmentsService.getClassAssignments(classId)

    @PutMapping("/classes/{classId}")
    fun updateClassAssignments(
        @PathVariable classId: UUID,
        @Valid @RequestBody updateDto: UpdateClassAssignmentsDto
    ): ClassAssignmentsDto = assignmentsService.updateClassAssignments(classId, updateDto)

    @GetMapping("/teachers")
    fun getTeachers(@RequestParam(required = false) q: String?): List<TeacherDto> =
        assignmentsService.getTeachers(q)
}
