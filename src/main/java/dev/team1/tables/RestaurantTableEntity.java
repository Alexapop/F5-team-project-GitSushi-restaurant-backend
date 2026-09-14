package dev.team1.tables;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "restaurant_tables")
@Getter
@Setter
@NoArgsConstructor
public class RestaurantTableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @NotNull
    @Positive
    @Column(name = "table_number", nullable = false, unique = true)
    private Integer tableNumber;

    @NotBlank
    @Column(name = "device_identifier", nullable = false, unique = true)
    private String deviceIdentifier;
}
