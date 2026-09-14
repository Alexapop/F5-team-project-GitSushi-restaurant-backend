package dev.team1.users;

import org.springframework.stereotype.Component;

@Component
public class NoOpPasswordEncoder implements PasswordEncoderPort {

    @Override
    public String encode(String rawPassword) {
        return rawPassword;
    }

}
