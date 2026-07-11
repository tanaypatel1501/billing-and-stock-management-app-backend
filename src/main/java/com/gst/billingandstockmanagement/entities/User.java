package com.gst.billingandstockmanagement.entities;

import com.gst.billingandstockmanagement.enums.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "details")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    private String firstname;
    private String lastname;
    private String email;
    private String password;
    @Enumerated(EnumType.STRING)
    private UserRole userRole;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean emailVerified = false;
    private String verificationToken;
    private LocalDateTime verificationTokenExpiry;
    @Column(nullable = true)
    private String googleId;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Details details;
}