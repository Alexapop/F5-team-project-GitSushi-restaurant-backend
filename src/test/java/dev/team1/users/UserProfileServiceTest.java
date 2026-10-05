package dev.team1.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import dev.team1.products.ProductRepository;
import dev.team1.roles.RoleEntity;
import dev.team1.roles.RoleRepository;
import dev.team1.users.dtos.UserProfileRequestDTO;
import dev.team1.users.dtos.UserResponseDTO;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID userId;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        RoleEntity role = new RoleEntity();
        role.setName("ROLE_CUSTOMER");

        user = new UserEntity();
        user.setId(userId);
        user.setFirstName("Ana");
        user.setLastName("Pop");
        user.setEmail("ana@example.com");
        user.setPassword("existing-password-hash");
        user.setAddress("Calle Antigua 1");
        user.setPostalCode("28001");
        user.setCity("Madrid");
        user.setActive(true);
        user.getRoles().add(role);
    }

    @Test
    void updateProfile_savesDataAndPreservesProtectedFields() {
        when(userRepository.findById(userId))
            .thenReturn(Optional.of(user));

        when(userRepository.saveAndFlush(any(UserEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileRequestDTO request = new UserProfileRequestDTO(
            " Ana María ",
            " Pop ",
            "ana.new@example.com",
            " Calle Nueva 20 ",
            " 08001 ",
            " Barcelona "
        );

        UserResponseDTO result = userService.updateProfile(
            userId,
            userId,
            request
        );

        assertThat(result.firstName()).isEqualTo("Ana María");
        assertThat(result.lastName()).isEqualTo("Pop");
        assertThat(result.email()).isEqualTo("ana.new@example.com");
        assertThat(result.address()).isEqualTo("Calle Nueva 20");
        assertThat(result.postalCode()).isEqualTo("08001");
        assertThat(result.city()).isEqualTo("Barcelona");
        assertThat(result.profileCompletion()).isEqualTo(100);

        assertThat(user.getPassword())
            .isEqualTo("existing-password-hash");

        assertThat(result.roles()).containsExactly("ROLE_CUSTOMER");
        assertThat(result.active()).isTrue();

        verify(userRepository).saveAndFlush(user);
        verifyNoInteractions(passwordEncoder, roleRepository);
    }

    @ParameterizedTest
    @MethodSource("invalidRequiredFields")
    void updateProfile_rejectsMissingFieldsWithoutChangingUser(
        UserProfileRequestDTO request
    ) {
        when(userRepository.findById(userId))
            .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateProfile(
            userId,
            userId,
            request
        )).isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never())
            .saveAndFlush(any(UserEntity.class));

        assertThat(user.getFirstName()).isEqualTo("Ana");
        assertThat(user.getAddress()).isEqualTo("Calle Antigua 1");
        assertThat(user.getEmail()).isEqualTo("ana@example.com");
    }

    static Stream<Arguments> invalidRequiredFields() {
        Stream.Builder<Arguments> cases = Stream.builder();

        for (int index = 0; index < 6; index++) {
            for (String invalidValue : new String[] {null, "", "   "}) {
                String[] values = {
                    "Nombre nuevo",
                    "Pop",
                    "ana@example.com",
                    "Calle Nueva 20",
                    "28001",
                    "Madrid"
                };

                values[index] = invalidValue;

                cases.add(Arguments.of(new UserProfileRequestDTO(
                    values[0],
                    values[1],
                    values[2],
                    values[3],
                    values[4],
                    values[5]
                )));
            }
        }

        return cases.build();
    }

    @Test
    void updateProfile_allowsKeepingOwnEmail() {
        when(userRepository.findById(userId))
            .thenReturn(Optional.of(user));

        when(userRepository.saveAndFlush(any(UserEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDTO result = userService.updateProfile(
            userId,
            userId,
            validRequest()
        );

        assertThat(result.email()).isEqualTo("ana@example.com");

        verify(userRepository)
            .existsByEmailAndIdNot("ana@example.com", userId);
    }

    @Test
    void updateProfile_rejectsEmailUsedByAnotherAccount() {
        when(userRepository.findById(userId))
            .thenReturn(Optional.of(user));

        when(userRepository.existsByEmailAndIdNot(
            "ana@example.com",
            userId
        )).thenReturn(true);

        assertThatThrownBy(() -> userService.updateProfile(
            userId,
            userId,
            validRequest()
        ))
            .isInstanceOfSatisfying(
                ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT)
            );

        verify(userRepository, never())
            .saveAndFlush(any(UserEntity.class));
    }

    @Test
    void updateProfile_rejectsInvalidEmail() {
        when(userRepository.findById(userId))
            .thenReturn(Optional.of(user));

        UserProfileRequestDTO request = new UserProfileRequestDTO(
            "Ana",
            "Pop",
            "invalid-email",
            "Calle Mayor 10",
            "28001",
            "Madrid"
        );

        assertThatThrownBy(() -> userService.updateProfile(
            userId,
            userId,
            request
        )).isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never())
            .saveAndFlush(any(UserEntity.class));
    }

    @Test
    void updateProfile_rejectsAnotherUsersId() {
        UUID anotherUserId = UUID.randomUUID();

        assertThatThrownBy(() -> userService.updateProfile(
            anotherUserId,
            userId,
            validRequest()
        ))
            .isInstanceOfSatisfying(
                ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN)
            );

        verifyNoInteractions(userRepository);
    }

    @Test
    void updateProfile_rejectsMissingAuthentication() {
        assertThatThrownBy(() -> userService.updateProfile(
            userId,
            null,
            validRequest()
        ))
            .isInstanceOfSatisfying(
                ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED)
            );

        verifyNoInteractions(userRepository);
    }

    @Test
    void updateProfile_rejectsUnknownUser() {
        when(userRepository.findById(userId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateProfile(
            userId,
            userId,
            validRequest()
        ))
            .isInstanceOfSatisfying(
                ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND)
            );

        verify(userRepository, never())
            .saveAndFlush(any(UserEntity.class));
    }

    @Test
    void profileCompletion_countsOnlyFilledRequiredFields() {
        UserResponseDTO partial = UserResponseDTO.builder()
            .firstName("Ana")
            .lastName("Pop")
            .email("ana@example.com")
            .address(" ")
            .postalCode("")
            .city(null)
            .build();

        UserResponseDTO empty = UserResponseDTO.builder().build();

        assertThat(partial.profileCompletion()).isEqualTo(50);
        assertThat(empty.profileCompletion()).isZero();
    }

    private UserProfileRequestDTO validRequest() {
        return new UserProfileRequestDTO(
            "Ana",
            "Pop",
            "ana@example.com",
            "Calle Mayor 10",
            "28001",
            "Madrid"
        );
    }
}