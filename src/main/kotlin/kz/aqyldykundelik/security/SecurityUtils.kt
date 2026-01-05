package kz.aqyldykundelik.security

import kz.aqyldykundelik.users.repo.UserRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.util.*

@Component
class SecurityUtils(
    private val userRepository: UserRepository
) {

    /**
     * Returns the current authenticated user's ID from the security context.
     * Returns null if authentication is not present or user ID cannot be resolved.
     */
    fun currentUserIdOrNull(): UUID? {
        return try {
            val auth = SecurityContextHolder.getContext().authentication
            val email = auth?.name ?: return null
            userRepository.findByEmail(email)?.id
        } catch (e: Exception) {
            null
        }
    }
}

