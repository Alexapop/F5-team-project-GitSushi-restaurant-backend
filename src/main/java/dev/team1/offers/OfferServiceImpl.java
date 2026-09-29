package dev.team1.offers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dev.team1.contracts.IOfferService;
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getById'");
    }

    @Override
    public OfferDTOResponse consume(UUID coupon, String email) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'consume'");
    }

    private List<OfferEntity> getOffersOfCurrentUser(String email) {
        UserEntity user = userRepository.findByEmail(email)
            .orElseThrow(() -> new OfferException("User is not found"));

        return user.getOffers();
    }

    

}
