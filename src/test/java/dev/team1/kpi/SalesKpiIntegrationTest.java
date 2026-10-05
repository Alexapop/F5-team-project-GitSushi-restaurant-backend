package dev.team1.kpi;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

// TDD: test de integración del endpoint GET /kpi/sales con Spring completo y la base H2.
// Se escribe antes que el controlador, así que al principio falla (rojo).
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class SalesKpiIntegrationTest {

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
        admin = userRepository.save(newUser("kpi-admin@test.com", findOrCreateRole(ADMIN_ROLE)));
        customer = userRepository.save(newUser("kpi-customer@test.com", findOrCreateRole(CUSTOMER_ROLE)));
    }

    @Test
    void salesKpi_asAdmin_returnsTheFormatTheFrontExpects() throws Exception {
        mockMvc.perform(get(apiEndpoint + "/kpi/sales")
                .cookie(accessTokenFor(admin, ADMIN_ROLE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.today.revenue").isNumber())
            .andExpect(jsonPath("$.today.previousRevenue").isNumber())
            .andExpect(jsonPath("$.month.revenue").isNumber())
            .andExpect(jsonPath("$.quarter.revenue").isNumber())
            .andExpect(jsonPath("$.year.revenue").isNumber())
            .andExpect(jsonPath("$.channels.inStore.percentage").isNumber())
            .andExpect(jsonPath("$.channels.delivery.revenue").isNumber())
            .andExpect(jsonPath("$.weekly", hasSize(7)))
            .andExpect(jsonPath("$.weekly[0].day").value("MONDAY"));
    }

    @Test
    void salesKpi_asCustomer_returns403() throws Exception {
        mockMvc.perform(get(apiEndpoint + "/kpi/sales")
                .cookie(accessTokenFor(customer, CUSTOMER_ROLE)))
            .andExpect(status().isForbidden());
    }

    @Test
    void salesKpi_withoutSession_returns401() throws Exception {
        mockMvc.perform(get(apiEndpoint + "/kpi/sales"))
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