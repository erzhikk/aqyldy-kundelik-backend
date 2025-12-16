package kz.aqyldykundelik.security
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.*
import org.springframework.security.web.SecurityFilterChain
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import java.security.*
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.*

@Configuration
@EnableMethodSecurity
class SecurityConfig(
    private val userDetailsService: org.springframework.security.core.userdetails.UserDetailsService
) {
    data class RsaKeys(val publicKey: RSAPublicKey, val privateKey: RSAPrivateKey)
    @Bean fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
    @Bean fun authenticationManager(): AuthenticationManager {
        val provider = DaoAuthenticationProvider().apply {
            setUserDetailsService(userDetailsService); setPasswordEncoder(passwordEncoder())
        }
        return ProviderManager(provider)
    }
    @Bean fun rsaKeys(): RsaKeys = generateRsa()
    @Bean fun jwtEncoder(keys: RsaKeys): JwtEncoder {
        val jwk = RSAKey.Builder(keys.publicKey).privateKey(keys.privateKey).keyID(UUID.randomUUID().toString()).build()
        val jwkSource = JWKSource<SecurityContext> { selector, _ -> selector.select(JWKSet(jwk)) }
        return NimbusJwtEncoder(jwkSource)
    }
    @Bean fun jwtDecoder(keys: RsaKeys): JwtDecoder = NimbusJwtDecoder.withPublicKey(keys.publicKey).build()
    @Bean
    fun securityFilterChain(http: HttpSecurity, decoder: JwtDecoder): SecurityFilterChain {
        val conv = org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter()
        conv.setJwtGrantedAuthoritiesConverter { jwt ->
            val roles = (jwt.claims["roles"] as? Collection<*>)?.map { "ROLE_${it.toString()}" } ?: emptyList()
            roles.map { SimpleGrantedAuthority(it) }
        }
        http.cors { }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers("/actuator/health","/api/auth/**").permitAll()
                 .anyRequest().authenticated()
            }
            .oauth2ResourceServer { rs -> rs.jwt { j -> j.jwtAuthenticationConverter(conv).decoder(decoder) } }
        return http.build()
    }
    private fun generateRsa(): RsaKeys {
        val kp = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        return RsaKeys(kp.public as RSAPublicKey, kp.private as RSAPrivateKey)
    }
}
