package dev.team1.users;

import dev.team1.roles.RoleEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@NoArgsConstructor 
@Data 
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    // TODO (Sprint 2): will be hashed with BCryptPasswordEncoder once security work starts.
    // For now stored as plain text via a no-op encoder (see PasswordEncoderPort).
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String address;

    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    @Column(nullable = false)
    private String city;

    @ManyToMany
    @JoinTable(
        name = "users_roles",
        joinColumns = @JoinColumn(name = "id_user"),
        inverseJoinColumns = @JoinColumn(name = "id_role")
    )
    private Set<RoleEntity> roles = new HashSet<>();
}