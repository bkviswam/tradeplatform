package com.algostrategix.trade.platform.controller;

import com.algostrategix.trade.platform.entity.TradeHistory;
import com.algostrategix.trade.platform.repository.TradeHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trades")
public class TradeHistoryController {

    @Autowired
    private TradeHistoryRepository tradeHistoryRepository;

    @GetMapping
    public Page<TradeHistory> getAllTrades(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return tradeHistoryRepository.findAll(pageable);
    }

    @GetMapping("/{symbol}")
    public Page<TradeHistory> getTradesBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return tradeHistoryRepository.findByTickerSymbol(symbol, pageable);
    }
}
