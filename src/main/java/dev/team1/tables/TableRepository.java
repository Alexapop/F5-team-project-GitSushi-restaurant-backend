package dev.team1.tables;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TableRepository extends JpaRepository<TableEntity, Long> {
    Optional<TableEntity> findByDeviceIdentifier(String deviceIdentifier);
}
