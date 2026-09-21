package com.tuckersoft.branchengine.repository;

import com.tuckersoft.branchengine.entity.StoryNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoryNodeRepository extends JpaRepository<StoryNode, Long> {
    Optional<StoryNode> findByNodeCode(String nodeCode);
    boolean existsByNodeCode(String nodeCode);
}
