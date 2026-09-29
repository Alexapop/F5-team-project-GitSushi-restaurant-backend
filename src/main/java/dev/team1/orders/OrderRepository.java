package dev.team1.orders;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import dev.team1.enums.OrderStatus;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

   List <OrderEntity> findByStatus(OrderStatus status);
   List<OrderEntity> findByStatusIn(List<OrderStatus> statuses);

}
