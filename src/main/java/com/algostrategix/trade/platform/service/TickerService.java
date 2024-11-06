package com.algostrategix.trade.platform.service;

import com.algostrategix.trade.platform.entity.Ticker;
import com.algostrategix.trade.platform.repository.TickerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TickerService {

    @Autowired
    private TickerRepository tickerRepository;

    public Ticker addTicker(Ticker ticker) {
        return tickerRepository.save(ticker);
    }

    public List<Ticker> getAllActiveTickers() {
        return tickerRepository.findAllByActiveTrue();
    }
}
