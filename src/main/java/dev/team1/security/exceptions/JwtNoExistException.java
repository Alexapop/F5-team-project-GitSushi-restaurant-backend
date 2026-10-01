package dev.team1.security.exceptions;

public class JwtNoExistException extends RuntimeException {

    public JwtNoExistException(String message) {
        super(message);
    }

}
