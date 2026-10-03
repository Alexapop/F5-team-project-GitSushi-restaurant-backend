package dev.team1.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfiguration {

    private final JwtFilter jwtFilter;

    @Value("/${api-endpoint}")
    private String pre;

    @Value("${frontend-domain}")
    private String frontendDomain;

    @Value("${cookie-same-site}")
    private String sameSite;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Configuration without auth and security
        //
        // http
        // .csrf(csrf -> csrf.disable())
        // .authorizeHttpRequests(auth -> auth
        // .anyRequest().permitAll()
        // );
        // return http.build();

        CookieCsrfTokenRepository csrfRepo = new CookieCsrfTokenRepository();
        csrfRepo.setCookieCustomizer(cookie -> cookie
                .httpOnly(false)
                .secure(true)
                .sameSite(sameSite));

        return http
            .cors(cors -> cors
                .configurationSource(corsConfigurationSource()))

            .httpBasic(AbstractHttpConfigurer::disable)
            
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfRepo)
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                .ignoringRequestMatchers(pre + "/auth/login")
                // Con JWT cada petición "autentica" de nuevo: sin esto Spring borra
                // la cookie XSRF-TOKEN en cada petición con sesión y los PATCH/POST dan 403.
                .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy()))
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
            
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, pre + "/users").permitAll()
                .requestMatchers(HttpMethod.PUT, pre + "/users/*").authenticated()
                //  el historial lo ve cualquier usuario autenticado; OrderService comprueba que sea el suyo
                .requestMatchers(HttpMethod.GET, pre + "/users/*/orders").authenticated()
                .requestMatchers(pre + "/users").hasRole("ADMIN")
                .requestMatchers(pre + "/users/**").hasRole("ADMIN")
                .requestMatchers(pre + "/products/administration").hasRole("ADMIN")
                .requestMatchers(pre + "/kitchen").hasAnyAuthority("ROLE_ADMIN", "ROLE_COOK")
                .requestMatchers(pre + "/kitchen/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_COOK")
                .requestMatchers(pre + "/delivery").hasAnyAuthority("ROLE_ADMIN", "ROLE_DELIVERYMAN")
                .requestMatchers(pre + "/delivery/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_DELIVERYMAN")
                .requestMatchers(HttpMethod.POST, pre + "/orders").permitAll()
                // GS-341: el pago es público para que un invitado pueda pagar sin autenticarse
                .requestMatchers(HttpMethod.POST, pre + "/payments/checkout").permitAll()
                .requestMatchers(HttpMethod.POST, pre + "/payments/confirm").permitAll()
                .requestMatchers(HttpMethod.PATCH, pre + "/orders/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_COOK", "ROLE_DELIVERYMAN")
                .requestMatchers(HttpMethod.GET, pre + "/invoices").hasAnyAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.GET, pre + "/invoices/paid/**").hasAnyAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.GET, pre + "/invoices/paid").hasAnyAuthority("ROLE_ADMIN")
                                .requestMatchers(HttpMethod.GET, pre + "/facturation").hasAnyAuthority("ROLE_ADMIN")
                .requestMatchers(pre + "/auth/login").permitAll()
                .requestMatchers(pre + "/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.GET, pre + "/products").permitAll()
                // GS-562: el acceso al ticket lo comprueba OrderService (dueño, admin o token)
                .requestMatchers(HttpMethod.GET, pre + "/tickets/*").permitAll()

                .anyRequest().authenticated())
            
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
        
            .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(frontendDomain)); // frontend domain
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        config.setAllowedHeaders(List.of("X-XSRF-TOKEN", "*"));
        config.setAllowCredentials(true); 

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean 
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
