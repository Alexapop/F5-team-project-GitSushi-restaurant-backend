package dev.team1.users;

import dev.team1.contracts.IUserService;
import dev.team1.mappers.UserMapper;
import dev.team1.offers.OfferEntity;
import dev.team1.products.ProductEntity;
import dev.team1.products.ProductRepository;
import dev.team1.products.exceptions.ProductExceptionNotFound;
import dev.team1.roles.RoleEntity;
import dev.team1.roles.RoleRepository;
import dev.team1.users.dtos.UserPatchRequestDTO;
import dev.team1.users.dtos.UserRequestDTO;
import dev.team1.users.dtos.UserResponseDTO;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor 
public class UserServiceImpl implements IUserService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");


    @Override 
    @Transactional 
    public UserResponseDTO store(UserRequestDTO requestDTO) {
        validateRequiredFields(requestDTO);
        validateEmailFormat(requestDTO.getEmail());
        validateEmailNotTaken(requestDTO.getEmail());

        UserEntity newUser = UserMapper.toEntity(requestDTO);
        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
        
        RoleEntity roleCustomer = roleRepository.findByName("ROLE_CUSTOMER")
            .orElseGet(() -> {
                RoleEntity role = new RoleEntity();
                role.setName("ROLE_CUSTOMER");
                roleRepository.save(role);
                return role;
            });
        newUser.getRoles().add(roleCustomer);


        // Autpmatically dding offers to a new user
        ProductEntity product1 = getProductForOffer("Salmon.js");
        ProductEntity product2 = getProductForOffer("Shrimp.java");
        
        OfferEntity offer1 = createOffer(product1, newUser, BigDecimal.valueOf(50));
        OfferEntity offer2 = createOffer(product2, newUser, BigDecimal.valueOf(50));
        newUser.setOffers(List.of(offer1, offer2));

        UserEntity savedUser = userRepository.save(newUser);
        
        return UserMapper.toDTO(savedUser);
    }

    @Override 
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> getAll(Pageable pageable) {
        Page<UserEntity> pageEntity = userRepository.findAll(pageable);

        return pageEntity.map(UserMapper::toDTO);
    }

    @Override 
    @Transactional 
    public UserResponseDTO update(UUID id, UserPatchRequestDTO dto) {

    }


    private void validateRequiredFields(UserRequestDTO dto) {
        if (isBlank(dto.getFirstName())) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (isBlank(dto.getLastName())) {
            throw new IllegalArgumentException("Los apellidos son obligatorios");
        }
        if (isBlank(dto.getEmail())) {
            throw new IllegalArgumentException("El email es obligatorio");
        }
        if (isBlank(dto.getAddress())) {
            throw new IllegalArgumentException("La dirección es obligatoria");
        }
        if (isBlank(dto.getPostalCode())) {
            throw new IllegalArgumentException("El código postal es obligatorio");
        }
        if (isBlank(dto.getCity())) {
            throw new IllegalArgumentException("La ciudad es obligatoria");
        }
        if (isBlank(dto.getPassword())) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
    }

    private void validateEmailFormat(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("El formato del email no es válido");
        }
    }

    private void validateEmailNotTaken(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Ya existe una cuenta con este email");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private ProductEntity getProductForOffer(String name) {
        return productRepository.findByName(name)
            .orElseThrow(() -> new ProductExceptionNotFound("Product " + name + " not found"));
    }

    private OfferEntity createOffer(ProductEntity product, UserEntity user, BigDecimal discountRate) {
        return OfferEntity.builder()
            .product(product)
            .originalPrice(product.getPrice())
            .discountRate(discountRate)
            .user(user)
            .build();
    }

}