package com.tuckersoft.branchengine.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "story_nodes")
@Getter
@Setter
@NoArgsConstructor
public class StoryNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String nodeCode;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sceneText;

    @Column(nullable = false)
    private Integer branchCapacity;

    @Column(nullable = false)
    private Integer currentBranches = 0;

    private String primaryBranchCode;

    private String glitchBranchCode;

    @Column(nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "currentNode")
    private List<Playthrough> playthroughs = new ArrayList<>();

    @OneToMany(mappedBy = "node")
    private List<Decision> decisions = new ArrayList<>();
}
