package dev.team1.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration 
@EnableWebSecurity 
@RequiredArgsConstructor 
@EnableMethodSecurity 
public class SecurityConfiguration {

    private final JwtFilter jwtFilter;

    @Bean 
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Configuration without auth and security
        //
        // http
        //     .csrf(csrf -> csrf.disable())
        //     .authorizeHttpRequests(auth -> auth
        //         .anyRequest().permitAll()
        //     );
        // return http.build();


        http
            .httpBasic(AbstractHttpConfigurer::disable)
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers( "/auth/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/users").permitAll()
                .requestMatchers("/users").hasRole("ADMIN")
                .requestMatchers("/products/administration").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH,"/orders/**").hasAnyAuthority("ROLE_COOK", "ROLE_DELIVERYMAN")
                .anyRequest().authenticated())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();

    }

}
