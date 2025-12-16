package kz.aqyldykundelik.users.repo
import kz.aqyldykundelik.users.domain.RefreshTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface RefreshTokenRepository : JpaRepository<RefreshTokenEntity, UUID> {
    @Query("select t from RefreshTokenEntity t where t.tokenHash = :hash and t.revoked = false")
    fun findActiveByHash(hash: String): RefreshTokenEntity?
}
