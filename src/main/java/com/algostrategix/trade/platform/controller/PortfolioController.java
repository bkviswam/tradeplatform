package com.algostrategix.trade.platform.controller;

import com.algostrategix.trade.platform.entity.Portfolio;
import com.algostrategix.trade.platform.service.PortfolioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    @Autowired
    private PortfolioService portfolioService;

    @GetMapping("/realtime")
    public List<Portfolio> getRealtimePortfolio() {
        return portfolioService.getAllPortfolios();
    }
}
