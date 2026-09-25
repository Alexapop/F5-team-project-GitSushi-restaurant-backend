package dev.team1.orders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import dev.team1.enums.OrderStatus;
import dev.team1.orders.dtos.KitchenOrderDTOResponse;
import dev.team1.orders.dtos.KitchenOrderDTOResponse.KitchenOrderItemDTO;

import dev.team1.security.JwtFilter;
import dev.team1.security.SecurityConfiguration;
import dev.team1.orders.dtos.KitchenMetricsDTOResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import org.springframework.security.test.context.support.WithMockUser;


@WebMvcTest(controllers = KitchenController.class, properties = "api-endpoint=api/v1")
@Import(SecurityConfiguration.class)
class KitchenControllerTest {

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
    @WithMockUser("COOK")
    void getActiveOrdersReturnsKitchenOrders() throws Exception {
        when(service.getActiveKitchenOrders()).thenReturn(List.of(kitchenResponse(OrderStatus.PROCESSING, false)));

        mockMvc.perform(get("/api/v1/kitchen/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("PROCESSING"))
                .andExpect(jsonPath("$[0].isDelayed").value(false));
        verify(service).getActiveKitchenOrders();
    }

    @Test
    @WithMockUser("COOK")
    void getActiveOrdersReturnsEmptyListWhenNoOrders() throws Exception {
        when(service.getActiveKitchenOrders()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/kitchen/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser("COOK")
    void getMetricsReturnsKitchenMetrics() throws Exception {
        KitchenMetricsDTOResponse metrics = new KitchenMetricsDTOResponse(3, 8.5, 2, 1, 0);
        when(service.getKitchenMetrics()).thenReturn(metrics);

        mockMvc.perform(get("/api/v1/kitchen/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActiveOrders").value(3))
                .andExpect(jsonPath("$.averagePreparationMinutes").value(8.5))
                .andExpect(jsonPath("$.processingCount").value(2))
                .andExpect(jsonPath("$.delayedCount").value(1))
                .andExpect(jsonPath("$.readyCount").value(0));
        verify(service).getKitchenMetrics();
    }

    @Test
    @WithMockUser("COOK")
    void getMetricsReturnsZerosWhenNoActiveOrders() throws Exception {
        KitchenMetricsDTOResponse metrics = new KitchenMetricsDTOResponse(0, 0.0, 0, 0, 0);
        when(service.getKitchenMetrics()).thenReturn(metrics);

        mockMvc.perform(get("/api/v1/kitchen/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActiveOrders").value(0));
        verify(service).getKitchenMetrics();
    }

    @Test
    @WithMockUser("COOK")
    void updateStatusReturnsUpdatedOrder() throws Exception {
        when(service.updateKitchenStatus(1L, OrderStatus.READY))
                .thenReturn(kitchenResponse(OrderStatus.READY, false));

        mockMvc.perform(patch("/api/v1/kitchen/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"READY"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));
        verify(service).updateKitchenStatus(1L, OrderStatus.READY);
    }

    @Test
    @WithMockUser("COOK")
    void updateStatusReturnsBadRequestForInvalidStatus() throws Exception {
        when(service.updateKitchenStatus(1L, OrderStatus.PAID))
                .thenThrow(new ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid kitchen status: PAID"));

        mockMvc.perform(patch("/api/v1/kitchen/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PAID"}
                                """))
                .andExpect(status().isBadRequest());
        verify(service).updateKitchenStatus(1L, OrderStatus.PAID);
    }

    @Test
    @WithMockUser("COOK")
    void updateStatusReturnsNotFoundWhenOrderMissing() throws Exception {
        when(service.updateKitchenStatus(99L, OrderStatus.READY))
                .thenThrow(new ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Order not found: 99"));

        mockMvc.perform(patch("/api/v1/kitchen/orders/99/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"READY"}
                                """))
                .andExpect(status().isNotFound());
    }

    private KitchenOrderDTOResponse kitchenResponse(OrderStatus status, boolean isDelayed) {
        return new KitchenOrderDTOResponse(
                1L, status, "No onions", LocalDateTime.now(), isDelayed,
                List.of(new KitchenOrderItemDTO("Sushi", new BigDecimal("2"))), null);
    }
}