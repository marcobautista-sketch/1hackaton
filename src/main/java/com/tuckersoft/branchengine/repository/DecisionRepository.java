package com.tuckersoft.branchengine.repository;

import com.tuckersoft.branchengine.entity.Decision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface DecisionRepository extends JpaRepository<Decision, Long>, JpaSpecificationExecutor<Decision> {
    List<Decision> findAllByPlaythrough_IdAndResolvedNodeCodeIsNotNullOrderByCreatedAtAsc(Long playthroughId);
}
