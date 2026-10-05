package dev.team1.invoices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import dev.team1.enums.OrderChannel;
import dev.team1.enums.OrderStatus;
import dev.team1.enums.PaymentMethod;
import dev.team1.enums.PaymentStatus;
import dev.team1.orders.OrderEntity;
import dev.team1.orders.OrderRepository;
import dev.team1.roles.RoleEntity;
import dev.team1.roles.RoleRepository;
import dev.team1.security.JwtService;
import dev.team1.users.UserEntity;
import dev.team1.users.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;

// Test de integración: al cobrar un pedido (pago confirmado o efectivo a la entrega)
// se guarda su factura en la base de datos, una sola vez por pedido.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class InvoiceOnPaymentIntegrationTest {

    private static final String ADMIN_ROLE = "ROLE_ADMIN";
    private static final BigDecimal ORDER_TOTAL = new BigDecimal("23.10");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private InvoiceRepository invoiceRepository;
    @Autowired
    private EntityManager entityManager;

    @Value("/${api-endpoint}")
    private String apiEndpoint;

    private UserEntity admin;

    @BeforeEach
    void setUp() {
        admin = userRepository.save(newUser("invoice-admin@test.com", findOrCreateRole(ADMIN_ROLE)));
    }

    @Test
    void markAsPaid_createsInvoiceWithOrderTotal() throws Exception {
        OrderEntity order = orderRepository.save(newOrder(
            OrderChannel.ONSITE, PaymentMethod.CASH_ONSITE, PaymentStatus.PENDING_CASH, OrderStatus.PLACED));

        mockMvc.perform(patch(apiEndpoint + "/orders/" + order.getId() + "/paid")
                .with(csrf())
                .cookie(accessTokenFor(admin)))
            .andExpect(status().isOk());

        List<InvoiceEntity> invoices = invoicesOf(order);
        assertThat(invoices).hasSize(1);
        assertThat(invoices.get(0).getAmount()).isEqualByComparingTo(ORDER_TOTAL);
        assertThat(invoices.get(0).getPaidAt()).isNotNull();
    }

    @Test
    void markAsPaidTwice_keepsASingleInvoice() throws Exception {
        OrderEntity order = orderRepository.save(newOrder(
            OrderChannel.ONSITE, PaymentMethod.CARD_ONSITE, PaymentStatus.PENDING_CARD_TERMINAL, OrderStatus.PLACED));

        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(patch(apiEndpoint + "/orders/" + order.getId() + "/paid")
                    .with(csrf())
                    .cookie(accessTokenFor(admin)))
                .andExpect(status().isOk());
        }

        assertThat(invoicesOf(order)).hasSize(1);
    }

    @Test
    void cashCollectedOnDelivery_createsInvoice() throws Exception {
        OrderEntity order = orderRepository.save(newOrder(
            OrderChannel.ONLINE, PaymentMethod.CASH_ON_DELIVERY, PaymentStatus.PENDING_CASH_ON_DELIVERY,
            OrderStatus.ONTHEWAY));

        mockMvc.perform(patch(apiEndpoint + "/delivery/orders/" + order.getId() + "/status")
                .with(csrf())
                .cookie(accessTokenFor(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cashCollected\": true}"))
            .andExpect(status().isOk());

        assertThat(invoicesOf(order)).hasSize(1);
    }

    // Vacía la caché de JPA para leer las facturas de la base de datos de verdad.
    private List<InvoiceEntity> invoicesOf(OrderEntity order) {
        entityManager.flush();
        entityManager.clear();
        return invoiceRepository.findAll().stream()
            .filter(invoice -> invoice.getOrder() != null
                && invoice.getOrder().getId().equals(order.getId()))
            .toList();
    }

    private OrderEntity newOrder(OrderChannel channel, PaymentMethod paymentMethod,
            PaymentStatus paymentStatus, OrderStatus status) {
        OrderEntity order = new OrderEntity();
        order.setSubtotal(new BigDecimal("21.00"));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setVatRate(10);
        order.setVatAmount(new BigDecimal("2.10"));
        order.setTotal(ORDER_TOTAL);
        order.setChannel(channel);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus(paymentStatus);
        order.setStatus(status);
        return order;
    }

    private Cookie accessTokenFor(UserEntity user) {
        return new Cookie("access_token", jwtService.generateAuthToken(user.getEmail(), ADMIN_ROLE).token());
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