package com.algostrategix.trade.platform.repository;

import com.algostrategix.trade.platform.entity.MartingaleConfig;
import com.algostrategix.trade.platform.enums.EnvironmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MartingaleConfigRepository extends JpaRepository<MartingaleConfig, Long> {
    // Fetch MartingaleConfig based on the environment type
    Optional<MartingaleConfig> findByEnvironmentType(EnvironmentType environmentType);
}