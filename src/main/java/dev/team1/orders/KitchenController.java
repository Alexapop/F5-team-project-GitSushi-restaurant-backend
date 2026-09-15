package dev.team1.orders;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.team1.orders.dtos.KitchenOrderDTOResponse;

@RestController
@RequestMapping(path = "${api-endpoint}/cocina")
public class KitchenController {

    private final OrderService orderService;

    public KitchenController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/comandas")
    public ResponseEntity<List<KitchenOrderDTOResponse>> getActiveOrders() {
        return ResponseEntity.ok(orderService.getActiveKitchenOrders());
    }
}
