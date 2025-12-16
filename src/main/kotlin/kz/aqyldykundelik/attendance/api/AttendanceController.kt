package kz.aqyldykundelik.attendance.api

import jakarta.validation.Valid
import kz.aqyldykundelik.attendance.domain.AttendanceRecordEntity
import kz.aqyldykundelik.attendance.domain.AttendanceSheetEntity
import kz.aqyldykundelik.attendance.repo.AttendanceRecordRepository
import kz.aqyldykundelik.attendance.repo.AttendanceSheetRepository
import kz.aqyldykundelik.common.PageDto
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

@RestController
@RequestMapping("/api/attendance")
class AttendanceController(
    private val sheets: AttendanceSheetRepository,
    private val records: AttendanceRecordRepository
) {
    @GetMapping("/sheets/{id}")
    fun getSheet(@PathVariable id: UUID): AttendanceSheetDto {
        val s = sheets.findById(id).orElseThrow()
        return AttendanceSheetDto(s.id!!, s.lessonDate.toString(), s.groupId!!, s.subjectId!!)
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @PostMapping("/sheets")
    fun createSheet(@Valid @RequestBody body: CreateSheetDto): AttendanceSheetDto {
        val s = AttendanceSheetEntity(
            lessonDate = LocalDate.parse(body.lessonDate),
            groupId = body.groupId,
            subjectId = body.subjectId
        )
        val saved = sheets.save(s)
        return AttendanceSheetDto(saved.id!!, saved.lessonDate.toString(), saved.groupId!!, saved.subjectId!!)
    }

    @GetMapping("/sheets/by-group")
    fun listByGroup(
        @RequestParam groupId: UUID,
        @RequestParam date: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): PageDto<AttendanceSheetDto> {
        val pageable = PageRequest.of(page, size, Sort.by("lessonDate"))
        val result = sheets.findByGroupIdAndLessonDate(groupId, LocalDate.parse(date), pageable)
        return PageDto(
            content = result.content.map { AttendanceSheetDto(it.id!!, it.lessonDate.toString(), it.groupId!!, it.subjectId!!) },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @PostMapping("/sheets/{id}/mark")
    fun bulkMark(@PathVariable id: UUID, @Valid @RequestBody body: BulkMarkDto) {
        records.deleteAll(records.findBySheetId(id)) // перезапись
        val now = OffsetDateTime.now()
        val list = body.items.map {
            AttendanceRecordEntity(
                sheetId = id,
                studentId = it.studentId,
                status = it.status,
                reason = it.reason,
                markedAt = now,
                markedBy = body.markedBy
            )
        }
        records.saveAll(list)
    }
}
