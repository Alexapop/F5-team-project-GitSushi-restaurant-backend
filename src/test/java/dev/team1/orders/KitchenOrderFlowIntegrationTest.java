package dev.team1.orders;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
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
import jakarta.servlet.http.Cookie;

// Flujo completo sin mocks: el cliente crea un pedido -> aparece en cocina -> el cocinero lo marca listo.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class KitchenOrderFlowIntegrationTest {

    private static final String DEVICE_ID = "tablet-integration-test";

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

    @Value("/${api-endpoint}")
    private String apiEndpoint;

    private Cookie customerCookie;
    private Cookie cookCookie;
    private ProductEntity sushi;

    @BeforeEach
    void setUp() {
        UserEntity customer = userRepository.save(newUser("flow-customer@test.com", "ROLE_CUSTOMER"));
        UserEntity cook = userRepository.save(newUser("flow-cook@test.com", "ROLE_COOK"));
        customerCookie = authCookie(customer, "ROLE_CUSTOMER");
        cookCookie = authCookie(cook, "ROLE_COOK");

        sushi = productRepository.save(newProduct("Flow Test Sushi", "10.00"));

        TableEntity table = new TableEntity();
        table.setTableNumber(99);
        table.setDeviceIdentifier(DEVICE_ID);
        tableRepository.save(table);
    }

    @Test
    void onsiteOrderGoesFromCustomerToKitchenAndIsMarkedReady() throws Exception {
        // 1. El cliente crea el pedido desde la tablet de la mesa.
        long orderId = createOnsiteOrder();

        // 2. El cocinero lo ve en su dashboard con el canal, la nota y los productos.
        mockMvc.perform(get(apiEndpoint + "/kitchen/orders").cookie(cookCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)].status", orderId).value(contains("PLACED")))
                .andExpect(jsonPath("$[?(@.id == %d)].channel", orderId).value(contains("ONSITE")))
                .andExpect(jsonPath("$[?(@.id == %d)].chefNote", orderId).value(contains("Sin wasabi")))
                .andExpect(jsonPath("$[?(@.id == %d)].hasPriorityNote", orderId).value(contains(true)))
                .andExpect(jsonPath("$[?(@.id == %d)].items[0].productName", orderId)
                        .value(contains("Flow Test Sushi")));

        // 3. Lo pasa a preparación y después a listo.
        updateKitchenStatus(orderId, "PROCESSING")
                .andExpect(jsonPath("$.status").value("PROCESSING"));
        updateKitchenStatus(orderId, "READY")
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.chefNote").value("Sin wasabi"));

        // 4. Ya no está entre las comandas activas, pero sí entre los pedidos listos.
        mockMvc.perform(get(apiEndpoint + "/kitchen/orders").cookie(cookCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)]", orderId).value(empty()));

        mockMvc.perform(get(apiEndpoint + "/orders").param("status", "READY").cookie(cookCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(hasItem((int) orderId)));
    }

    @Test
    void onlineCardOrderOnlyReachesKitchenAfterPayment() throws Exception {
        long orderId = createOnlineCardOrder();

        // Pendiente de pago online: la cocina todavía no lo ve.
        mockMvc.perform(get(apiEndpoint + "/kitchen/orders").cookie(cookCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(not(hasItem((int) orderId))));

        mockMvc.perform(patch(apiEndpoint + "/orders/" + orderId + "/paid")
                        .with(csrf())
                        .cookie(cookCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        // Pagado: aparece en cocina y en el filtro "A Domicilio", no en "En Sala".
        mockMvc.perform(get(apiEndpoint + "/kitchen/orders").param("channel", "ONLINE").cookie(cookCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)].status", orderId).value(contains("PAID")));

        mockMvc.perform(get(apiEndpoint + "/kitchen/orders").param("channel", "ONSITE").cookie(cookCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(not(hasItem((int) orderId))));
    }

    @Test
    void channelCountsIncludeNewOrders() throws Exception {
        createOnsiteOrder();

        mockMvc.perform(get(apiEndpoint + "/kitchen/orders/counts").cookie(cookCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.inStore").value(1))
                .andExpect(jsonPath("$.delivery").value(0));
    }

    @Test
    void customerCannotChangeKitchenStatus() throws Exception {
        long orderId = createOnsiteOrder();

        mockMvc.perform(patch(apiEndpoint + "/kitchen/orders/" + orderId + "/status")
                        .with(csrf())
                        .cookie(customerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"READY"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(apiEndpoint + "/kitchen/orders").cookie(cookCookie))
                .andExpect(jsonPath("$[?(@.id == %d)].status", orderId).value(contains("PLACED")));
    }

    @Test
    void kitchenRejectsInvalidStatus() throws Exception {
        long orderId = createOnsiteOrder();

        updateKitchenStatus(orderId, "PAID", status().isBadRequest());
    }

    private long createOnsiteOrder() throws Exception {
        MvcResult result = mockMvc.perform(post(apiEndpoint + "/orders")
                        .with(csrf())
                        .cookie(customerCookie)
                        .header("Device-Identifier", DEVICE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [{"productId": %d, "quantity": 2}],
                                  "chefNote": "Sin wasabi",
                                  "channel": "ONSITE",
                                  "paymentMethod": "CASH_ONSITE"
                                }
                                """.formatted(sushi.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.tableNumber").value(99))
                .andReturn();

        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private long createOnlineCardOrder() throws Exception {
        MvcResult result = mockMvc.perform(post(apiEndpoint + "/orders")
                        .with(csrf())
                        .cookie(customerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [{"productId": %d, "quantity": 1}],
                                  "channel": "ONLINE",
                                  "paymentMethod": "ONLINE_CARD",
                                  "deliveryAddress": {
                                    "deliveryStreet": "Calle Mayor 1",
                                    "deliveryCity": "Madrid",
                                    "deliveryPostalCode": "28001"
                                  }
                                }
                                """.formatted(sushi.getId())))
                .andExpect(status().isCreated())
                .andReturn();

        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private ResultActions updateKitchenStatus(long orderId, String newStatus)
            throws Exception {
        return updateKitchenStatus(orderId, newStatus, status().isOk());
    }

    private ResultActions updateKitchenStatus(long orderId, String newStatus,
            ResultMatcher expectedStatus) throws Exception {
        return mockMvc.perform(patch(apiEndpoint + "/kitchen/orders/" + orderId + "/status")
                        .with(csrf())
                        .cookie(cookCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"%s"}
                                """.formatted(newStatus)))
                .andExpect(expectedStatus);
    }

    private Cookie authCookie(UserEntity user, String role) {
        return new Cookie("access_token", jwtService.generateAuthToken(user.getEmail(), role).token());
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
