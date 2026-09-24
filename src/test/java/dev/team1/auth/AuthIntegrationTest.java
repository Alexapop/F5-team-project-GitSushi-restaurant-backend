package dev.team1.auth;

import dev.team1.roles.RoleEntity;
import dev.team1.roles.RoleRepository;
import dev.team1.users.UserEntity;
import dev.team1.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dev.team1.security.JwtService;
import jakarta.servlet.http.Cookie;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private JwtService jwtService;


    @Value("/${api-endpoint}")
    private String apiEndpoint;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        RoleEntity userRole = roleRepository.findByName("ROLE_USER")
            .orElseGet(() -> {
                RoleEntity role = new RoleEntity();
                role.setName("ROLE_USER");
                return roleRepository.save(role);
            });

        user = new UserEntity();
        user.setEmail("login-test@test.com");
        user.setPassword("correct-password"); // TODO: change with Hash when we will implement BCryptPasswordEncoder
        user.setFirstName("Test");
        user.setLastName("User");
        user.setAddress("Test address");
        user.setPostalCode("00000");
        user.setCity("Test city");
        user.setRoles(Set.of(userRole));
        userRepository.save(user);
    }

    @Test
    void login_withCorrectCredentials_returns200AndSetsCookies() throws Exception {
        String body = """
            {"email":"login-test@test.com","password":"correct-password"}
            """;

        mockMvc.perform(post(apiEndpoint + "/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("access_token"))
            .andExpect(cookie().exists("refresh_token"))
            .andExpect(jsonPath("$.email").value("login-test@test.com"));
    }

    @Test
    void login_withWrongPassword_returnsUnauthorized() throws Exception {
        String body = """
            {"email":"login-test@test.com","password":"wrong-password"}
            """;

        mockMvc.perform(post(apiEndpoint + "/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withNonexistentEmail_returnsUnauthorized() throws Exception {
        String body = """
            {"email":"nobody@test.com","password":"whatever"}
            """;

        mockMvc.perform(post(apiEndpoint + "/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnauthorized());
    }

    
}