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

@Service 
@RequiredArgsConstructor 
public class OfferServiceImpl implements IOfferService {

    private final OfferRepository offerRepository;
    private final UserRepository userRepository;

    @Override
    public List<OfferDTOResponse> getAll(String email) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAll'");
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
            .orElseThrow(() -> OfferException("User is not found"));

        
    }

    

}
