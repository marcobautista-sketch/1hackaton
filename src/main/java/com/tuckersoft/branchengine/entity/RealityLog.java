package com.tuckersoft.branchengine.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "reality_logs")
@Getter
@Setter
@NoArgsConstructor
public class RealityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "decision_id", nullable = false)
    private Decision decision;

    @Column(nullable = false)
    private String recipientEmail;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String logStatus;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private Instant sentAt;

    @Column(nullable = false)
    private Instant createdAt;
}
