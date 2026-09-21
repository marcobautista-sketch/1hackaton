package com.tuckersoft.branchengine.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "playthroughs")
@Getter
@Setter
@NoArgsConstructor
public class Playthrough {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String playerTag;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String startNodeCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "current_node_id", nullable = false)
    private StoryNode currentNode;

    @Column(nullable = false)
    private Integer lucidity = 100;

    @Column(nullable = false)
    private Integer controlLevel = 0;

    @Column(nullable = false)
    private String status = "ACTIVA";

    private String endingCode;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "playthrough")
    private List<Decision> decisions = new ArrayList<>();
}
