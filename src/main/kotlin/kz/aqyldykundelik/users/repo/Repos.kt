package kz.aqyldykundelik.users.repo
import kz.aqyldykundelik.users.domain.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UserRepository : JpaRepository<UserEntity, UUID> {
    fun findByEmail(email: String): UserEntity?
    fun findAllByIsDeletedFalse(pageable: Pageable): Page<UserEntity>
    fun findAllByIsDeletedTrue(pageable: Pageable): Page<UserEntity>

    // Students
    fun findAllByRoleAndIsDeletedFalse(role: String, pageable: Pageable): Page<UserEntity>

    // Staff (teachers and admins)
    fun findAllByRoleInAndIsDeletedFalse(roles: List<String>, pageable: Pageable): Page<UserEntity>
}
interface GroupRepository : JpaRepository<ClassGroupEntity, UUID>
