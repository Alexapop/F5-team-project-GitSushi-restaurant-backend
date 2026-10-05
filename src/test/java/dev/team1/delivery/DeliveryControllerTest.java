package dev.team1.delivery;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.team1.auth.CustomUserDetails;
import dev.team1.delivery.dtos.DeliveryMetricsDTOResponse;
import dev.team1.enums.OrderStatus;
import dev.team1.orders.OrderService;
import dev.team1.orders.dtos.PendingDeliveryDTOResponse;

import dev.team1.security.JwtFilter;
import dev.team1.security.SecurityConfiguration;
import dev.team1.users.UserEntity;
import dev.team1.roles.RoleEntity;
import org.springframework.http.MediaType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

@WebMvcTest(controllers = DeliveryController.class, properties = "api-endpoint=api/v1")
@Import(SecurityConfiguration.class)
class DeliveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService service;

    @MockitoBean
    JwtFilter jwtFilter;

    @BeforeEach
    void setup() throws Exception {
        doAnswer(invocation -> {
            ServletRequest req = invocation.getArgument(0);
            ServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "DELIVERYMAN")
    void getMetricsReturnsDeliveryMetrics() throws Exception {
        DeliveryMetricsDTOResponse metrics = new DeliveryMetricsDTOResponse(4, 2, 7, 18.5);
        when(service.getDeliveryMetrics()).thenReturn(metrics);

        mockMvc.perform(get("/api/v1/delivery/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readyCount").value(4))
                .andExpect(jsonPath("$.inTransitCount").value(2))
                .andExpect(jsonPath("$.deliveredTodayCount").value(7))
                .andExpect(jsonPath("$.averageDeliveryMinutes").value(18.5));
        verify(service).getDeliveryMetrics();
    }

    @Test
    @WithMockUser(roles = "DELIVERYMAN")
    void getMetricsReturnsZerosWhenNoOrders() throws Exception {
        DeliveryMetricsDTOResponse metrics = new DeliveryMetricsDTOResponse(0, 0, 0, 0.0);
        when(service.getDeliveryMetrics()).thenReturn(metrics);

        mockMvc.perform(get("/api/v1/delivery/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readyCount").value(0))
                .andExpect(jsonPath("$.inTransitCount").value(0))
                .andExpect(jsonPath("$.deliveredTodayCount").value(0))
                .andExpect(jsonPath("$.averageDeliveryMinutes").value(0.0));
        verify(service).getDeliveryMetrics();
    }

    @Test
    @WithMockUser(roles = "DELIVERYMAN")
    void markAsDeliveredReturnsUpdatedOrder() throws Exception {
        when(service.markAsDelivered(eq(1L), any()))
                .thenReturn(new dev.team1.orders.dtos.OrderDTOResponse(
                        1L, null, null, null, null, null, null, null,
                                                OrderStatus.DELIVERED, null, null, null, null, null, null, null));

        mockMvc.perform(patch("/api/v1/delivery/orders/1/status")
                        .secure(true)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
        verify(service).markAsDelivered(eq(1L), any());
    }

    @Test
    @WithMockUser(roles = "DELIVERYMAN")
    void markAsDeliveredReturnsConflictWhenNotOnTheWay() throws Exception {
        when(service.markAsDelivered(eq(1L), any()))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT,
                        "Cannot mark as delivered from status: PROCESSING"));

        mockMvc.perform(patch("/api/v1/delivery/orders/1/status")
                        .secure(true)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "DELIVERYMAN")
    void markAsDeliveredReturnsBadRequestWithoutCashConfirmation() throws Exception {
        when(service.markAsDelivered(eq(1L), any()))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "Cash collection must be confirmed for cash-on-delivery orders"));

        mockMvc.perform(patch("/api/v1/delivery/orders/1/status")
                        .secure(true)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "DELIVERYMAN")
    void assignDeliverymanReturnsUpdatedOrder() throws Exception {
        UUID deliverymanId = UUID.randomUUID();
        UserEntity deliveryman = new UserEntity();
        deliveryman.setId(deliverymanId);
        RoleEntity role = new RoleEntity();
        role.setName("ROLE_DELIVERYMAN");
        deliveryman.setRoles(Set.of(role));

                when(service.assignDeliveryman(1L, deliverymanId))
                .thenReturn(new dev.team1.orders.dtos.OrderDTOResponse(
                        1L, null, null, null, null, null, null, null,
                                        OrderStatus.READY, null, null, null, null, null, null, null));

        mockMvc.perform(patch("/api/v1/delivery/orders/1/assign")
                        .secure(true)
                        .with(user(new CustomUserDetails(deliveryman)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));
        verify(service).assignDeliveryman(1L, deliverymanId);
    }

    @Test
    @WithMockUser(roles = "DELIVERYMAN")
    void assignDeliverymanReturnsConflictWhenNotAvailable() throws Exception {
        UUID deliverymanId = UUID.randomUUID();
        UserEntity deliveryman = new UserEntity();
        deliveryman.setId(deliverymanId);
        RoleEntity role = new RoleEntity();
        role.setName("ROLE_DELIVERYMAN");
        deliveryman.setRoles(Set.of(role));

        when(service.assignDeliveryman(1L, deliverymanId))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT,
                        "Order is not available for delivery assignment: PROCESSING"));

        mockMvc.perform(patch("/api/v1/delivery/orders/1/assign")
                        .secure(true)
                        .with(user(new CustomUserDetails(deliveryman)))
                        .with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "DELIVERYMAN")
    void assignDeliverymanReturnsUnauthorizedWhenDeliverymanUnknown() throws Exception {
        UUID deliverymanId = UUID.randomUUID();
        UserEntity deliveryman = new UserEntity();
        deliveryman.setId(deliverymanId);
        RoleEntity role = new RoleEntity();
        role.setName("ROLE_DELIVERYMAN");
        deliveryman.setRoles(Set.of(role));

        when(service.assignDeliveryman(1L, deliverymanId))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED,
                        "Authenticated user no longer exists"));

        mockMvc.perform(patch("/api/v1/delivery/orders/1/assign")
                        .secure(true)
                        .with(user(new CustomUserDetails(deliveryman)))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "DELIVERYMAN")
    void getPendingDeliveriesReturnsList() throws Exception {
        when(service.getPendingDeliveries())
                .thenReturn(List.of(new PendingDeliveryDTOResponse(1L, "Calle Mayor 1, Gijón")));

        mockMvc.perform(get("/api/v1/delivery/orders/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].address").value("Calle Mayor 1, Gijón"));
        verify(service).getPendingDeliveries();
    }

    @Test
    @WithMockUser(roles = "DELIVERYMAN")
    void getPendingDeliveriesReturnsEmptyListWhenNoneAvailable() throws Exception {
        when(service.getPendingDeliveries()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/delivery/orders/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        verify(service).getPendingDeliveries();
    }
}