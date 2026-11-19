package com.koalagroup.koala

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.koalagroup.koala.infrastructure.security.CorsProperties
import com.koalagroup.koala.infrastructure.security.JwtCookie
import com.koalagroup.koala.infrastructure.security.JwtProperties
import com.koalagroup.koala.infrastructure.security.JwtTokenService
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.util.Base64
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties::class, CorsProperties::class)
class SecurityConfig {

    @Bean
    fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()

    @Bean
    fun jwtSecretKey(properties: JwtProperties): SecretKey {
        val keyBytes = try {
            Base64.getDecoder().decode(properties.secret)
        } catch (ex: IllegalArgumentException) {
            throw IllegalStateException("app.security.jwt.secret must be a Base64-encoded secret", ex)
        }
        require(keyBytes.size >= 32) { "app.security.jwt.secret must contain at least 32 bytes" }
        require(!properties.expiration.isZero && !properties.expiration.isNegative) {
            "app.security.jwt.expiration must be a positive duration"
        }
        require(properties.cookieSameSite in setOf("Strict", "Lax", "None")) {
            "app.security.jwt.cookie-same-site must be Strict, Lax, or None"
        }
        require(properties.cookieSameSite != "None" || properties.cookieSecure) {
            "app.security.jwt.cookie-secure must be true when cookie-same-site is None"
        }
        return SecretKeySpec(keyBytes, "HmacSHA256")
    }

    @Bean
    fun jwtEncoder(secretKey: SecretKey): JwtEncoder = NimbusJwtEncoder(ImmutableSecret(secretKey))

    @Bean
    fun jwtDecoder(secretKey: SecretKey): JwtDecoder =
        NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build().also {
            it.setJwtValidator(JwtValidators.createDefaultWithIssuer(JwtTokenService.ISSUER))
        }

    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter =
        JwtAuthenticationConverter().apply {
            setJwtGrantedAuthoritiesConverter { jwt: Jwt ->
                jwt.getClaimAsString("role")
                    ?.takeIf { it.isNotBlank() }
                    ?.let { listOf(SimpleGrantedAuthority("ROLE_$it")) }
                    ?: emptyList()
            }
        }

    @Bean
    fun bearerTokenResolver(): BearerTokenResolver {
        return BearerTokenResolver { request ->
            request.cookies?.firstOrNull { it.name == JwtCookie.NAME }?.value
        }
    }

    @Bean
    fun corsConfigurationSource(properties: CorsProperties): CorsConfigurationSource {
        require(properties.allowedOriginPatterns.isNotEmpty() && properties.allowedOriginPatterns.none(String::isBlank)) {
            "app.security.cors.allowed-origin-patterns must contain at least one non-blank origin pattern"
        }

        val configuration = CorsConfiguration().apply {
            allowedOriginPatterns = properties.allowedOriginPatterns
            allowedMethods = listOf("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
            allowedHeaders = listOf("Content-Type", "X-XSRF-TOKEN")
            allowCredentials = true
            maxAge = 3600
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }

    @Bean
    fun csrfTokenRepository(jwtProperties: JwtProperties): CookieCsrfTokenRepository =
        CookieCsrfTokenRepository.withHttpOnlyFalse().apply {
            setCookiePath("/")
            setCookieCustomizer {
                it.secure(jwtProperties.cookieSecure).sameSite(jwtProperties.cookieSameSite)
            }
        }

    @Bean
    fun filterChain(
        http: HttpSecurity,
        jwtAuthenticationConverter: JwtAuthenticationConverter,
        bearerTokenResolver: BearerTokenResolver,
        csrfTokenRepository: CookieCsrfTokenRepository
    ): SecurityFilterChain {
        http.csrf {
            it.csrfTokenRepository(csrfTokenRepository)
                .csrfTokenRequestHandler(CsrfTokenRequestAttributeHandler())
        }
        http.cors { }
        http.authorizeHttpRequests {
            it.requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/roles").permitAll()
                .anyRequest().authenticated()
        }
        http.oauth2ResourceServer {
            it.bearerTokenResolver(bearerTokenResolver)
                .jwt { jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter) }
        }
        http.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        http.requestCache { it.disable() }
        http.formLogin { it.disable() }
        http.httpBasic { it.disable() }
        http.logout { it.disable() }
        return http.build()
    }
}
