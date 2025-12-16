package kz.aqyldykundelik.users.domain
import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity @Table(name = "class_group")
class ClassGroupEntity(
    @Id @GeneratedValue(strategy = GenerationType.UUID) var id: UUID? = null,
    @Column(nullable = false) var name: String,
    var year: Int? = null,
    @Column(name = "created_at") var createdAt: OffsetDateTime? = null,
    @Column(name = "updated_at") var updatedAt: OffsetDateTime? = null,
)
