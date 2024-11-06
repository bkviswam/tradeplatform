package com.algostrategix.trade.platform.service;

import com.algostrategix.trade.platform.entity.Portfolio;
import com.algostrategix.trade.platform.entity.Ticker;
import com.algostrategix.trade.platform.repository.PortfolioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PortfolioService {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private AlpacaService alpacaService;

    public void updatePortfolioForTicker(Ticker ticker) throws Exception {
        double currentPrice = alpacaService.getMarketPrice(ticker.getSymbol());

        Portfolio portfolio = portfolioRepository.findByTicker(ticker)
                .orElseGet(() -> createNewPortfolioEntry(ticker, currentPrice));

        portfolio.setMarketPrice(currentPrice);
        portfolio.calculateDerivedFields();

        portfolioRepository.save(portfolio);  // Save if you need to persist any updated fields
    }

    private Portfolio createNewPortfolioEntry(Ticker ticker, double initialPrice) {
        Portfolio newPortfolio = new Portfolio();
        newPortfolio.setTicker(ticker);
        newPortfolio.setQuantity(0);       // Start with 0 quantity if no trades have occurred
        newPortfolio.setAveragePrice(initialPrice);
        newPortfolio.setMarketPrice(initialPrice);

        newPortfolio.calculateDerivedFields();
        return portfolioRepository.save(newPortfolio);
    }

    public List<Portfolio> getAllPortfolios() {
        List<Portfolio> portfolios = portfolioRepository.findAll();
        portfolios.forEach(Portfolio::calculateDerivedFields);
        return portfolios;
    }
}
