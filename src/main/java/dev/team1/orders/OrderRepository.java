package dev.team1.orders;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    // Inherited CRUD methods are sufficient for creating orders for now

}
