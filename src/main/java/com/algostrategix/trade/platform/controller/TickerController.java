package com.algostrategix.trade.platform.controller;

import com.algostrategix.trade.platform.entity.Ticker;
import com.algostrategix.trade.platform.service.TickerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickers")
public class TickerController {

    @Autowired
    private TickerService tickerService;

    @PostMapping
    public ResponseEntity<Ticker> addTicker(@RequestBody Ticker ticker) {
        return ResponseEntity.ok(tickerService.addTicker(ticker));
    }

    @GetMapping
    public ResponseEntity<List<Ticker>> getAllActiveTickers() {
        return ResponseEntity.ok(tickerService.getAllActiveTickers());
    }
}
