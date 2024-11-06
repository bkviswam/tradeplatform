package com.algostrategix.trade.platform.repository;

import com.algostrategix.trade.platform.entity.Ticker;
import com.algostrategix.trade.platform.entity.TradeHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TradeHistoryRepository extends JpaRepository<TradeHistory, Long> {
    List<TradeHistory> findByTickerAndTradeTimestampBetween(Ticker ticker, LocalDateTime start, LocalDateTime end);
    TradeHistory findTopByTickerOrderByTradeTimestampDesc(Ticker ticker);
    List<TradeHistory> findByTickerSymbol(String symbol);
    Page<TradeHistory> findAll(Pageable pageable);
    Page<TradeHistory> findByTickerSymbol(String symbol, Pageable pageable);
}

