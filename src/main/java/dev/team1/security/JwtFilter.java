package dev.team1.security;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import dev.team1.auth.CustomUserDetails;
import dev.team1.auth.CustomUserDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component 
public class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final CustomUserDetailsService customUserService;
    private final HandlerExceptionResolver resolver;

    public JwtFilter(JwtService jwtService,
        CustomUserDetailsService customUserService,
        @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) 
    {
        this.jwtService = jwtService;
        this.customUserService = customUserService;
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, 
        HttpServletResponse response, 
        FilterChain filterChain
    )
        throws ServletException, IOException 
    {
        String token = getTokenFromRequest(request);

        System.out.print("TOKEN: " + token);

        try {
            jwtService.validateJwtToken(token);
            setCustomUserDetailsToSecurityContextHolder(token);
        } catch (ExpiredJwtException exc) {
            resolver.resolveException(request, response, null, exc);
            return;
        } catch (JwtException exc) {
            resolver.resolveException(request, response, null, exc);
            return;
        } catch (IllegalArgumentException exc) {}
            
        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        // From Danyil: I know about this strategy...

        // String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        // if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
        //     return bearerToken.substring(7);
        // }
        // return null;

        // But we use other one bacause we want JWT-token to be in httpOnly-cookie to awoid XSS attacks by JavaScript:

        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
            .filter(cookie -> "access_token".equals(cookie.getName()))
            .findFirst()
            .map(Cookie::getValue)
            .orElse(null);
    }

    private void setCustomUserDetailsToSecurityContextHolder(String token) {
        String email = jwtService.getEmailFromToken(token);

        System.out.println(email);

        CustomUserDetails customUserDetails = customUserService.loadUserByUsername(email);

        System.out.println(customUserDetails);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            customUserDetails,
            null,
            customUserDetails.getAuthorities()
        );

        System.out.println("\nauthentication: " + authentication + "\n");

        SecurityContextHolder.getContext().setAuthentication(authentication); 
    }
    
}
