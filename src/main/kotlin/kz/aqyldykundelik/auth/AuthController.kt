package kz.aqyldykundelik.auth

import kz.aqyldykundelik.security.JwtService
import kz.aqyldykundelik.users.repo.UserRepository
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.web.bind.annotation.*

data class LoginRequest(val email: String, val password: String)
data class TokenResponse(val accessToken: String, val refreshToken: String, val expiresAt: Long)
data class RefreshRequest(val refreshToken: String)

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authManager: AuthenticationManager,
    private val users: UserRepository,
    private val jwt: JwtService
) {
    @PostMapping("/login")
    fun login(@RequestBody body: LoginRequest): TokenResponse {
        authManager.authenticate(UsernamePasswordAuthenticationToken(body.email, body.password))
        val user = users.findByEmail(body.email)!!
        val t = jwt.issueTokens(user)
        return TokenResponse(t.access, t.refresh, t.expiresAt.epochSecond)
    }

    @PostMapping("/refresh")
    fun refresh(@RequestBody r: RefreshRequest): TokenResponse {
        val t = jwt.rotate(r.refreshToken)
        return TokenResponse(t.access, t.refresh, t.expiresAt.epochSecond)
    }

    @PostMapping("/logout")
    fun logout(@RequestBody r: RefreshRequest) {
        jwt.revoke(r.refreshToken)
    }
}
