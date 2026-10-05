package dev.team1.contracts;

import java.util.List;
import java.util.UUID;

import dev.team1.offers.dtos.OfferDTOResponse;

public interface IOfferService {
    
    List<OfferDTOResponse> getAll(String email);
    OfferDTOResponse getById(Long id, String email);
    OfferDTOResponse consume(UUID coupon, String email);
    
}
