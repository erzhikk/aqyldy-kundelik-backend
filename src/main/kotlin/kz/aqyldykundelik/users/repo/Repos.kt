package kz.aqyldykundelik.users.repo
import kz.aqyldykundelik.users.domain.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface UserRepository : JpaRepository<UserEntity, UUID> {
    fun findByEmail(email: String): UserEntity?
    fun findAllByIsDeletedFalse(pageable: Pageable): Page<UserEntity>
    fun findAllByIsDeletedTrue(pageable: Pageable): Page<UserEntity>

    // Students
    fun findAllByRoleAndIsDeletedFalse(role: String, pageable: Pageable): Page<UserEntity>
    @Query(
        value = """
            select u.*
            from app_user u
            left join school_class sc on sc.id = u.class_id
            where u.role = :role and not(u.is_deleted)
            order by
                case when sc.code is null then 1 else 0 end,
                cast(nullif(regexp_replace(sc.code, E'\\D', '', 'g'), '') as int),
                regexp_replace(sc.code, E'\\d', '', 'g'),
                u.full_name
        """,
        countQuery = """
            select count(*)
            from app_user u
            where u.role = :role and not(u.is_deleted)
        """,
        nativeQuery = true
    )
    fun findAllStudentsOrderByClassAndName(role: String, pageable: Pageable): Page<UserEntity>

    // Staff (teachers and admins)
    fun findAllByRoleInAndIsDeletedFalse(roles: List<String>, pageable: Pageable): Page<UserEntity>
    @Query(
        value = """
            select u.*
            from app_user u
            left join school_class sc on sc.id = u.class_id
            where u.role in (:roles) and not(u.is_deleted)
            order by
                case when sc.code is null then 1 else 0 end,
                cast(nullif(regexp_replace(sc.code, E'\\D', '', 'g'), '') as int),
                regexp_replace(sc.code, E'\\d', '', 'g'),
                u.full_name
        """,
        countQuery = """
            select count(*)
            from app_user u
            where u.role in (:roles) and not(u.is_deleted)
        """,
        nativeQuery = true
    )
    fun findAllStaffOrderByClassAndName(roles: List<String>, pageable: Pageable): Page<UserEntity>

    // Teachers (no pagination)
    fun findAllByRoleAndIsDeletedFalse(role: String, sort: org.springframework.data.domain.Sort): List<UserEntity>

    fun findAllByRoleAndClassIdAndIsDeletedFalse(role: String, classId: UUID, sort: Sort): List<UserEntity>
}
interface GroupRepository : JpaRepository<ClassGroupEntity, UUID>
