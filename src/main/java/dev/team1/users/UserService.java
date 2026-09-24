package dev.team1.users;

import dev.team1.mappers.UserMapper;
import dev.team1.roles.RoleEntity;
import dev.team1.roles.RoleRepository;
import dev.team1.security.PasswordEncoderPort;
import dev.team1.users.dtos.UserRequestDTO;
import dev.team1.users.dtos.UserResponseDTO;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class UserService {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoderPort;
    private final RoleRepository roleRepository;

    public UserService(UserRepository userRepository, PasswordEncoderPort passwordEncoderPort, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.passwordEncoderPort = passwordEncoderPort;
        this.roleRepository = roleRepository;
    }

    public UserResponseDTO registerUser(UserRequestDTO requestDTO) {
        validateRequiredFields(requestDTO);
        validateEmailFormat(requestDTO.getEmail());
        validateEmailNotTaken(requestDTO.getEmail());

        UserEntity newUser = UserMapper.toEntity(requestDTO);
        newUser.setPassword(passwordEncoderPort.encode(newUser.getPassword()));
        
        RoleEntity roleCustomer = roleRepository.findByName("ROLE_CUSTOMER")
            .orElseGet(() -> {
                RoleEntity role = new RoleEntity();
                role.setName("ROLE_CUSTOMER");
                roleRepository.save(role);
                return role;
            });
        newUser.getRoles().add(roleCustomer);

        UserEntity savedUser = userRepository.save(newUser);
        return UserMapper.toDTO(savedUser);
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

}