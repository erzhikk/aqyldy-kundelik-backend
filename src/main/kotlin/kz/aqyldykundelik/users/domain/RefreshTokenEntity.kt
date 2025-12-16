package kz.aqyldykundelik.users.domain
import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity @Table(name = "refresh_token")
class RefreshTokenEntity(
    @Id @GeneratedValue(strategy = GenerationType.UUID) var id: UUID? = null,
    @Column(name = "user_id", nullable = false) var userId: UUID? = null,
    @Column(name = "token_hash", nullable = false, unique = true) var tokenHash: String? = null,
    @Column(name = "expires_at", nullable = false) var expiresAt: Instant? = null,
    @Column(nullable = false) var revoked: Boolean = false,
)
