package dev.team1.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import dev.team1.roles.RoleEntity;
import dev.team1.roles.RoleRepository;
import dev.team1.users.UserEntity;
import dev.team1.users.UserRepository;
import jakarta.servlet.http.Cookie;

// Contexto propio (TestPropertySource) para no compartir el filtro CSRF con
// los tests que usan .with(csrf()), que sustituye el repositorio de tokens.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = "csrf-cookie-test=true")
@Transactional
class CsrfCookieIntegrationTest {

    private static final String EMAIL = "csrf-admin@test.com";
    private static final String PASSWORD = "admin-password";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("/${api-endpoint}")
    private String apiEndpoint;

    @BeforeEach
    void setUp() {
        RoleEntity adminRole = roleRepository.findByName("ROLE_ADMIN")
            .orElseGet(() -> {
                RoleEntity role = new RoleEntity();
                role.setName("ROLE_ADMIN");
                return roleRepository.save(role);
            });

        UserEntity admin = new UserEntity();
        admin.setEmail(EMAIL);
        admin.setPassword(passwordEncoder.encode(PASSWORD));
        admin.setFirstName("Csrf");
        admin.setLastName("Admin");
        admin.setAddress("Test address");
        admin.setPostalCode("00000");
        admin.setCity("Test city");
        admin.setRoles(Set.of(adminRole));
        userRepository.save(admin);
    }

    private MvcResult login() throws Exception {
        String body = """
            {"email":"%s","password":"%s"}
            """.formatted(EMAIL, PASSWORD);

        return mockMvc.perform(post(apiEndpoint + "/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("XSRF-TOKEN"))
            .andReturn();
    }

    @Test
    void authenticatedRequest_doesNotClearCsrfCookie() throws Exception {
        MvcResult login = login();
        Cookie accessToken = login.getResponse().getCookie("access_token");
        Cookie csrfToken = login.getResponse().getCookie("XSRF-TOKEN");

        MvcResult list = mockMvc.perform(get(apiEndpoint + "/users")
                .cookie(accessToken, csrfToken))
            .andExpect(status().isOk())
            .andReturn();

        Cookie csrfAfter = list.getResponse().getCookie("XSRF-TOKEN");
        assertThat(csrfAfter == null || !csrfAfter.getValue().isEmpty()).isTrue();
    }

    @Test
    void patchWithCsrfHeader_isNotRejectedByCsrf() throws Exception {
        MvcResult login = login();
        Cookie accessToken = login.getResponse().getCookie("access_token");
        Cookie csrfToken = login.getResponse().getCookie("XSRF-TOKEN");

        // Usuario que no existe: si pasa el CSRF, el backend responde 404 (no 403).
        mockMvc.perform(patch(apiEndpoint + "/users/" + UUID.randomUUID())
                .cookie(accessToken, csrfToken)
                .header("X-XSRF-TOKEN", csrfToken.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\": false}"))
            .andExpect(status().isNotFound());
    }
}