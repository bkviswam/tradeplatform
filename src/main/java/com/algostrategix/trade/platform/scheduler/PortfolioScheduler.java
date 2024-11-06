package com.algostrategix.trade.platform.scheduler;

import com.algostrategix.trade.platform.entity.Ticker;
import com.algostrategix.trade.platform.repository.TickerRepository;
import com.algostrategix.trade.platform.service.PortfolioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class PortfolioScheduler {

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private TickerRepository tickerRepository;

    @Scheduled(fixedRate = 60000) // Run every minute
    public void updateAllPortfolios() {
        List<Ticker> tickers = tickerRepository.findAllByActiveTrue();
        tickers.forEach(ticker -> {
            try {
                portfolioService.updatePortfolioForTicker(ticker);
            } catch (Exception e) {
                log.error("Error updating portfolio for ticker {}: {}", ticker.getSymbol(), e.getMessage());
            }
        });
    }
}
