package kz.aqyldykundelik.users.service

import kz.aqyldykundelik.attendance.repo.AttendanceRecordRepository
import kz.aqyldykundelik.classes.api.mappers.toDto
import kz.aqyldykundelik.classes.repo.ClassRepository
import kz.aqyldykundelik.common.PageDto
import kz.aqyldykundelik.config.AppProperties
import kz.aqyldykundelik.media.domain.MediaObjectStatus
import kz.aqyldykundelik.media.repo.MediaObjectRepository
import kz.aqyldykundelik.media.service.MediaCleanupService
import kz.aqyldykundelik.timetable.repo.SubjectRepository
import kz.aqyldykundelik.timetable.repo.TimetableLessonRepository
import kz.aqyldykundelik.users.api.dto.*
import kz.aqyldykundelik.users.api.mappers.*
import kz.aqyldykundelik.users.domain.UserEntity
import kz.aqyldykundelik.users.repo.GroupRepository
import kz.aqyldykundelik.users.repo.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.text.Collator
import java.util.*

@Service
class UserService(
    private val userRepository: UserRepository,
    private val classRepository: ClassRepository,
    private val attendanceRecordRepository: AttendanceRecordRepository,
    private val timetableLessonRepository: TimetableLessonRepository,
    private val subjectRepository: SubjectRepository,
    private val groupRepository: GroupRepository,
    private val mediaObjectRepository: MediaObjectRepository,
    private val mediaCleanupService: MediaCleanupService,
    private val appProperties: AppProperties
) {

    private val kazakhCollator = Collator.getInstance(Locale.forLanguageTag("kk-KZ")).apply {
        strength = Collator.PRIMARY
    }

    fun findAll(page: Int = 0, size: Int = 20): PageDto<UserDto> {
        val pageable = PageRequest.of(page, size, Sort.by("fullName"))
        val result = userRepository.findAllByIsDeletedFalse(pageable)
        return PageDto(
            content = result.content.map { it.toDto(generatePhotoUrl(it)) },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }

    fun findAllStudents(page: Int = 0, size: Int = 20): PageDto<UserDto> {
        val pageable = PageRequest.of(page, size, Sort.by("fullName"))
        val result = userRepository.findAllByRoleAndIsDeletedFalse("STUDENT", pageable)

        // Загружаем все классы одним запросом для избежания N+1
        val classIds = result.content.mapNotNull { it.classId }
        val classes = if (classIds.isNotEmpty()) {
            classRepository.findAllById(classIds).associateBy { it.id }
        } else {
            emptyMap()
        }

        val students = result.content.map { student ->
            val classEntity = student.classId?.let { classes[it] }
            val photoUrl = generatePhotoUrl(student)
            student.toDtoWithClass(classEntity, photoUrl)
        }

        return PageDto(
            content = students,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }

    fun findAllStaff(page: Int = 0, size: Int = 20): PageDto<UserDto> {
        val staffRoles = listOf("TEACHER", "ADMIN", "ADMIN_SCHEDULE", "ADMIN_ASSESSMENT", "SUPER_ADMIN")
        val pageable = PageRequest.of(page, size, Sort.by("fullName"))
        val result = userRepository.findAllByRoleInAndIsDeletedFalse(staffRoles, pageable)
        return PageDto(
            content = result.content.map { it.toDto(generatePhotoUrl(it)) },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }

    fun findAllTeachersNoPagination(): List<UserDto> {
        val result = userRepository.findAllByRoleAndIsDeletedFalse("TEACHER", Sort.by("fullName"))
        return result.map { it.toDto(generatePhotoUrl(it)) }
    }

    fun findAllDeleted(page: Int = 0, size: Int = 20): PageDto<UserDto> {
        val pageable = PageRequest.of(page, size, Sort.by("fullName"))
        val result = userRepository.findAllByIsDeletedTrue(pageable)
        return PageDto(
            content = result.content.map { it.toDto(generatePhotoUrl(it)) },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }

    fun findById(id: UUID): UserDto {
        val user = userRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }
        return user.toDto(generatePhotoUrl(user))
    }

    fun create(createDto: CreateUserDto): UserDto {
        // Валидация: classId можно присваивать только студентам
        if (createDto.role != "STUDENT" && createDto.classId != null) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "classId can only be assigned to users with role STUDENT"
            )
        }

        // Валидация photoMediaId
        createDto.photoMediaId?.let { validatePhotoMediaId(it) }

        val savedUser = userRepository.save(createDto.toEntity())
        return savedUser.toDto(generatePhotoUrl(savedUser))
    }

    fun update(id: UUID, updateDto: UpdateUserDto): UserDto {
        val user = userRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }

        // Определяем финальную роль после обновления
        val finalRole = updateDto.role ?: user.role

        // Валидация: classId можно присваивать только студентам
        if (finalRole != "STUDENT" && updateDto.classId != null) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "classId can only be assigned to users with role STUDENT"
            )
        }

        // Если меняем роль на не-STUDENT, обнуляем classId автоматически
        if (updateDto.role != null && updateDto.role != "STUDENT" && user.classId != null) {
            user.classId = null
        }

        // Валидация photoMediaId
        updateDto.photoMediaId?.let { validatePhotoMediaId(it) }

        // Если обновляется фото - удалить старое из хранилища
        if (updateDto.photoMediaId != null && user.photoMediaId != null && updateDto.photoMediaId != user.photoMediaId) {
            try {
                mediaCleanupService.deleteMediaObject(user.photoMediaId!!)
            } catch (e: Exception) {
                // Игнорируем ошибки удаления - продолжаем загружать новое фото
            }
        }

        val updatedUser = user.applyUpdate(updateDto)
        val savedUser = userRepository.save(updatedUser)

        return savedUser.toDto(generatePhotoUrl(savedUser))
    }

    fun deactivate(id: UUID): UserDto {
        val user = userRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }
        user.isActive = false
        val savedUser = userRepository.save(user)
        return savedUser.toDto(generatePhotoUrl(savedUser))
    }

    fun activate(id: UUID): UserDto {
        val user = userRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }
        user.isActive = true
        val savedUser = userRepository.save(user)
        return savedUser.toDto(generatePhotoUrl(savedUser))
    }

    fun delete(id: UUID): UserDto {
        val user = userRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }

        // Удалить фото пользователя из хранилища
        user.photoMediaId?.let {
            try {
                mediaCleanupService.deleteMediaObject(it)
            } catch (e: Exception) {
                // Игнорируем ошибки удаления
            }
        }

        user.isDeleted = true
        user.isActive = false
        val savedUser = userRepository.save(user)
        return savedUser.toDto(generatePhotoUrl(savedUser))
    }

    fun deletePhoto(id: UUID): UserDto {
        val user = userRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }

        // Проверяем, есть ли фото для удаления
        val photoMediaId = user.photoMediaId
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User does not have a photo")

        // Удаляем фото из хранилища
        try {
            mediaCleanupService.deleteMediaObject(photoMediaId)
        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to delete photo from storage: ${e.message}"
            )
        }

        // Обнуляем photoMediaId
        user.photoMediaId = null
        val savedUser = userRepository.save(user)
        return savedUser.toDto(generatePhotoUrl(savedUser))
    }

    /**
     * Получить карточку студента с расширенной информацией
     */
    fun getStudentCard(id: UUID): StudentCardDto {
        val user = userRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }

        // Проверяем, что это студент
        if (user.role != "STUDENT") {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a student")
        }

        // Загружаем информацию о классе
        val classEntity = user.classId?.let { classRepository.findById(it).orElse(null) }

        // Получаем статистику посещаемости
        val attendanceStats = calculateAttendanceStats(id)

        // Генерируем presigned URL для фото
        val photoUrl = generatePhotoUrl(user)

        return user.toStudentCard(classEntity, attendanceStats, photoUrl)
    }

    /**
     * Получить карточку сотрудника с расширенной информацией
     */
    fun getStaffCard(id: UUID): StaffCardDto {
        val user = userRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }

        // Проверяем, что это сотрудник
        val staffRoles = listOf("TEACHER", "ADMIN", "ADMIN_SCHEDULE", "ADMIN_ASSESSMENT", "SUPER_ADMIN")
        if (user.role !in staffRoles) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a staff member")
        }

        // Загружаем класс, в котором пользователь является классным руководителем
        val classAsTeacher = classRepository.findAll()
            .firstOrNull { it.classTeacherId == id }
            ?.toDto()

        // Получаем список предметов, которые преподает
        val taughtSubjects = getTaughtSubjects(id)

        // Генерируем presigned URL для фото
        val photoUrl = generatePhotoUrl(user)

        return user.toStaffCard(classAsTeacher, taughtSubjects, photoUrl)
    }

    /**
     * Рассчитать статистику посещаемости для студента
     */
    private fun calculateAttendanceStats(studentId: UUID): AttendanceStatsDto? {
        val totalRecords = attendanceRecordRepository.countTotalRecordsByStudent(studentId)

        if (totalRecords == 0L) {
            return null // Нет данных о посещаемости
        }

        val present = attendanceRecordRepository.countByStudentAndStatus(studentId, "PRESENT")
        val late = attendanceRecordRepository.countByStudentAndStatus(studentId, "LATE")
        val absent = attendanceRecordRepository.countByStudentAndStatus(studentId, "ABSENT")
        val excused = attendanceRecordRepository.countByStudentAndStatus(studentId, "EXCUSED")

        val attendanceRate = if (totalRecords > 0) {
            ((present + late).toDouble() / totalRecords.toDouble()) * 100.0
        } else {
            0.0
        }

        return AttendanceStatsDto(
            totalLessons = totalRecords,
            present = present,
            late = late,
            absent = absent,
            excused = excused,
            attendanceRate = String.format("%.2f", attendanceRate).toDouble()
        )
    }

    /**
     * Получить список предметов, которые преподает учитель
     */
    private fun getTaughtSubjects(teacherId: UUID): List<TaughtSubjectDto> {
        // Получаем все уроки учителя из расписания
        val pageable = PageRequest.of(0, 1000) // получаем все
        val lessons = timetableLessonRepository.findByTeacherIdOrderByWeekdayAscStartTimeAsc(teacherId, pageable)

        // Группируем по предметам
        val subjectMap = lessons.content
            .groupBy { it.subjectId }
            .mapNotNull { (subjectId, lessonsList) ->
                subjectId?.let { sid ->
                    val subject = subjectRepository.findById(sid).orElse(null)
                    val groups = lessonsList.mapNotNull { lesson ->
                        lesson.groupId?.let { groupRepository.findById(it).orElse(null)?.name }
                    }.distinct()

                    subject?.let {
                        TaughtSubjectDto(
                            subjectId = sid,
                            subjectName = it.nameRu ?: it.nameKk ?: it.nameEn ?: "Unknown",
                            groups = groups
                        )
                    }
                }
            }

        return subjectMap
    }

    /**
     * Валидация photoMediaId перед сохранением
     * Проверяет существование медиа объекта и его статус READY
     */
    private fun validatePhotoMediaId(photoMediaId: UUID) {
        val mediaObject = mediaObjectRepository.findById(photoMediaId).orElse(null)
            ?: throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Media object with id $photoMediaId not found"
            )

        if (mediaObject.status != MediaObjectStatus.READY) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Media object must have READY status, current status: ${mediaObject.status}"
            )
        }
    }

    /**
     * Генерация URL для фотографии пользователя через бэкенд proxy
     * Возвращает URL только если photo_media_id существует и статус READY
     * Добавляет параметр ?v= для cache-busting (первые 8 символов sha256 или updatedAt)
     */
    private fun generatePhotoUrl(user: UserEntity): String? {
        val photoMediaId = user.photoMediaId ?: return null

        val mediaObject = mediaObjectRepository.findById(photoMediaId).orElse(null)
            ?: return null

        // Проверяем статус - только READY
        if (mediaObject.status != MediaObjectStatus.READY) {
            return null
        }

        // Генерируем версию для cache-busting
        val version = mediaObject.sha256?.take(8)
            ?: mediaObject.updatedAt?.toEpochSecond()?.toString()
            ?: "1"

        // Генерируем URL к эндпоинту бэкенда с параметром версии
        return "${appProperties.baseUrl}/api/media/photo/${photoMediaId}?v=${version}"
    }
}
