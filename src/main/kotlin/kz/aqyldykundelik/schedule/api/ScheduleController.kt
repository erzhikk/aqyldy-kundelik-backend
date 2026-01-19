package kz.aqyldykundelik.schedule.api

import jakarta.validation.Valid
import kz.aqyldykundelik.schedule.api.dto.*
import kz.aqyldykundelik.schedule.service.ScheduleService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/admin/schedule")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ADMIN_SCHEDULE')")
class ScheduleController(private val scheduleService: ScheduleService) {

    @GetMapping("/classes/{classId}")
    fun getClassSchedule(@PathVariable classId: UUID): ClassScheduleDto =
        scheduleService.getClassSchedule(classId)

    @PutMapping("/classes/{classId}")
    fun updateClassSchedule(
        @PathVariable classId: UUID,
        @Valid @RequestBody updateDto: UpdateClassScheduleDto
    ): ClassScheduleDto = scheduleService.updateClassSchedule(classId, updateDto)

    @PostMapping("/classes/{classId}/activate")
    fun activateSchedule(@PathVariable classId: UUID): ClassScheduleDto =
        scheduleService.activateSchedule(classId)

    @ExceptionHandler(ScheduleActivationException::class)
    fun handleScheduleActivationException(ex: ScheduleActivationException): ResponseEntity<ClassScheduleDto> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(ex.scheduleDto)
}
