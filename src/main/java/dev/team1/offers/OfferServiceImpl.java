package dev.team1.offers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dev.team1.contracts.IOfferService;
import dev.team1.mappers.OfferMapper;
import dev.team1.offers.dtos.OfferDTOResponse;
import dev.team1.users.UserEntity;
import dev.team1.users.UserRepository;
import lombok.RequiredArgsConstructor;
import dev.team1.offers.exceptions.OfferException;

@Service 
@RequiredArgsConstructor 
public class OfferServiceImpl implements IOfferService {

    private final OfferRepository offerRepository;
    private final UserRepository userRepository;

    @Override
    public List<OfferDTOResponse> getAll(String email) {
        List<OfferEntity> offerEntities = getOffersOfCurrentUser(email);
        return offerEntities.stream()
            .map(OfferMapper::toDTO)
            .toList();
    }

    @Override
    public OfferDTOResponse getById(Long id, String email) {
        List<OfferEntity> offerEntities = getOffersOfCurrentUser(email);

        OfferEntity offer = offerRepository.findById(id)
            .orElseThrow(() -> new OfferException("Offer not found with id " + id));

        if (!offerEntities.stream().anyMatch(o -> o.getId() == id)) {
            throw new OfferException("Offer is not belong to actual user");
        }
        return OfferMapper.toDTO(offer);
    }

    @Override
    public OfferDTOResponse consume(UUID coupon, String email) {
        List<OfferEntity> offerEntities = getOffersOfCurrentUser(email);

        OfferEntity offer = offerRepository.findByCoupon(coupon)
            .orElseThrow(() -> new OfferException("Offer not found with coupon " + coupon));

        if (!offerEntities.stream().anyMatch(o -> o.getId() == offer.getId())) {
            throw new OfferException("Offer is not belong to actual user");
        }
        offer.setUsed(true);
        offerRepository.save(offer);

        return OfferMapper.toDTO(offer);
    }

    private List<OfferEntity> getOffersOfCurrentUser(String email) {
        UserEntity user = userRepository.findByEmail(email)
            .orElseThrow(() -> new OfferException("User not found"));

        return user.getOffers();
    }
    
}
