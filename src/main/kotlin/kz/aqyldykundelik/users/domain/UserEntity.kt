package kz.aqyldykundelik.users.domain

import jakarta.persistence.*
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "app_user")
class UserEntity(
    @Id @GeneratedValue(strategy = GenerationType.UUID) var id: UUID? = null,
    @Column(nullable = false, unique = true) var email: String? = null,
    @Column(name = "full_name", nullable = false) var fullName: String? = null,
    @Column(nullable = false) var role: String? = null,
    @Column(nullable = false) var status: String = "ACTIVE",
    @Column(name = "is_active", nullable = false) var isActive: Boolean = true,
    @Column(name = "is_deleted", nullable = false) var isDeleted: Boolean = false,
    @Column(name = "password_hash", nullable = false) var passwordHash: String = "",
    @Column(name = "class_id") var classId: UUID? = null,
    @Column(name = "date_of_birth") var dateOfBirth: LocalDate? = null,
    @Column(name = "created_at") var createdAt: OffsetDateTime? = null,
    @Column(name = "updated_at") var updatedAt: OffsetDateTime? = null,
) {
    @PrePersist
    fun prePersist() {
        val now = OffsetDateTime.now()
        if (createdAt == null) createdAt = now
        if (updatedAt == null) updatedAt = now
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = OffsetDateTime.now()
    }
}
