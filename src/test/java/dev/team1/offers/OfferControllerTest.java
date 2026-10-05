package dev.team1.offers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import dev.team1.auth.CustomUserDetails;
import dev.team1.contracts.IOfferService;
import dev.team1.offers.dtos.OfferDTOResponse;

@ExtendWith(MockitoExtension.class)
class OfferControllerTest {

    private static final String EMAIL = "user@test.com";

    @Mock
    private IOfferService offerService;
    @Mock
    private CustomUserDetails principal;
    @Mock
    private OfferDTOResponse dto;

    @InjectMocks
    private OfferController controller;

    @BeforeEach
    void setUp() {
        when(principal.getUsername()).thenReturn(EMAIL);
    }

    @Test
    void index_returnsOkWithOffersOfPrincipal() {
        List<OfferDTOResponse> offers = List.of(dto);
        when(offerService.getAll(EMAIL)).thenReturn(offers);

        ResponseEntity<List<OfferDTOResponse>> response = controller.index(principal);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody(), is(sameInstance(offers)));
    }

    @Test
    void consume_returnsOkAndDelegatesCouponAndEmail() {
        UUID coupon = UUID.randomUUID();
        when(offerService.consume(coupon, EMAIL)).thenReturn(dto);

        ResponseEntity<OfferDTOResponse> response = controller.consume(principal, coupon);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody(), is(sameInstance(dto)));
        verify(offerService).consume(coupon, EMAIL);
    }

    @Test
    void getById_returnsOkAndDelegatesIdAndEmail() {
        when(offerService.getById(5L, EMAIL)).thenReturn(dto);

        ResponseEntity<OfferDTOResponse> response = controller.getById(principal, 5L);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody(), is(sameInstance(dto)));
    }
}