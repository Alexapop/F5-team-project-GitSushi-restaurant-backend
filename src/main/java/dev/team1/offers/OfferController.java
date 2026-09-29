package dev.team1.offers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.team1.auth.CustomUserDetails;
import dev.team1.contracts.IOfferService;
import dev.team1.offers.dtos.OfferDTOResponse;
import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping(path = "${api-endpoint}/offers")
@RequiredArgsConstructor 
public class OfferController {

    private final IOfferService offerService;

    @GetMapping("")
    public ResponseEntity<List<OfferDTOResponse>> index(@AuthenticationPrincipal CustomUserDetails userPrincipal) {
        return ResponseEntity.ok(offerService.getAll(userPrincipal.getUsername()));
    }

    @PatchMapping("consume/{coupon}")
    public ResponseEntity<OfferDTOResponse> consume(@AuthenticationPrincipal CustomUserDetails userPrincipal,
            @PathVariable UUID coupon) {
        return ResponseEntity.ok(offerService.consume(coupon, userPrincipal.getUsername()));
    }

    @GetMapping("{id}")
    public ResponseEntity<OfferDTOResponse> getById(@AuthenticationPrincipal CustomUserDetails userPrincipal,
            @PathVariable Long id) {
        return ResponseEntity.ok(offerService.getById(id, userPrincipal.getUsername()));
    }
}
