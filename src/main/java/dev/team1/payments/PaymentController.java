package dev.team1.payments;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.team1.orders.dtos.OrderDTOResponse;
import dev.team1.payments.dtos.PaymentDTORequest;
import dev.team1.payments.dtos.PaymentDTOResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;

// GS-341: endpoints de pago con tarjeta online. Son públicos (invitado o usuario autenticado).
@RestController
@RequestMapping(path = "${api-endpoint}/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // Crea la sesión de Stripe y devuelve la URL a la que el frontend redirige al cliente.
    @PostMapping("/checkout")
    public ResponseEntity<PaymentDTOResponse> createCheckoutSession(
            @Valid @RequestBody PaymentDTORequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createCheckoutSession(request));
    }

    // El frontend lo llama al volver de Stripe; devuelve el pedido ya marcado como pagado.
    @PostMapping("/confirm")
    public ResponseEntity<OrderDTOResponse> confirmPayment(@RequestParam String sessionId) {
        return ResponseEntity.ok(paymentService.confirmPayment(sessionId));
    }
}
