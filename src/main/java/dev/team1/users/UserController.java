package dev.team1.users;

import dev.team1.users.dtos.UserRequestDTO;
import dev.team1.users.dtos.UserResponseDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "${api-endpoint}/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("") 
    public ResponseEntity<Page<UserResponseDTO> index(Pageable pageable) {
        return ResponseEntity.ok(userService.getAll(pageable));
    }

    @PostMapping("")
    public ResponseEntity<UserResponseDTO> createUser(@RequestBody UserRequestDTO requestDTO) {
        UserResponseDTO savedUser = userService.registerUser(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleInvalidUserData(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

}
