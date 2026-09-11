package dev.team1.users;

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

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoderPort passwordEncoderPort;

    @InjectMocks
    private UserService userService;

    private UserEntity validUser;

    @BeforeEach
    void setUp() {
        validUser = new UserEntity();
        validUser.setFirstName("Ahmet");
        validUser.setLastName("Yılmaz");
        validUser.setEmail("ahmet@example.com");
        validUser.setPassword("secret123");
        validUser.setAddress("Calle Mayor 5");
        validUser.setPostalCode("28001");
        validUser.setCity("Madrid");
    }

    @Test
    void registerUser_withValidData_savesAndReturnsUser() {
        when(userRepository.existsByEmail(validUser.getEmail())).thenReturn(false);
        when(passwordEncoderPort.encode(validUser.getPassword())).thenReturn("encoded-secret123");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserEntity result = userService.registerUser(validUser);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("ahmet@example.com");
        assertThat(result.getPassword()).isEqualTo("encoded-secret123");
        verify(userRepository).save(validUser);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void registerUser_withBlankFirstName_throwsException(String blankValue) {
        validUser.setFirstName(blankValue);

        assertThatThrownBy(() -> userService.registerUser(validUser))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("El nombre es obligatorio");

        verifyNoInteractions(userRepository, passwordEncoderPort);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void registerUser_withBlankLastName_throwsException(String blankValue) {
        validUser.setLastName(blankValue);

        assertThatThrownBy(() -> userService.registerUser(validUser))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Los apellidos son obligatorios");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void registerUser_withBlankEmail_throwsException(String blankValue) {
        validUser.setEmail(blankValue);

        assertThatThrownBy(() -> userService.registerUser(validUser))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("El email es obligatorio");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void registerUser_withBlankAddress_throwsException(String blankValue) {
        validUser.setAddress(blankValue);

        assertThatThrownBy(() -> userService.registerUser(validUser))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("La dirección es obligatoria");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void registerUser_withBlankPostalCode_throwsException(String blankValue) {
        validUser.setPostalCode(blankValue);

        assertThatThrownBy(() -> userService.registerUser(validUser))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("El código postal es obligatorio");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void registerUser_withBlankCity_throwsException(String blankValue) {
        validUser.setCity(blankValue);

        assertThatThrownBy(() -> userService.registerUser(validUser))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("La ciudad es obligatoria");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void registerUser_withBlankPassword_throwsException(String blankValue) {
        validUser.setPassword(blankValue);

        assertThatThrownBy(() -> userService.registerUser(validUser))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("La contraseña es obligatoria");
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-an-email", "missing-at-sign.com", "no-domain@", "@no-local-part.com", "spaces in@email.com"})
    void registerUser_withInvalidEmailFormat_throwsException(String invalidEmail) {
        validUser.setEmail(invalidEmail);

        assertThatThrownBy(() -> userService.registerUser(validUser))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("El formato del email no es válido");
    }

    @Test
    void registerUser_withAlreadyRegisteredEmail_throwsException() {
        when(userRepository.existsByEmail(validUser.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(validUser))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Ya existe una cuenta con este email");

        verify(userRepository, never()).save(any());
    }

}