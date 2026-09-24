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


    @Test
    void logout_withValidToken_returns204() throws Exception {
        String token = jwtService.generateAuthToken(user.getEmail(), "ROLE_USER").token();

        mockMvc.perform(get(apiEndpoint + "/auth/logout")
                .cookie(new Cookie("access_token", token)))
            .andExpect(status().isNoContent());
    }

    @Test
    void logout_withoutToken_returnsForbidden() throws Exception {
        // /auth/logout не в publicURIList и не имеет permitAll в SecurityConfiguration
        mockMvc.perform(get(apiEndpoint + "/auth/logout"))
            .andExpect(status().isForbidden());
    }

    @Test
    void logout_clearsCookiesWithEmptyValue() throws Exception {
        String token = jwtService.generateAuthToken(user.getEmail(), "ROLE_USER").token();

        mockMvc.perform(get(apiEndpoint + "/auth/logout")
                .cookie(new Cookie("access_token", token)))
            .andExpect(cookie().value("access_token", ""))
            .andExpect(cookie().value("refresh_token", ""));
    }


    @Test
    void refresh_withValidRefreshToken_returns204AndSetsNewCookies() throws Exception {
        String refreshToken = jwtService.generateAuthToken(user.getEmail(), "ROLE_USER").refreshToken();

        mockMvc.perform(get(apiEndpoint + "/auth/refresh")
                .cookie(new Cookie("refresh_token", refreshToken)))
            .andExpect(status().isNoContent())
            .andExpect(cookie().exists("access_token"))
            .andExpect(cookie().exists("refresh_token"));
    }

    @Test
    void refresh_withoutRefreshToken_returnsError() throws Exception {
        mockMvc.perform(get(apiEndpoint + "/auth/refresh"))
            .andExpect(status().isForbidden());
    }

    @Test
    void me_withValidToken_returns200WithUserData() throws Exception {
        String token = jwtService.generateAuthToken(user.getEmail(), "ROLE_USER").token();

        mockMvc.perform(get(apiEndpoint + "/auth/me")
                .cookie(new Cookie("access_token", token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(user.getEmail()));
    }

    @Test
    void me_withoutToken_returnsForbidden() throws Exception {
        mockMvc.perform(get(apiEndpoint + "/auth/me"))
            .andExpect(status().isForbidden());
    }

}