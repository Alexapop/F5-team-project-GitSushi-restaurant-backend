package dev.team1.orders;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.team1.auth.CustomUserDetails;
import dev.team1.orders.dtos.OrderHistoryDTOResponse;

// GS-600: historial de pedidos del cliente, paginado y del más reciente al más antiguo.
@RestController
@RequestMapping(path = "${api-endpoint}/users")
public class OrderHistoryController {

    private final OrderService orderService;

    public OrderHistoryController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{id}/orders")
    public ResponseEntity<Page<OrderHistoryDTOResponse>> getOrderHistory(
            @PathVariable UUID id,
            @PageableDefault(size = 3) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        UUID currentUserId = currentUser.user().getId();
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        return ResponseEntity.ok(orderService.getOrderHistory(id, currentUserId, isAdmin, pageable));
    }
}
