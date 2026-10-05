package dev.team1.orders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import dev.team1.auth.CustomUserDetails;
import dev.team1.config.PaginationJsonGlobalConfiguration;
import dev.team1.orders.dtos.OrderHistoryDTOResponse;
import dev.team1.orders.dtos.OrderHistoryDTOResponse.OrderHistoryItemDTO;
import dev.team1.orders.dtos.RepeatOrderItemDTOResponse;
import dev.team1.roles.RoleEntity;
import dev.team1.security.JwtFilter;
import dev.team1.security.SecurityConfiguration;
import dev.team1.users.UserEntity;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

// GS-582 / GS-583: historial de pedidos y "Repetir orden ahora".
@WebMvcTest(controllers = { OrderHistoryController.class, OrderController.class },
        properties = "api-endpoint=api/v1")
@Import({ SecurityConfiguration.class, PaginationJsonGlobalConfiguration.class })
class OrderHistoryControllerTest {

    private static final UUID CUSTOMER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

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
    void getOrderHistoryReturnsPagedOrdersForOwner() throws Exception {
        CustomUserDetails customer = principal(CUSTOMER_ID, "ROLE_CUSTOMER");
        when(service.getOrderHistory(eq(CUSTOMER_ID), eq(CUSTOMER_ID), eq(false), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(historyOrder(105L)), PageRequest.of(0, 3), 5));

        mockMvc.perform(get("/api/v1/users/" + CUSTOMER_ID + "/orders").with(user(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(105))
                .andExpect(jsonPath("$.content[0].date").isNotEmpty())
                .andExpect(jsonPath("$.content[0].total").value(17.5))
                .andExpect(jsonPath("$.content[0].items[0].productId").value(3))
                .andExpect(jsonPath("$.content[0].items[0].name").value("Kaisen Init"))
                .andExpect(jsonPath("$.content[0].items[0].quantity").value(2))
                .andExpect(jsonPath("$.content[0].items[0].price").value(6.5))
                .andExpect(jsonPath("$.content[0].items[0].available").value(true))
                .andExpect(jsonPath("$.page.totalElements").value(5))
                .andExpect(jsonPath("$.page.totalPages").value(2))
                .andExpect(jsonPath("$.page.number").value(0));
    }

    @Test
    void getOrderHistoryUsesPageSizeThreeByDefault() throws Exception {
        CustomUserDetails customer = principal(CUSTOMER_ID, "ROLE_CUSTOMER");
        when(service.getOrderHistory(eq(CUSTOMER_ID), eq(CUSTOMER_ID), eq(false), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/users/" + CUSTOMER_ID + "/orders").with(user(customer)))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).getOrderHistory(eq(CUSTOMER_ID), eq(CUSTOMER_ID), eq(false), captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(3, captor.getValue().getPageSize());
        org.junit.jupiter.api.Assertions.assertEquals(0, captor.getValue().getPageNumber());
    }

    @Test
    void getOrderHistoryPassesRequestedPageToService() throws Exception {
        CustomUserDetails customer = principal(CUSTOMER_ID, "ROLE_CUSTOMER");
        when(service.getOrderHistory(eq(CUSTOMER_ID), eq(CUSTOMER_ID), eq(false), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/users/" + CUSTOMER_ID + "/orders")
                        .param("page", "1").param("size", "10")
                        .with(user(customer)))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).getOrderHistory(eq(CUSTOMER_ID), eq(CUSTOMER_ID), eq(false), captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(1, captor.getValue().getPageNumber());
        org.junit.jupiter.api.Assertions.assertEquals(10, captor.getValue().getPageSize());
    }

    @Test
    void getOrderHistoryReturnsForbiddenForAnotherCustomer() throws Exception {
        CustomUserDetails other = principal(OTHER_ID, "ROLE_CUSTOMER");
        when(service.getOrderHistory(eq(CUSTOMER_ID), eq(OTHER_ID), eq(false), any(Pageable.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden"));

        mockMvc.perform(get("/api/v1/users/" + CUSTOMER_ID + "/orders").with(user(other)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOrderHistoryPassesAdminFlagToService() throws Exception {
        CustomUserDetails admin = principal(OTHER_ID, "ROLE_ADMIN");
        when(service.getOrderHistory(eq(CUSTOMER_ID), eq(OTHER_ID), eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/users/" + CUSTOMER_ID + "/orders").with(user(admin)))
                .andExpect(status().isOk());

        verify(service).getOrderHistory(eq(CUSTOMER_ID), eq(OTHER_ID), eq(true), any(Pageable.class));
    }

    @Test
    void getOrderHistoryRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + CUSTOMER_ID + "/orders"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void getOrderHistoryReturnsBadRequestForInvalidUserId() throws Exception {
        CustomUserDetails customer = principal(CUSTOMER_ID, "ROLE_CUSTOMER");

        mockMvc.perform(get("/api/v1/users/not-a-uuid/orders").with(user(customer)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void getRepeatOrderItemsReturnsAvailableLines() throws Exception {
        CustomUserDetails customer = principal(CUSTOMER_ID, "ROLE_CUSTOMER");
        when(service.getRepeatOrderItems(105L, CUSTOMER_ID, false)).thenReturn(List.of(
                new RepeatOrderItemDTOResponse(3L, "Kaisen Init", new BigDecimal("7.00"), 2)));

        mockMvc.perform(get("/api/v1/orders/105/repeat").with(user(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productId").value(3))
                .andExpect(jsonPath("$[0].name").value("Kaisen Init"))
                .andExpect(jsonPath("$[0].price").value(7.0))
                .andExpect(jsonPath("$[0].quantity").value(2));

        verify(service).getRepeatOrderItems(105L, CUSTOMER_ID, false);
    }

    @Test
    void getRepeatOrderItemsReturnsNotFoundWhenOrderMissing() throws Exception {
        CustomUserDetails customer = principal(CUSTOMER_ID, "ROLE_CUSTOMER");
        when(service.getRepeatOrderItems(999L, CUSTOMER_ID, false))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: 999"));

        mockMvc.perform(get("/api/v1/orders/999/repeat").with(user(customer)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRepeatOrderItemsReturnsForbiddenForAnotherCustomer() throws Exception {
        CustomUserDetails other = principal(OTHER_ID, "ROLE_CUSTOMER");
        when(service.getRepeatOrderItems(105L, OTHER_ID, false))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden"));

        mockMvc.perform(get("/api/v1/orders/105/repeat").with(user(other)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getRepeatOrderItemsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/orders/105/repeat"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void getRepeatOrderItemsReturnsBadRequestForInvalidOrderId() throws Exception {
        CustomUserDetails customer = principal(CUSTOMER_ID, "ROLE_CUSTOMER");

        mockMvc.perform(get("/api/v1/orders/abc/repeat").with(user(customer)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    private OrderHistoryDTOResponse historyOrder(Long id) {
        return new OrderHistoryDTOResponse(
                id,
                LocalDateTime.of(2026, 9, 20, 21, 10),
                List.of(new OrderHistoryItemDTO(3L, "Kaisen Init", 2, new BigDecimal("6.50"), true)),
                new BigDecimal("17.50"));
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
}
