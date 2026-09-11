package dev.team1.users;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class UsersService {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");

    private final UsersRepository usersRepository;
    private final PasswordEncoderPort passwordEncoderPort;

    public UsersService(UsersRepository usersRepository, PasswordEncoderPort passwordEncoderPort) {
        this.usersRepository = usersRepository;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    public UserEntity registerUser(UserEntity newUser) {
        validateRequiredFields(newUser);
        validateEmailFormat(newUser.getEmail());
        validateEmailNotTaken(newUser.getEmail());

        newUser.setPassword(passwordEncoderPort.encode(newUser.getPassword()));

        return usersRepository.save(newUser);
    }

    private void validateRequiredFields(UserEntity user) {
        if (isBlank(user.getFirstName())) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (isBlank(user.getLastName())) {
            throw new IllegalArgumentException("Los apellidos son obligatorios");
        }
        if (isBlank(user.getEmail())) {
            throw new IllegalArgumentException("El email es obligatorio");
        }
        if (isBlank(user.getAddress())) {
            throw new IllegalArgumentException("La dirección es obligatoria");
        }
        if (isBlank(user.getPostalCode())) {
            throw new IllegalArgumentException("El código postal es obligatorio");
        }
        if (isBlank(user.getCity())) {
            throw new IllegalArgumentException("La ciudad es obligatoria");
        }
        if (isBlank(user.getPassword())) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
    }

    private void validateEmailFormat(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("El formato del email no es válido");
        }
    }

    private void validateEmailNotTaken(String email) {
        if (usersRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Ya existe una cuenta con este email");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}