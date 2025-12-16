package kz.aqyldykundelik.dev

import kz.aqyldykundelik.users.domain.UserEntity
import kz.aqyldykundelik.users.repo.UserRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*

@Configuration
@Profile("dev")
class DevSeed {
    @Bean
    fun seedAdmin(users: UserRepository, passwordEncoder: PasswordEncoder) = CommandLineRunner {
        if (users.findByEmail("admin@local") == null) {
            users.save(
                UserEntity(
                    email = "admin@local",
                    fullName = "Admin User",
                    role = "ADMIN",
                    status = "ACTIVE",
                    passwordHash = passwordEncoder.encode("admin123")
                )
            )
            println("✓ Created admin@local with password admin123")
        }
    }
}
