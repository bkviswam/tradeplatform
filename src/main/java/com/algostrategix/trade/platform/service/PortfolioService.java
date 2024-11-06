package com.algostrategix.trade.platform.service;

import com.algostrategix.trade.platform.entity.Portfolio;
import com.algostrategix.trade.platform.entity.Ticker;
import com.algostrategix.trade.platform.repository.PortfolioRepository;
import com.algostrategix.trade.platform.repository.TickerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PortfolioService {

    @Autowired
    private AlpacaService alpacaService;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private TickerRepository tickerRepository;

    @Cacheable(value = "portfolioToday", key = "'portfolio_' + T(java.time.LocalDate).now()")
    public List<Portfolio> getTodayPortfolio() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        return portfolioRepository.findByTimestampAfter(startOfDay);
    }

    @Scheduled(fixedRate = 60000) // Update every minute
    public void updatePortfolio() {
        List<Ticker> tickers = getActiveTickers();

        for (Ticker ticker : tickers) {
            updatePortfolioForTicker(ticker);
        }
    }

    public void updatePortfolioForTicker(Ticker ticker) {
        try {
            double currentPrice = alpacaService.getMarketPrice(ticker.getSymbol());
            Portfolio portfolio = portfolioRepository.findByTicker(ticker)
                    .orElseGet(() -> createNewPortfolioEntry(ticker, currentPrice));

            int quantity = portfolio.getQuantity();
            double totalValue = currentPrice * quantity;
            double unrealizedPnl = (currentPrice - portfolio.getAveragePrice()) * quantity;

            portfolio.setTotalValue(totalValue);
            portfolio.setUnrealizedPnl(unrealizedPnl);
            portfolio.setTimestamp(LocalDateTime.now());

            portfolioRepository.save(portfolio);
        } catch (Exception e) {
            // Handle API errors, log, etc.
        }
    }

    private Portfolio createNewPortfolioEntry(Ticker ticker, double currentPrice) {
        Portfolio portfolio = new Portfolio();
        portfolio.setTicker(ticker);
        portfolio.setQuantity(0);
        portfolio.setAveragePrice(currentPrice);
        portfolio.setTotalValue(0.0);
        portfolio.setUnrealizedPnl(0.0);
        portfolio.setRealizedPnl(0.0);
        portfolio.setTimestamp(LocalDateTime.now());
        return portfolio;
    }
    public List<Ticker> getActiveTickers() {
        return tickerRepository.findAllByActiveTrue();  // Assuming this repository method is defined
    }
}
