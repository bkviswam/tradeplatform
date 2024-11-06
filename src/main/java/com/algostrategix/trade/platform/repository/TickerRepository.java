package com.algostrategix.trade.platform.repository;

import com.algostrategix.trade.platform.entity.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TickerRepository extends JpaRepository<Ticker, Long> {
    List<Ticker> findAllByActiveTrue();
}

