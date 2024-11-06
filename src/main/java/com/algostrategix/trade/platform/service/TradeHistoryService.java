package com.algostrategix.trade.platform.service;

import com.algostrategix.trade.platform.entity.TradeHistory;
import com.algostrategix.trade.platform.repository.TradeHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class TradeHistoryService {

    @Autowired
    TradeHistoryRepository tradeHistoryRepository;

    @Cacheable(value = "tradeHistory", key = "#page + '-' + #size")
    public Page<TradeHistory> getAllTrades(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return tradeHistoryRepository.findAll(pageable);
    }

    @Cacheable(value = "tradeHistoryBySymbol", key = "#symbol + '-' + #page + '-' + #size")
    public Page<TradeHistory> getTradesBySymbol(String symbol, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return tradeHistoryRepository.findByTickerSymbol(symbol, pageable);
    }
}