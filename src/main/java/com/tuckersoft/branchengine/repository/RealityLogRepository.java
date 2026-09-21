package com.tuckersoft.branchengine.repository;

import com.tuckersoft.branchengine.entity.RealityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RealityLogRepository extends JpaRepository<RealityLog, Long> {
    List<RealityLog> findAllByDecision_IdOrderByCreatedAtAsc(Long decisionId);
}
