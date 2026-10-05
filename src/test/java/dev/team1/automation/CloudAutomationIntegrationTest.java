package dev.team1.automation;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import dev.team1.roles.RoleEntity;
import dev.team1.roles.RoleRepository;
import dev.team1.security.JwtService;
import dev.team1.users.UserEntity;
import dev.team1.users.UserRepository;
import jakarta.servlet.http.Cookie;

// Test de integración del estado de la subida automática (tarjeta del panel de admin).
// Sin credenciales de Supabase, aunque tu .env las tenga: el estado debe ser ERROR.
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = { "cloud.supabase.url=", "cloud.supabase.bucket=", "cloud.supabase.api-key=" })
@AutoConfigureMockMvc
@Transactional
class CloudAutomationIntegrationTest {

    private static final String ADMIN_ROLE = "ROLE_ADMIN";
    private static final String CUSTOMER_ROLE = "ROLE_CUSTOMER";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;

    @Value("/${api-endpoint}")
    private String apiEndpoint;

    private UserEntity admin;
    private UserEntity customer;

    @BeforeEach
    void setUp() {
        admin = userRepository.save(newUser("cron-admin@test.com", findOrCreateRole(ADMIN_ROLE)));
        customer = userRepository.save(newUser("cron-customer@test.com", findOrCreateRole(CUSTOMER_ROLE)));
    }

    @Test
    void cronStatus_asAdmin_returnsTheStatusForTheAdminCard() throws Exception {
        mockMvc.perform(get(apiEndpoint + "/sistema/cron-status")
                .cookie(accessTokenFor(admin, ADMIN_ROLE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ERROR"))
            .andExpect(jsonPath("$.lastError").value(containsString("no está configurado")));
    }

    @Test
    void cronRun_asAdmin_runsTheUploadNowAndReturnsTheStatus() throws Exception {
        mockMvc.perform(post(apiEndpoint + "/sistema/cron-run")
                .with(csrf())
                .cookie(accessTokenFor(admin, ADMIN_ROLE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ERROR"));
    }

    @Test
    void cronStatus_asCustomer_returns403() throws Exception {
        mockMvc.perform(get(apiEndpoint + "/sistema/cron-status")
                .cookie(accessTokenFor(customer, CUSTOMER_ROLE)))
            .andExpect(status().isForbidden());
    }

    @Test
    void cronStatus_withoutSession_returns401() throws Exception {
        mockMvc.perform(get(apiEndpoint + "/sistema/cron-status"))
            .andExpect(status().isUnauthorized());
    }

    private Cookie accessTokenFor(UserEntity user, String role) {
        return new Cookie("access_token", jwtService.generateAuthToken(user.getEmail(), role).token());
    }

    private RoleEntity findOrCreateRole(String name) {
        return roleRepository.findByName(name)
            .orElseGet(() -> {
                RoleEntity role = new RoleEntity();
                role.setName(name);
                return roleRepository.save(role);
            });
    }

    private UserEntity newUser(String email, RoleEntity role) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPassword("irrelevant");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setAddress("Test address");
        user.setPostalCode("00000");
        user.setCity("Test city");
        user.setRoles(Set.of(role));
        return user;
    }
}