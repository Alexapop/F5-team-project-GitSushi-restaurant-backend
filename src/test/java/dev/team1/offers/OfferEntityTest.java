package dev.team1.offers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class OfferEntityTest {

    @Test
    void onCreate_generatesCoupon() {
        OfferEntity offer = new OfferEntity();

        offer.onCreate();

        assertThat(offer.getCoupon(), is(notNullValue()));
    }

    @Test
    void onCreate_generatesNewCouponOnEachCall() {
        OfferEntity offer = new OfferEntity();

        offer.onCreate();
        UUID first = offer.getCoupon();
        offer.onCreate();

        assertThat(offer.getCoupon(), is(not(first)));
    }

    @Test
    void onCreate_calculatesFinalPrice_whenNotProvided() {
        OfferEntity offer = OfferEntity.builder()
                .originalPrice(new BigDecimal("100"))
                .discountRate(new BigDecimal("20"))
                .build();

        offer.onCreate();

        assertThat(offer.getFinalPrice(), comparesEqualTo(new BigDecimal("80")));
    }

    @Test
    void onCreate_keepsFinalPrice_whenAlreadyProvided() {
        OfferEntity offer = OfferEntity.builder()
                .originalPrice(new BigDecimal("100"))
                .discountRate(new BigDecimal("20"))
                .finalPrice(new BigDecimal("50"))
                .build();

        offer.onCreate();

        assertThat(offer.getFinalPrice(), comparesEqualTo(new BigDecimal("50")));
    }

    @Test
    void onCreate_doesNotCalculateFinalPrice_whenOriginalPriceMissing() {
        OfferEntity offer = OfferEntity.builder()
                .discountRate(new BigDecimal("20"))
                .build();

        offer.onCreate();

        assertThat(offer.getFinalPrice(), is(nullValue()));
    }

    @Test
    void onCreate_doesNotCalculateFinalPrice_whenDiscountRateMissing() {
        OfferEntity offer = OfferEntity.builder()
                .originalPrice(new BigDecimal("100"))
                .build();

        offer.onCreate();

        assertThat(offer.getFinalPrice(), is(nullValue()));
    }

    @Test
    void newOffer_isNotUsedByDefault() {
        assertThat(new OfferEntity().isUsed(), is(false));
    }
}