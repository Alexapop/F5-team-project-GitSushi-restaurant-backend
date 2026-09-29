package dev.team1.orders;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.team1.orders.dtos.DeliveryMetricsDTOResponse;

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
}
