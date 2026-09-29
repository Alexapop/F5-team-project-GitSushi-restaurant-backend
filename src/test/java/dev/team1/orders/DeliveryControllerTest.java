package dev.team1.orders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.team1.orders.dtos.DeliveryMetricsDTOResponse;
import dev.team1.security.JwtFilter;
import dev.team1.security.SecurityConfiguration;
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
    @WithMockUser("DELIVERYMAN")
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
    @WithMockUser("DELIVERYMAN")
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
}
