package dev.team1.offers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import dev.team1.offers.dtos.OfferDTOResponse;
import dev.team1.offers.exceptions.OfferException;
import dev.team1.products.ProductEntity;
import dev.team1.users.UserEntity;
import dev.team1.users.UserRepository;

@ExtendWith(MockitoExtension.class)
class OfferServiceTest {

    private static final String EMAIL = "user@test.com";

    @Mock
    private OfferRepository offerRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserEntity user;
    @Mock
    private ProductEntity product;

    @InjectMocks
    private OfferServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(user.getEmail()).thenReturn(EMAIL);
    }

    private OfferEntity offer(long id) {
        OfferEntity o = OfferEntity.builder()
                .originalPrice(new BigDecimal("100"))
                .discountRate(new BigDecimal("20"))
                .finalPrice(new BigDecimal("80"))
                .user(user)
                .product(product)
                .build();
        ReflectionTestUtils.setField(o, "id", id); // у id нет сеттера (@Setter(NONE))
        o.setCoupon(UUID.randomUUID());
        return o;
    }

    private void userExistsWith(OfferEntity... offers) {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(user.getOffers()).thenReturn(List.of(offers));
    }

    // ---------- getAll ----------

    @Test
    void getAll_returnsMappedOffersOfUser() {
        userExistsWith(offer(1L), offer(2L));

        List<OfferDTOResponse> result = service.getAll(EMAIL);

        assertThat(result, hasSize(2));
        assertThat(result.stream().map(OfferDTOResponse::id).toList(), contains(1L, 2L));
    }

    @Test
    void getAll_returnsEmptyList_whenUserHasNoOffers() {
        userExistsWith();

        assertThat(service.getAll(EMAIL), is(empty()));
    }

    @Test
    void getAll_throws_whenUserNotFound() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        OfferException ex = assertThrows(OfferException.class, () -> service.getAll(EMAIL));

        assertThat(ex.getMessage(), is("User not found"));
        verifyNoInteractions(offerRepository);
    }

    // ---------- getById ----------

    @Test
    void getById_returnsOffer_whenBelongsToUser() {
        OfferEntity o = offer(1L);
        userExistsWith(o);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(o));

        OfferDTOResponse result = service.getById(1L, EMAIL);

        assertThat(result.id(), is(1L));
        assertThat(result.coupon(), is(o.getCoupon()));
    }

    @Test
    void getById_throws_whenOfferNotFound() {
        userExistsWith(offer(1L));
        when(offerRepository.findById(99L)).thenReturn(Optional.empty());

        OfferException ex = assertThrows(OfferException.class, () -> service.getById(99L, EMAIL));

        assertThat(ex.getMessage(), is("Offer not found with id 99"));
    }

    @Test
    void getById_throws_whenOfferBelongsToAnotherUser() {
        userExistsWith(offer(1L));
        when(offerRepository.findById(2L)).thenReturn(Optional.of(offer(2L)));

        OfferException ex = assertThrows(OfferException.class, () -> service.getById(2L, EMAIL));

        assertThat(ex.getMessage(), is("Offer is not belong to actual user"));
    }

    @Test
    void getById_throws_whenUserNotFound() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(OfferException.class, () -> service.getById(1L, EMAIL));
        verifyNoInteractions(offerRepository);
    }

    @Test
    void consume_marksOfferAsUsedAndSaves() {
        OfferEntity o = offer(1L);
        userExistsWith(o);
        when(offerRepository.findByCoupon(o.getCoupon())).thenReturn(Optional.of(o));

        OfferDTOResponse result = service.consume(o.getCoupon(), EMAIL);

        assertThat(o.isUsed(), is(true));
        assertThat(result.used(), is(true));
        verify(offerRepository).save(o);
    }

    @Test
    void consume_throws_whenCouponNotFound() {
        UUID coupon = UUID.randomUUID();
        userExistsWith(offer(1L));
        when(offerRepository.findByCoupon(coupon)).thenReturn(Optional.empty());

        OfferException ex = assertThrows(OfferException.class, () -> service.consume(coupon, EMAIL));

        assertThat(ex.getMessage(), is("Offer not found with coupon " + coupon));
        verify(offerRepository, never()).save(any());
    }

    @Test
    void consume_throws_whenOfferBelongsToAnotherUser() {
        OfferEntity other = offer(2L);
        userExistsWith(offer(1L));
        when(offerRepository.findByCoupon(other.getCoupon())).thenReturn(Optional.of(other));

        OfferException ex = assertThrows(OfferException.class,
                () -> service.consume(other.getCoupon(), EMAIL));

        assertThat(ex.getMessage(), is("Offer is not belong to actual user"));
        assertThat(other.isUsed(), is(false));
        verify(offerRepository, never()).save(any());
    }

    @Test
    void consume_throws_whenUserNotFound() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(OfferException.class, () -> service.consume(UUID.randomUUID(), EMAIL));
        verifyNoInteractions(offerRepository);
    }

    @Test
    void consume_marksOffer_whenIdOutsideLongCache() {
        OfferEntity o = offer(1000L);
        userExistsWith(o);
        when(offerRepository.findByCoupon(o.getCoupon())).thenReturn(Optional.of(o));

        service.consume(o.getCoupon(), EMAIL);

        assertThat(o.isUsed(), is(true));
    }
}