package com.algostrategix.trade.platform.repository;

import com.algostrategix.trade.platform.entity.Portfolio;
import com.algostrategix.trade.platform.entity.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    Optional<Portfolio> findByTicker(Ticker ticker);
    List<Portfolio> findByTimestampAfter(LocalDateTime startOfDay);
}

