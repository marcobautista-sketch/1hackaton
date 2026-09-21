package com.tuckersoft.branchengine.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 60)
    private String displayName;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "user")
    private List<Playthrough> playthroughs = new ArrayList<>();
}
