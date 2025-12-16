package kz.aqyldykundelik.timetable.api

import jakarta.validation.Valid
import kz.aqyldykundelik.common.PageDto
import kz.aqyldykundelik.timetable.domain.TimetableLessonEntity
import kz.aqyldykundelik.timetable.repo.TimetableLessonRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalTime
import java.util.*

@RestController
@RequestMapping("/api/timetable")
class TimetableController(
    private val lessons: TimetableLessonRepository
) {
    @GetMapping("/by-group/{groupId}")
    fun byGroup(
        @PathVariable groupId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): PageDto<LessonDto> {
        val pageable = PageRequest.of(page, size, Sort.by("weekday", "startTime"))
        val result = lessons.findByGroupIdOrderByWeekdayAscStartTimeAsc(groupId, pageable)
        return PageDto(
            content = result.content.map { it.toDto() },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }

    @GetMapping("/by-teacher/{teacherId}")
    fun byTeacher(
        @PathVariable teacherId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): PageDto<LessonDto> {
        val pageable = PageRequest.of(page, size, Sort.by("weekday", "startTime"))
        val result = lessons.findByTeacherIdOrderByWeekdayAscStartTimeAsc(teacherId, pageable)
        return PageDto(
            content = result.content.map { it.toDto() },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }

    @PreAuthorize("hasRole('ADMIN_SCHEDULE') or hasRole('SUPER_ADMIN')")
    @PostMapping("/lessons")
    fun create(@Valid @RequestBody body: CreateLessonDto): LessonDto {
        val e = TimetableLessonEntity(
            id = null,
            groupId = body.groupId,
            subjectId = body.subjectId,
            teacherId = body.teacherId,
            roomId = body.roomId,
            weekday = body.weekday,
            startTime = LocalTime.parse(body.startTime),
            endTime = LocalTime.parse(body.endTime)
        )
        return lessons.save(e).toDto()
    }

    @PreAuthorize("hasRole('ADMIN_SCHEDULE') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/lessons/{id}")
    fun delete(@PathVariable id: UUID) {
        lessons.deleteById(id)
    }
}

private fun TimetableLessonEntity.toDto() = LessonDto(
    id = this.id!!,
    groupId = this.groupId!!,
    subjectId = this.subjectId!!,
    teacherId = this.teacherId!!,
    roomId = this.roomId,
    weekday = this.weekday!!,
    startTime = this.startTime.toString(),
    endTime = this.endTime.toString()
)
