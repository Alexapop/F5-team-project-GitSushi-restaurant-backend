package dev.team1.tickets;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.team1.auth.CustomUserDetails;
import dev.team1.orders.OrderService;
import dev.team1.tickets.dtos.TicketDTOResponse;

// GS-562: detalle completo del pedido (ticket) para el cliente.
@RestController
@RequestMapping(path = "${api-endpoint}/tickets")
public class TicketController {

    private final OrderService orderService;

    public TicketController(OrderService orderService) {
        this.orderService = orderService;
    }

    // Usuario autenticado: ve sus pedidos (admin, todos). Invitado: necesita el token que recibió al crear el pedido.
    @GetMapping("/{id}")
    public ResponseEntity<TicketDTOResponse> getTicket(
            @PathVariable Long id,
            @RequestParam(required = false) String token,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        UUID userId = null;
        boolean isAdmin = false;

        if (currentUser != null) {
            userId = currentUser.user().getId();
            isAdmin = currentUser.getAuthorities().stream()
                    .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        }

        return ResponseEntity.ok(orderService.getTicket(id, userId, isAdmin, token));
    }
}
