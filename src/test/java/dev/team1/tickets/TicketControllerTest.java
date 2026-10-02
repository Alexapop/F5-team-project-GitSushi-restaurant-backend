package dev.team1.tickets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import dev.team1.auth.CustomUserDetails;
import dev.team1.delivery.dtos.DeliveryAddressDTOResponse;
import dev.team1.enums.OrderChannel;
import dev.team1.enums.OrderStatus;
import dev.team1.enums.PaymentMethod;
import dev.team1.enums.PaymentStatus;
import dev.team1.orders.OrderService;
import dev.team1.roles.RoleEntity;
import dev.team1.security.JwtFilter;
import dev.team1.security.SecurityConfiguration;
import dev.team1.tickets.dtos.TicketDTOResponse;
import dev.team1.tickets.dtos.TicketDTOResponse.TicketItemDTO;
import dev.team1.users.UserEntity;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

@WebMvcTest(controllers = TicketController.class, properties = "api-endpoint=api/v1")
@Import(SecurityConfiguration.class)
class TicketControllerTest {

    private static final UUID CUSTOMER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ADMIN_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String TICKET_TOKEN =
            "33333333-3333-3333-3333-333333333333";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService service;

    @MockitoBean
    private JwtFilter jwtFilter;

    @BeforeEach
    void setup() throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
    }

    @Test
    void getTicketReturnsOnsiteDetailsForGuestWithToken() throws Exception {
        when(service.getTicket(1L, null, false, TICKET_TOKEN))
                .thenReturn(onsiteTicket());

        mockMvc.perform(get("/api/v1/tickets/1").param("token", TICKET_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.channel").value("ONSITE"))
                .andExpect(jsonPath("$.tableNumber").value(12))
                .andExpect(jsonPath("$.paidAt").isNotEmpty())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productName").value("Sushi"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].unitPrice").value(10.0))
                .andExpect(jsonPath("$.items[0].lineTotal").value(20.0))
                .andExpect(jsonPath("$.subtotal").value(20.0))
                .andExpect(jsonPath("$.discountAmount").value(2.0))
                .andExpect(jsonPath("$.vatRate").value(10))
                .andExpect(jsonPath("$.vatAmount").value(1.8))
                .andExpect(jsonPath("$.deliveryFee").value(0.0))
                .andExpect(jsonPath("$.total").value(19.8))
                .andExpect(jsonPath("$.paymentMethod").value("CASH_ONSITE"))
                .andExpect(jsonPath("$.paymentStatus").isEmpty())
                .andExpect(jsonPath("$.deliveryAddress").isEmpty())
                .andExpect(jsonPath("$.ticketAccessToken").doesNotExist());

        verify(service).getTicket(1L, null, false, TICKET_TOKEN);
    }

    @Test
    void getTicketPassesAuthenticatedCustomerIdToService() throws Exception {
        CustomUserDetails customer = principal(CUSTOMER_ID, "ROLE_CUSTOMER");
        when(service.getTicket(1L, CUSTOMER_ID, false, null)).thenReturn(onsiteTicket());

        mockMvc.perform(get("/api/v1/tickets/1").with(user(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(service).getTicket(1L, CUSTOMER_ID, false, null);
    }

    @Test
    void getTicketPassesAdminPermissionToService() throws Exception {
        CustomUserDetails admin = principal(ADMIN_ID, "ROLE_ADMIN");
        when(service.getTicket(1L, ADMIN_ID, true, null)).thenReturn(onsiteTicket());

        mockMvc.perform(get("/api/v1/tickets/1").with(user(admin)))
                .andExpect(status().isOk());

        verify(service).getTicket(1L, ADMIN_ID, true, null);
    }

    @Test
    void getTicketReturnsDeliveryAddressAndPendingPayment() throws Exception {
        CustomUserDetails customer = principal(CUSTOMER_ID, "ROLE_CUSTOMER");
        when(service.getTicket(2L, CUSTOMER_ID, false, null)).thenReturn(deliveryTicket());

        mockMvc.perform(get("/api/v1/tickets/2").with(user(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.channel").value("ONLINE"))
                .andExpect(jsonPath("$.tableNumber").isEmpty())
                .andExpect(jsonPath("$.paidAt").isEmpty())
                .andExpect(jsonPath("$.deliveryFee").value(2.5))
                .andExpect(jsonPath("$.total").value(22.3))
                .andExpect(jsonPath("$.paymentMethod").value("CASH_ON_DELIVERY"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING_CASH_ON_DELIVERY"))
                .andExpect(jsonPath("$.deliveryAddress.deliveryStreet").value("Calle Mayor 10"))
                .andExpect(jsonPath("$.deliveryAddress.deliveryCity").value("Madrid"))
                .andExpect(jsonPath("$.deliveryAddress.deliveryPostalCode").value("28001"))
                .andExpect(jsonPath("$.deliveryAddress.deliveryInstructions").value("Llamar al llegar"));

        verify(service).getTicket(2L, CUSTOMER_ID, false, null);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "wrong-token"})
    void getTicketReturnsForbiddenWhenServiceRejectsGuestToken(String token) throws Exception {
        when(service.getTicket(1L, null, false, token))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "You are not allowed to see this ticket"));

        var request = get("/api/v1/tickets/1");
        if (token != null) {
            request.param("token", token);
        }

        mockMvc.perform(request).andExpect(status().isForbidden());

        verify(service).getTicket(1L, null, false, token);
    }

    @Test
    void getTicketReturnsForbiddenWhenServiceRejectsCustomer() throws Exception {
        CustomUserDetails customer = principal(CUSTOMER_ID, "ROLE_CUSTOMER");
        when(service.getTicket(1L, CUSTOMER_ID, false, null))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "You are not allowed to see this ticket"));

        mockMvc.perform(get("/api/v1/tickets/1").with(user(customer)))
                .andExpect(status().isForbidden());

        verify(service).getTicket(1L, CUSTOMER_ID, false, null);
    }

    @Test
    void getTicketReturnsNotFoundWhenOrderMissing() throws Exception {
        when(service.getTicket(99L, null, false, TICKET_TOKEN))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: 99"));

        mockMvc.perform(get("/api/v1/tickets/99").param("token", TICKET_TOKEN))
                .andExpect(status().isNotFound());

        verify(service).getTicket(99L, null, false, TICKET_TOKEN);
    }

    @Test
    void getTicketReturnsBadRequestForNonNumericId() throws Exception {
        mockMvc.perform(get("/api/v1/tickets/abc").param("token", TICKET_TOKEN))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    private CustomUserDetails principal(UUID id, String roleName) {
        RoleEntity role = new RoleEntity();
        role.setName(roleName);
        UserEntity account = new UserEntity();
        account.setId(id);
        account.setEmail(id + "@example.com");
        account.setPassword("test-password");
        account.setRoles(Set.of(role));
        return new CustomUserDetails(account);
    }

    private List<TicketItemDTO> ticketItems() {
        return List.of(new TicketItemDTO(
                "Sushi", new BigDecimal("2"), new BigDecimal("10.00"), new BigDecimal("20.00")));
    }

    private TicketDTOResponse onsiteTicket() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 2, 12, 0);
        return new TicketDTOResponse(
                1L, OrderStatus.PAID, OrderChannel.ONSITE, 12,
                createdAt, createdAt.plusMinutes(2), ticketItems(),
                new BigDecimal("20.00"), new BigDecimal("2.00"), 10,
                new BigDecimal("1.80"), new BigDecimal("0.00"), new BigDecimal("19.80"),
                PaymentMethod.CASH_ONSITE, null, null);
    }

    private TicketDTOResponse deliveryTicket() {
        DeliveryAddressDTOResponse address = new DeliveryAddressDTOResponse(
                "Calle Mayor 10", "Madrid", "28001", "Llamar al llegar");
        return new TicketDTOResponse(
                2L, OrderStatus.ONTHEWAY, OrderChannel.ONLINE, null,
                LocalDateTime.of(2026, 10, 2, 12, 0), null, ticketItems(),
                new BigDecimal("20.00"), new BigDecimal("2.00"), 10,
                new BigDecimal("1.80"), new BigDecimal("2.50"), new BigDecimal("22.30"),
                PaymentMethod.CASH_ON_DELIVERY, PaymentStatus.PENDING_CASH_ON_DELIVERY, address);
    }
}
