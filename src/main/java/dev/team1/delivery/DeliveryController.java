package dev.team1.delivery;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.team1.delivery.dtos.DeliveryConfirmationDTORequest;
import dev.team1.delivery.dtos.DeliveryMetricsDTOResponse;
import dev.team1.orders.OrderService;
import dev.team1.orders.dtos.OrderDTOResponse;

@RestController
@RequestMapping(path = "${api-endpoint}/delivery")
public class DeliveryController {

    private final OrderService orderService;

    public DeliveryController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/metrics")
    public ResponseEntity<DeliveryMetricsDTOResponse> getMetrics() {
        return ResponseEntity.ok(orderService.getDeliveryMetrics());
    }

    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<OrderDTOResponse> markAsDelivered(
            @PathVariable Long id,
            @RequestBody(required = false) DeliveryConfirmationDTORequest request) {
        return ResponseEntity.ok(orderService.markAsDelivered(id, request));
    }
}