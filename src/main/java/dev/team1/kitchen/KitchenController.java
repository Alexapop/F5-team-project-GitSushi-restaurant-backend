package dev.team1.kitchen;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.team1.enums.OrderChannel;
import dev.team1.kitchen.dtos.KitchenChannelCountsDTOResponse;
import dev.team1.kitchen.dtos.KitchenMetricsDTOResponse;
import dev.team1.kitchen.dtos.KitchenOrderDTOResponse;
import dev.team1.kitchen.dtos.KitchenStatusUpdateDTORequest;
import dev.team1.orders.OrderService;

@RestController
@RequestMapping(path = "${api-endpoint}/kitchen")
public class KitchenController {

    private final OrderService orderService;

    public KitchenController(OrderService orderService) {
        this.orderService = orderService;
    }

    // GS-685/GS-688: ?channel=ONSITE|ONLINE; sin parámetro devuelve ambos canales.
    // Un valor no válido lanza MethodArgumentTypeMismatchException -> 400 (GlobalExceptionHandler).
    @GetMapping("/orders")
    public ResponseEntity<List<KitchenOrderDTOResponse>> getActiveOrders(
            @RequestParam(required = false) OrderChannel channel) {
        return ResponseEntity.ok(orderService.getActiveKitchenOrders(channel));
    }

    @GetMapping("/orders/counts")
    public ResponseEntity<KitchenChannelCountsDTOResponse> getChannelCounts() {
        return ResponseEntity.ok(orderService.getKitchenChannelCounts());
    }

    @GetMapping("/metrics")
    public ResponseEntity<KitchenMetricsDTOResponse> getMetrics() {
        return ResponseEntity.ok(orderService.getKitchenMetrics());
    }

    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<KitchenOrderDTOResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody KitchenStatusUpdateDTORequest request) {
        return ResponseEntity.ok(orderService.updateKitchenStatus(id, request.status()));
    }
}
