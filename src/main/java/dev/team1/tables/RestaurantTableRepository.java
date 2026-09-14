package dev.team1.tables;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantTableRepository extends JpaRepository<RestaurantTableEntity, Long> {
    Optional<RestaurantTableEntity> findByDeviceIdentifier(String deviceIdentifier);
}
