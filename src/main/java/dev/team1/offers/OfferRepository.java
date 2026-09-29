package dev.team1.offers;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;


public interface OfferRepository extends JpaRepository<OfferEntity, Long> {

    Optional<OfferEntity> findByCoupon(UUID coupon);

}
