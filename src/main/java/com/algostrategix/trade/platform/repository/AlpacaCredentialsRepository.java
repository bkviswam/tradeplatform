package com.algostrategix.trade.platform.repository;

import com.algostrategix.trade.platform.entity.AlpacaCredentials;
import com.algostrategix.trade.platform.enums.EnvironmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AlpacaCredentialsRepository extends JpaRepository<AlpacaCredentials, Long> {
    Optional<AlpacaCredentials> findByEnvironmentType(EnvironmentType environmentType);
}

