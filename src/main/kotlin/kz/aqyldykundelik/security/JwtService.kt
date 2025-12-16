package kz.aqyldykundelik.security

import kz.aqyldykundelik.users.domain.RefreshTokenEntity
import kz.aqyldykundelik.users.domain.UserEntity
import kz.aqyldykundelik.users.repo.RefreshTokenRepository
import kz.aqyldykundelik.users.repo.UserRepository
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class JwtService(
    private val encoder: JwtEncoder,
    private val refreshRepo: RefreshTokenRepository,
    private val users: UserRepository
) {
    data class Tokens(val access: String, val refresh: String, val expiresAt: Instant)

    fun issueTokens(user: UserEntity): Tokens {
        val now = Instant.now()
        val exp = now.plus(15, ChronoUnit.MINUTES)
        val claims = JwtClaimsSet.builder()
            .subject(user.email).issuedAt(now).expiresAt(exp)
            .claim("uid", user.id.toString())
            .claim("roles", listOf(user.role))
            .build()
        val access = encoder.encode(JwtEncoderParameters.from(claims)).tokenValue

        val refreshPlain = UUID.randomUUID().toString().replace("-","") +
                           UUID.randomUUID().toString().replace("-","")
        val refreshHash = sha256(refreshPlain)
        val refreshExp = now.plus(30, ChronoUnit.DAYS)
        refreshRepo.save(RefreshTokenEntity(userId = user.id!!, tokenHash = refreshHash, expiresAt = refreshExp))
        return Tokens(access, refreshPlain, exp)
    }

    fun rotate(refreshPlain: String): Tokens {
        val hash = sha256(refreshPlain)
        val token = refreshRepo.findActiveByHash(hash) ?: throw IllegalArgumentException("invalid refresh")
        if (token.expiresAt!!.isBefore(Instant.now())) throw IllegalStateException("expired refresh")
        token.revoked = true
        refreshRepo.save(token)
        val user = users.findById(token.userId!!).orElseThrow()
        return issueTokens(user)
    }

    fun revoke(refreshPlain: String) {
        val hash = sha256(refreshPlain)
        val token = refreshRepo.findActiveByHash(hash) ?: return
        token.revoked = true
        refreshRepo.save(token)
    }

    private fun sha256(s: String) =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
}
