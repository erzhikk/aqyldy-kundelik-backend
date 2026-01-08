package kz.aqyldykundelik.users.api

import jakarta.validation.Valid
import kz.aqyldykundelik.common.PageDto
import kz.aqyldykundelik.users.api.dto.*
import kz.aqyldykundelik.users.service.UserService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/users")
class UsersController(private val userService: UserService) {

    @GetMapping
    fun all(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): PageDto<UserDto> = userService.findAll(page, size)

    @GetMapping("/students")
    fun students(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): PageDto<UserDto> = userService.findAllStudents(page, size)

    @GetMapping("/staff")
    fun staff(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): PageDto<UserDto> = userService.findAllStaff(page, size)

    @GetMapping("/teachers/all")
    fun teachersAll(): List<UserDto> = userService.findAllTeachersNoPagination()

    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @GetMapping("/deleted")
    fun allDeleted(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): PageDto<UserDto> = userService.findAllDeleted(page, size)

    @GetMapping("/{id}")
    fun one(@PathVariable id: UUID): UserDto = userService.findById(id)

    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @PostMapping
    fun create(@Valid @RequestBody body: CreateUserDto): UserDto = userService.create(body)

    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}")
    fun update(@PathVariable id: UUID, @Valid @RequestBody body: UpdateUserDto): UserDto =
        userService.update(id, body)

    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}/deactivate")
    fun deactivate(@PathVariable id: UUID): UserDto = userService.deactivate(id)

    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}/activate")
    fun activate(@PathVariable id: UUID): UserDto = userService.activate(id)

    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: UUID): UserDto = userService.delete(id)

    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}/photo")
    fun deletePhoto(@PathVariable id: UUID): UserDto = userService.deletePhoto(id)

    @GetMapping("/student/{id}/card")
    fun getStudentCard(@PathVariable id: UUID): StudentCardDto = userService.getStudentCard(id)

    @GetMapping("/staff/{id}/card")
    fun getStaffCard(@PathVariable id: UUID): StaffCardDto = userService.getStaffCard(id)
}
