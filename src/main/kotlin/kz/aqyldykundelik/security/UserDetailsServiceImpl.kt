package kz.aqyldykundelik.security
import kz.aqyldykundelik.users.repo.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(private val users: UserRepository) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        val u = users.findByEmail(username) ?: error("User not found")
        val authorities = listOf(SimpleGrantedAuthority("ROLE_${u.role}"))
        return User(u.email, u.passwordHash, authorities)
    }
}
