package dev.team1.users;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UsersController {

    private final UsersService usersService;

    public UsersController(UsersService usersService) {
        this.usersService = usersService;
    }

    @PostMapping("/usuarios")
    public ResponseEntity<UserEntity> createUser(@RequestBody UserEntity newUser) {
        UserEntity savedUser = usersService.registerUser(newUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

}
