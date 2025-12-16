package kz.aqyldykundelik.timetable.domain
import jakarta.persistence.*
import java.util.*

@Entity @Table(name = "room")
class RoomEntity(
    @Id @GeneratedValue(strategy = GenerationType.UUID) var id: UUID? = null,
    @Column(nullable=false) var name: String? = null,
    var capacity: Int? = null
)
