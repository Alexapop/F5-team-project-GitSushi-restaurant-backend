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
import dev.team1.users.dtos.UserProfileRequestDTO;
import dev.team1.users.dtos.UserRequestDTO;
import dev.team1.users.dtos.UserResponseDTO;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
        Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");


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
    public UserResponseDTO update(UUID id, UserPatchRequestDTO requestDto) {
        UserEntity originalEntity = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Solo se busca el rol si viene en el body: {"active": false} no lo trae.
        RoleEntity newRole = isBlank(requestDto.role()) ? null : findNewRole(requestDto.role());
        UserEntity updatedEntity = UserMapper.updateEntity(originalEntity, requestDto, newRole);
        UserEntity savedEntity = userRepository.save(updatedEntity);
        return UserMapper.toDTO(savedEntity);
    }

    @Override
    @Transactional
    public UserResponseDTO updateProfile(
        UUID id,
        UUID authenticatedUserId,
        UserProfileRequestDTO requestDTO
    ) {
        if (authenticatedUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión");
        }

        if (!authenticatedUserId.equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes editar tu propio perfil");
        }

        UserEntity user = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        validateProfileFields(
            requestDTO.firstName(),
            requestDTO.lastName(),
            requestDTO.email(),
            requestDTO.address(),
            requestDTO.postalCode(),
            requestDTO.city()
        );

        String email = requestDTO.email().trim();
        validateEmailFormat(email);

        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con este email");
        }

        user.setFirstName(requestDTO.firstName().trim());
        user.setLastName(requestDTO.lastName().trim());
        user.setEmail(email);
        user.setAddress(requestDTO.address().trim());
        user.setPostalCode(requestDTO.postalCode().trim());
        user.setCity(requestDTO.city().trim());

        UserEntity savedUser = userRepository.saveAndFlush(user);
        return UserMapper.toDTO(savedUser);
    }

    @Override 
    public void delete(UUID id) {
        userRepository.deleteById(id);
    }

    private void validateRequiredFields(UserRequestDTO dto) {
        validateProfileFields(
            dto.getFirstName(),
            dto.getLastName(),
            dto.getEmail(),
            dto.getAddress(),
            dto.getPostalCode(),
            dto.getCity()
        );

        if (isBlank(dto.getPassword())) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
    }

    private void validateProfileFields(
        String firstName,
        String lastName,
        String email,
        String address,
        String postalCode,
        String city
    ) {
        if (isBlank(firstName)) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (isBlank(lastName)) {
            throw new IllegalArgumentException("Los apellidos son obligatorios");
        }
        if (isBlank(email)) {
            throw new IllegalArgumentException("El email es obligatorio");
        }
        if (isBlank(address)) {
            throw new IllegalArgumentException("La dirección es obligatoria");
        }
        if (isBlank(postalCode)) {
            throw new IllegalArgumentException("El código postal es obligatorio");
        }
        if (isBlank(city)) {
            throw new IllegalArgumentException("La ciudad es obligatoria");
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

    private RoleEntity findNewRole(String dtoName) {
        String name;
        if (!dtoName.startsWith("ROLE_")) {
            name = "ROLE_" + dtoName;
        } else {
            name = dtoName;
        }
        return roleRepository.findByName(name)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role '" + name + "' not found!"));
    }

}
