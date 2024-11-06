package com.algostrategix.trade.platform.repository;

import com.algostrategix.trade.platform.entity.Portfolio;
import com.algostrategix.trade.platform.entity.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    Portfolio findByTicker(Ticker ticker);
}

