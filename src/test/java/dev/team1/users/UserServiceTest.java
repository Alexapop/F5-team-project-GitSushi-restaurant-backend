package dev.team1.users;

import dev.team1.mappers.UserMapper;
import dev.team1.users.dtos.UserRequestDTO;
import dev.team1.users.dtos.UserResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoderPort passwordEncoderPort;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private UserRequestDTO validRequest;
    private UserEntity mappedEntity;

    @BeforeEach
    void setUp() {
        validRequest = new UserRequestDTO();
        validRequest.setFirstName("Ahmet");
        validRequest.setLastName("Yılmaz");
        validRequest.setEmail("ahmet@example.com");
        validRequest.setPassword("secret123");
        validRequest.setAddress("Calle Mayor 5");
        validRequest.setPostalCode("28001");
        validRequest.setCity("Madrid");

        mappedEntity = new UserEntity();
        mappedEntity.setFirstName("Ahmet");
        mappedEntity.setLastName("Yılmaz");
        mappedEntity.setEmail("ahmet@example.com");
        mappedEntity.setPassword("secret123");
        mappedEntity.setAddress("Calle Mayor 5");
        mappedEntity.setPostalCode("28001");
        mappedEntity.setCity("Madrid");
    }

    @Test
    void registerUser_withValidData_savesAndReturnsUser() {
        UserEntity savedEntity = new UserEntity();
        savedEntity.setEmail("ahmet@example.com");
        savedEntity.setPassword("encoded-secret123");

        UUID mockId = UUID.randomUUID();
        UserResponseDTO expectedResponse = UserResponseDTO.builder()
            .id(mockId)
            .email("ahmet@example.com")
            .build();

        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(userMapper.toEntity(validRequest)).thenReturn(mappedEntity);
        when(passwordEncoderPort.encode("secret123")).thenReturn("encoded-secret123");
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedEntity);
        when(userMapper.toDTO(savedEntity)).thenReturn(expectedResponse);

        UserResponseDTO result = userService.registerUser(validRequest);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(mockId);
        assertThat(result.email()).isEqualTo("ahmet@example.com");
        verify(userRepository).save(mappedEntity);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void registerUser_withBlankFirstName_throwsException(String blankValue) {
        validRequest.setFirstName(blankValue);

        assertThatThrownBy(() -> userService.registerUser(validRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("El nombre es obligatorio");

        verifyNoInteractions(userRepository, passwordEncoderPort, userMapper);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void registerUser_withBlankLastName_throwsException(String blankValue) {
        validRequest.setLastName(blankValue);

        assertThatThrownBy(() -> userService.registerUser(validRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Los apellidos son obligatorios");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void registerUser_withBlankEmail_throwsException(String blankValue) {
        validRequest.setEmail(blankValue);

        assertThatThrownBy(() -> userService.registerUser(validRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("El email es obligatorio");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void registerUser_withBlankAddress_throwsException(String blankValue) {
        validRequest.setAddress(blankValue);

        assertThatThrownBy(() -> userService.registerUser(validRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("La dirección es obligatoria");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void registerUser_withBlankPostalCode_throwsException(String blankValue) {
        validRequest.setPostalCode(blankValue);

        assertThatThrownBy(() -> userService.registerUser(validRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("El código postal es obligatorio");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void registerUser_withBlankCity_throwsException(String blankValue) {
        validRequest.setCity(blankValue);

        assertThatThrownBy(() -> userService.registerUser(validRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("La ciudad es obligatoria");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void registerUser_withBlankPassword_throwsException(String blankValue) {
        validRequest.setPassword(blankValue);

        assertThatThrownBy(() -> userService.registerUser(validRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("La contraseña es obligatoria");
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-an-email", "missing-at-sign.com", "no-domain@", "@no-local-part.com", "spaces in@email.com"})
    void registerUser_withInvalidEmailFormat_throwsException(String invalidEmail) {
        validRequest.setEmail(invalidEmail);

        assertThatThrownBy(() -> userService.registerUser(validRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("El formato del email no es válido");
    }

    @Test
    void registerUser_withAlreadyRegisteredEmail_throwsException() {
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(validRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Ya existe una cuenta con este email");

        verify(userRepository, never()).save(any());
    }

}