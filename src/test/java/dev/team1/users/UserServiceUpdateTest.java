package dev.team1.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import dev.team1.products.ProductRepository;
import dev.team1.roles.RoleEntity;
import dev.team1.roles.RoleRepository;
import dev.team1.users.dtos.UserPatchRequestDTO;
import dev.team1.users.dtos.UserResponseDTO;

@ExtendWith(MockitoExtension.class)
class UserServiceUpdateTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private final UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private UserEntity user;

    @BeforeEach
    void setUp() {
        RoleEntity cook = new RoleEntity();
        cook.setName("ROLE_COOK");

        user = new UserEntity();
        user.setFirstName("Kenji");
        user.setLastName("Sato");
        user.setEmail("cook@gitsushi.com");
        user.setActive(true);
        user.getRoles().add(cook);
    }

    @Test
    void update_withOnlyActive_keepsRoleAndDoesNotLookUpRoles() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UserPatchRequestDTO dto = new UserPatchRequestDTO(null, null, null, null, null, null, null, false);

        UserResponseDTO response = userService.update(userId, dto);

        assertThat(response.active()).isFalse();
        assertThat(response.roles()).containsExactly("ROLE_COOK");
        verify(roleRepository, never()).findByName(anyString());
    }

    @Test
    void update_withRole_changesRole() {
        RoleEntity admin = new RoleEntity();
        admin.setName("ROLE_ADMIN");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(admin));
        UserPatchRequestDTO dto = new UserPatchRequestDTO(null, null, null, null, null, null, "ADMIN", null);

        UserResponseDTO response = userService.update(userId, dto);

        assertThat(response.roles()).containsExactly("ROLE_ADMIN");
        assertThat(response.active()).isTrue();
    }

    @Test
    void update_withUnknownUser_throwsNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        UserPatchRequestDTO dto = new UserPatchRequestDTO(null, null, null, null, null, null, null, false);

        assertThatThrownBy(() -> userService.update(userId, dto))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("User not found");

        verify(userRepository, never()).save(any(UserEntity.class));
    }
}