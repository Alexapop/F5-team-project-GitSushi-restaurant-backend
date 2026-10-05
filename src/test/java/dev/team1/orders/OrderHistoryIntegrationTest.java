package dev.team1.orders;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import dev.team1.enums.ProductCategory;
import dev.team1.products.ProductEntity;
import dev.team1.products.ProductRepository;
import dev.team1.roles.RoleEntity;
import dev.team1.roles.RoleRepository;
import dev.team1.security.JwtService;
import dev.team1.tables.TableEntity;
import dev.team1.tables.TableRepository;
import dev.team1.users.UserEntity;
import dev.team1.users.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;

// Flujo completo sin mocks: el cliente hace pedidos -> ve su historial ordenado -> "repetir" devuelve los productos.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class OrderHistoryIntegrationTest {

    private static final String DEVICE_ID = "tablet-history-test";
    private static final LocalDateTime NEWER_DATE = LocalDateTime.of(2026, 9, 20, 21, 10);
    private static final LocalDateTime OLDER_DATE = LocalDateTime.of(2026, 8, 22, 14, 5);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private TableRepository tableRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManager entityManager;

    @Value("/${api-endpoint}")
    private String apiEndpoint;

    private UserEntity customer;
    private Cookie customerCookie;
    private Cookie otherCustomerCookie;
    private Long sushiId;
    private Long ramenId;

    @BeforeEach
    void setUp() {
        customer = userRepository.save(newUser("history-customer@test.com", "ROLE_CUSTOMER"));
        UserEntity otherCustomer = userRepository.save(newUser("history-other@test.com", "ROLE_CUSTOMER"));
        customerCookie = authCookie(customer);
        otherCustomerCookie = authCookie(otherCustomer);

        sushiId = productRepository.save(newProduct("History Test Sushi", "6.50")).getId();
        ramenId = productRepository.save(newProduct("History Test Ramen", "5.50")).getId();

        TableEntity table = new TableEntity();
        table.setTableNumber(98);
        table.setDeviceIdentifier(DEVICE_ID);
        tableRepository.save(table);
    }

    @Test
    void customerSeesOwnOrdersFromNewestToOldest() throws Exception {
        // El primer pedido creado es el más reciente: así comprobamos que se ordena por fecha y no por id.
        long newerOrderId = createOrder(customerCookie, sushiId, 2);
        long olderOrderId = createOrder(customerCookie, ramenId, 1);
        setCreatedAt(newerOrderId, NEWER_DATE);
        setCreatedAt(olderOrderId, OLDER_DATE);

        mockMvc.perform(get(apiEndpoint + "/users/" + customer.getId() + "/orders").cookie(customerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(newerOrderId))
                .andExpect(jsonPath("$.content[0].date").value("2026-09-20T21:10:00"))
                .andExpect(jsonPath("$.content[0].items[0].productId").value(sushiId))
                .andExpect(jsonPath("$.content[0].items[0].name").value("History Test Sushi"))
                .andExpect(jsonPath("$.content[0].items[0].quantity").value(2))
                .andExpect(jsonPath("$.content[0].items[0].price").value(6.5))
                .andExpect(jsonPath("$.content[0].items[0].available").value(true))
                .andExpect(jsonPath("$.content[0].total").isNumber())
                .andExpect(jsonPath("$.content[1].id").value(olderOrderId))
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.page.number").value(0));
    }

    @Test
    void historyIsPaginated() throws Exception {
        long newerOrderId = createOrder(customerCookie, sushiId, 1);
        long olderOrderId = createOrder(customerCookie, ramenId, 1);
        setCreatedAt(newerOrderId, NEWER_DATE);
        setCreatedAt(olderOrderId, OLDER_DATE);

        mockMvc.perform(get(apiEndpoint + "/users/" + customer.getId() + "/orders")
                        .param("page", "1").param("size", "1")
                        .cookie(customerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(olderOrderId))
                .andExpect(jsonPath("$.page.number").value(1))
                .andExpect(jsonPath("$.page.totalPages").value(2));
    }

    @Test
    void historyOnlyContainsOrdersOfThatCustomer() throws Exception {
        long ownOrderId = createOrder(customerCookie, sushiId, 1);
        createOrder(otherCustomerCookie, ramenId, 3);

        mockMvc.perform(get(apiEndpoint + "/users/" + customer.getId() + "/orders").cookie(customerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(ownOrderId));
    }

    @Test
    void repeatReturnsAvailableProductsWithCurrentPriceAndOriginalQuantity() throws Exception {
        long orderId = createOrder(customerCookie, sushiId, 2);

        // Después del pedido el sushi sube de precio.
        ProductEntity sushi = productRepository.findById(sushiId).orElseThrow();
        sushi.setPrice(new BigDecimal("7.25"));
        productRepository.save(sushi);

        mockMvc.perform(get(apiEndpoint + "/orders/" + orderId + "/repeat").cookie(customerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productId").value(sushiId))
                .andExpect(jsonPath("$[0].name").value("History Test Sushi"))
                .andExpect(jsonPath("$[0].price").value(7.25))
                .andExpect(jsonPath("$[0].quantity").value(2));

        // El historial sigue mostrando el precio que se pagó.
        mockMvc.perform(get(apiEndpoint + "/users/" + customer.getId() + "/orders").cookie(customerCookie))
                .andExpect(jsonPath("$.content[0].items[0].price").value(6.5));
    }

    @Test
    void repeatSkipsProductsThatAreNoLongerAvailable() throws Exception {
        long orderId = createOrder(customerCookie, ramenId, 1);

        ProductEntity ramen = productRepository.findById(ramenId).orElseThrow();
        ramen.setAvailable(false);
        productRepository.save(ramen);

        mockMvc.perform(get(apiEndpoint + "/orders/" + orderId + "/repeat").cookie(customerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get(apiEndpoint + "/users/" + customer.getId() + "/orders").cookie(customerCookie))
                .andExpect(jsonPath("$.content[0].items[0].available").value(false));
    }

    @Test
    void anotherCustomerCannotSeeHistoryOrRepeatOrder() throws Exception {
        long orderId = createOrder(customerCookie, sushiId, 1);

        mockMvc.perform(get(apiEndpoint + "/users/" + customer.getId() + "/orders").cookie(otherCustomerCookie))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(apiEndpoint + "/orders/" + orderId + "/repeat").cookie(otherCustomerCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void historyAndRepeatRequireLogin() throws Exception {
        long orderId = createOrder(customerCookie, sushiId, 1);

        mockMvc.perform(get(apiEndpoint + "/users/" + customer.getId() + "/orders"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(apiEndpoint + "/orders/" + orderId + "/repeat"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void repeatReturnsNotFoundForMissingOrder() throws Exception {
        mockMvc.perform(get(apiEndpoint + "/orders/999999/repeat").cookie(customerCookie))
                .andExpect(status().isNotFound());
    }

    private long createOrder(Cookie cookie, Long productId, int quantity) throws Exception {
        MvcResult result = mockMvc.perform(post(apiEndpoint + "/orders")
                        .with(csrf())
                        .cookie(cookie)
                        .header("Device-Identifier", DEVICE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [{"productId": %d, "quantity": %d}],
                                  "channel": "ONSITE",
                                  "paymentMethod": "CASH_ONSITE"
                                }
                                """.formatted(productId, quantity)))
                .andExpect(status().isCreated())
                .andReturn();

        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    // createdAt lo pone @PrePersist y no es actualizable desde JPA, así que fijamos la fecha con SQL.
    private void setCreatedAt(long orderId, LocalDateTime createdAt) {
        entityManager.flush();
        jdbcTemplate.update("UPDATE orders SET created_at = ? WHERE id_order = ?",
                Timestamp.valueOf(createdAt), orderId);
        entityManager.clear();
    }

    private Cookie authCookie(UserEntity user) {
        return new Cookie("access_token", jwtService.generateAuthToken(user.getEmail(), "ROLE_CUSTOMER").token());
    }

    private UserEntity newUser(String email, String roleName) {
        RoleEntity role = roleRepository.findByName(roleName)
                .orElseGet(() -> {
                    RoleEntity newRole = new RoleEntity();
                    newRole.setName(roleName);
                    return roleRepository.save(newRole);
                });

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPassword("test-password");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setAddress("Test address");
        user.setPostalCode("00000");
        user.setCity("Test city");
        user.setRoles(Set.of(role));
        return user;
    }

    private ProductEntity newProduct(String name, String price) {
        return new ProductEntity(null, name, ProductCategory.ENTRANTES, "Integration test product",
                "test.png", new BigDecimal(price), BigDecimal.ZERO, true, false, new ArrayList<>());
    }
}
