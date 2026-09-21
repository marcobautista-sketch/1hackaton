package com.tuckersoft.branchengine.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "decisions")
@Getter
@Setter
@NoArgsConstructor
public class Decision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "playthrough_id", nullable = false)
    private Playthrough playthrough;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "node_id", nullable = false)
    private StoryNode node;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String rawInput;

    @Column(nullable = false)
    private String branchType;

    @Column(nullable = false)
    private String impactLevel;

    @Column(nullable = false)
    private String handlerUnit;

    @Column(nullable = false)
    private String outcomeCode;

    private String resolvedNodeCode;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "decision")
    private List<RealityLog> realityLogs = new ArrayList<>();
}
