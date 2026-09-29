package dev.team1.users;

import dev.team1.users.dtos.UserRequestDTO;
import dev.team1.users.dtos.UserResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

class UserControllerTest {

    private MockMvc mockMvc;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        UserController controller = new UserController(userService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .addPlaceholderValue("api-endpoint", "api/v1")
            .build();
    }

    private String validUserJson() {
        return """
            {
              "firstName": "Ahmet",
              "lastName": "Yılmaz",
              "email": "ahmet@example.com",
              "password": "secret123",
              "address": "Calle Mayor 5",
              "postalCode": "28001",
              "city": "Madrid"
            }
            """;
    }

    @Test
    void createUser_withValidData_returns201AndCreatedUser() throws Exception {
        
        UUID mockId = UUID.randomUUID();
        UserResponseDTO savedUser = UserResponseDTO.builder()
            .id(mockId)
            .email("ahmet@example.com")
            .build();

        when(userService.registerUser(any(UserRequestDTO.class))).thenReturn(savedUser);

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validUserJson()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(mockId.toString()))
            .andExpect(jsonPath("$.email").value("ahmet@example.com"));
    }

    @Test
    void createUser_withBlankFirstName_returns400() throws Exception {
        when(userService.registerUser(any(UserRequestDTO.class)))
            .thenThrow(new IllegalArgumentException("El nombre es obligatorio"));

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validUserJson()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_withDuplicateEmail_returns400() throws Exception {
        when(userService.registerUser(any(UserRequestDTO.class)))
            .thenThrow(new IllegalArgumentException("Ya existe una cuenta con este email"));

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validUserJson()))
            .andExpect(status().isBadRequest());
    }

}