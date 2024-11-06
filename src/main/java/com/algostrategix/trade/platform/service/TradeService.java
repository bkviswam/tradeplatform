package com.algostrategix.trade.platform.service;

import com.algostrategix.trade.platform.entity.MartingaleConfig;
import com.algostrategix.trade.platform.entity.Ticker;
import com.algostrategix.trade.platform.entity.TradeHistory;
import com.algostrategix.trade.platform.enums.EnvironmentType;
import com.algostrategix.trade.platform.enums.MarketSession;
import com.algostrategix.trade.platform.repository.MartingaleConfigRepository;
import com.algostrategix.trade.platform.repository.TickerRepository;
import com.algostrategix.trade.platform.repository.TradeHistoryRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.jacobpeterson.alpaca.openapi.trader.ApiException;
import net.jacobpeterson.alpaca.openapi.trader.model.Clock;
import net.jacobpeterson.alpaca.openapi.trader.model.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class TradeService {

    @Autowired
    private AlpacaService alpacaService;

    @Autowired
    private TickerRepository tickerRepository;

    @Autowired
    private TradeHistoryRepository tradeHistoryRepository;

    @Autowired
    private MartingaleConfigRepository configRepository;

    @Autowired
    private PortfolioService portfolioService;

    ExecutorService executorService;

    @PostConstruct
    public void init() {
        // Initialize executorService with the number of CPU cores
        int coreCount = Runtime.getRuntime().availableProcessors();
        this.executorService = Executors.newFixedThreadPool(coreCount);

        // Perform initial buys for tickers without trade history
        log.info("Initializing starter buys for all active tickers...");
        List<Ticker> tickers = tickerRepository.findAllByActiveTrue();

        for (Ticker ticker : tickers) {
            TradeHistory lastTrade = tradeHistoryRepository.findTopByTickerOrderByTradeTimestampDesc(ticker);

            // Place an initial buy if no previous buy exists
            if (lastTrade == null || !"BUY".equals(lastTrade.getAction())) {
                try {
                    double initialPrice = alpacaService.getMarketPrice(ticker.getSymbol());
                    int initialQuantity = configRepository.findById(1L)
                            .map(MartingaleConfig::getInitialQuantity)
                            .orElse(1);

                    Order order = alpacaService.placeBuyOrder(ticker.getSymbol(), initialQuantity);
                    saveTradeHistory(ticker, initialPrice, initialQuantity, "BUY", order, MarketSession.REGULAR);
                    portfolioService.updatePortfolioForTicker(ticker);
                    log.info("Placed initial buy order for {} at price {} with quantity {}", ticker.getSymbol(), initialPrice, initialQuantity);
                } catch (Exception e) {
                    log.error("Error placing initial buy order for ticker {}: {}", ticker.getSymbol(), e.getMessage(), e);
                }
            } else {
                log.info("Ticker {} already has a previous buy in trade history. Skipping initial buy.", ticker.getSymbol());
            }
        }
    }

    public void autoTrade(EnvironmentType environmentType) {
        log.info("Starting auto trade for active tickers...");

        List<Ticker> tickers = tickerRepository.findAllByActiveTrue();
        MartingaleConfig config = configRepository.findByEnvironmentType(environmentType)
                .orElseThrow(() -> new RuntimeException("Configuration not found for environment: " + environmentType));

        Clock clock;
        try {
            clock = alpacaService.getMarketClock();
        } catch (Exception e) {
            log.error("Failed to fetch market clock: {}", e.getMessage(), e);
            return;
        }

        LocalTime now = LocalTime.now();
        boolean isPreMarket = now.isBefore(Objects.requireNonNull(clock.getNextOpen()).toLocalTime());
        boolean isAfterMarket = now.isAfter(Objects.requireNonNull(clock.getNextClose()).toLocalTime());

        for (Ticker ticker : tickers) {
            executorService.submit(() -> {
                try {
                    log.info("Processing ticker: {}", ticker.getSymbol());
                    if (isPreMarket) {
                        log.info("Market is in pre-market session. Executing pre-market strategy for ticker: {}", ticker.getSymbol());
                        preMarketStrategy(ticker, config);
                    } else if (isAfterMarket) {
                        log.info("Market is in after-hours session. Executing after-market strategy for ticker: {}", ticker.getSymbol());
                        afterMarketStrategy(ticker, config);
                    } else {
                        log.info("Market is in regular session. Executing normal martingale strategy for ticker: {}", ticker.getSymbol());
                        executeMartingaleStrategy(ticker, config);
                    }
                } catch (Exception e) {
                    log.error("Error executing strategy for ticker {}: {}", ticker.getSymbol(), e.getMessage(), e);
                }
            });
        }
    }

    public void executeMartingaleStrategy(Ticker ticker, MartingaleConfig config) throws Exception {
        TradeHistory lastTrade = tradeHistoryRepository.findTopByTickerOrderByTradeTimestampDesc(ticker);
        double currentPrice = alpacaService.getMarketPrice(ticker.getSymbol());

        log.info("Current price for {} is: {}", ticker.getSymbol(), currentPrice);

        double lastPrice = (lastTrade != null) ? lastTrade.getPrice() : currentPrice;
        int quantityToTrade = (lastTrade != null) ? lastTrade.getQuantity() * 2 : config.getInitialQuantity();

        try {
            if (lastPrice > currentPrice * (1 + config.getThreshold())) {
                log.info("Price dropped for {}: lastPrice={}, currentPrice={}", ticker.getSymbol(), lastPrice, currentPrice);
                double buyingPower = Double.parseDouble(alpacaService.getBuyingPower());
                double requiredAmount = currentPrice * quantityToTrade;

                if (buyingPower >= requiredAmount) {
                    Order order = alpacaService.placeBuyOrder(ticker.getSymbol(), quantityToTrade);
                    saveTradeHistory(ticker, currentPrice, quantityToTrade, "BUY", order, MarketSession.REGULAR);
                    portfolioService.updatePortfolioForTicker(ticker);
                } else {
                    log.warn("Insufficient buying power to place a buy order for {}. Required: {}, Available: {}", ticker.getSymbol(), requiredAmount, buyingPower);
                }
            } else if (currentPrice > lastPrice) {
                // Check position availability for sell orders
                int availableQty = Integer.parseInt(alpacaService.getAvailableQty(ticker.getSymbol()));
                if (availableQty >= quantityToTrade) {
                    Order order = alpacaService.placeSellOrder(ticker.getSymbol(), quantityToTrade);
                    saveTradeHistory(ticker, currentPrice, quantityToTrade, "SELL", order, MarketSession.REGULAR);
                    portfolioService.updatePortfolioForTicker(ticker);
                } else {
                    log.warn("Insufficient quantity available to place a sell order for {}. Requested: {}, Available: {}", ticker.getSymbol(), quantityToTrade, availableQty);
                }
            }
        } catch (ApiException e) {
            handleApiException(e, ticker);
        }
    }



    private void afterMarketStrategy(Ticker ticker, MartingaleConfig config) throws Exception {
        double currentPrice = alpacaService.getMarketPrice(ticker.getSymbol());
        TradeHistory lastTrade = tradeHistoryRepository.findTopByTickerOrderByTradeTimestampDesc(ticker);
        double lastPrice = (lastTrade != null) ? lastTrade.getPrice() : currentPrice;
        double priceChangePercentage = Math.abs((currentPrice - lastPrice) / lastPrice) * 100;

        if (priceChangePercentage > config.getPriceChangePercentage()) {
            try {
                Order order;
                if (currentPrice > lastPrice) {
                    // Sell Order
                    order = alpacaService.placeSellOrder(ticker.getSymbol(), config.getInitialQuantity());
                    saveTradeHistory(ticker, currentPrice, config.getInitialQuantity(), "SELL", order, MarketSession.AFTER_MARKET);
                    portfolioService.updatePortfolioForTicker(ticker);
                } else {
                    // Check Buying Power before placing Buy Order
                    double buyingPower = Double.parseDouble(alpacaService.getBuyingPower());
                    double requiredAmount = currentPrice * config.getInitialQuantity();

                    if (buyingPower >= requiredAmount) {
                        order = alpacaService.placeBuyOrder(ticker.getSymbol(), config.getInitialQuantity());
                        saveTradeHistory(ticker, currentPrice, config.getInitialQuantity(), "BUY", order, MarketSession.AFTER_MARKET);
                        portfolioService.updatePortfolioForTicker(ticker);
                    } else {
                        if (log.isWarnEnabled()) {
                            log.warn("Insufficient buying power ({}) to place a buy order for {}. Required: {}", buyingPower, ticker.getSymbol(), requiredAmount);
                        }
                    }
                }
            } catch (ApiException e) {
                handleApiException(e, ticker);
            }
        } else {
            getInfo(ticker, config);
        }
    }

    private void preMarketStrategy(Ticker ticker, MartingaleConfig config) throws Exception {
        double currentPrice = alpacaService.getMarketPrice(ticker.getSymbol());
        TradeHistory lastTrade = tradeHistoryRepository.findTopByTickerOrderByTradeTimestampDesc(ticker);
        double lastPrice = (lastTrade != null) ? lastTrade.getPrice() : currentPrice;
        double priceChangePercentage = Math.abs((currentPrice - lastPrice) / lastPrice) * 100;

        if (priceChangePercentage > config.getPriceChangePercentage()) {
            try {
                Order order;
                if (currentPrice > lastPrice) {
                    // Sell Order
                    order = alpacaService.placeSellOrder(ticker.getSymbol(), config.getInitialQuantity());
                    saveTradeHistory(ticker, currentPrice, config.getInitialQuantity(), "SELL", order, MarketSession.PRE_MARKET);
                    portfolioService.updatePortfolioForTicker(ticker);
                } else {
                    // Check Buying Power before placing Buy Order
                    double buyingPower = Double.parseDouble(alpacaService.getBuyingPower());
                    double requiredAmount = currentPrice * config.getInitialQuantity();

                    if (buyingPower >= requiredAmount) {
                        order = alpacaService.placeBuyOrder(ticker.getSymbol(), config.getInitialQuantity());
                        saveTradeHistory(ticker, currentPrice, config.getInitialQuantity(), "BUY", order, MarketSession.PRE_MARKET);
                        portfolioService.updatePortfolioForTicker(ticker);
                    } else {
                        if (log.isWarnEnabled()) {
                            log.warn("Insufficient buying power ({}) to place a buy order for {}. Required: {}", buyingPower, ticker.getSymbol(), requiredAmount);
                        }
                    }
                }
            } catch (ApiException e) {
                handleApiException(e, ticker);
            }
        } else {
            getInfo(ticker, config);
        }
    }

    // Helper method to log API exception details
    private void handleApiException(ApiException e, Ticker ticker) {
        log.error("API Exception occurred while trading {}: {} - {}", ticker.getSymbol(), e.getCode(), e.getMessage());
        log.error("HTTP response body: {}", e.getResponseBody());

        // Handling specific error scenarios
        if (e.getCode() == 403) {
            if (e.getMessage().contains("insufficient buying power")) {
                log.warn("Insufficient buying power for {}. Adjust configuration or add funds.", ticker.getSymbol());
            } else if (e.getMessage().contains("cannot open a short sell while a long buy order is open")) {
                log.warn("Cannot short sell {} while there is an open long position.", ticker.getSymbol());
            } else if (e.getMessage().contains("insufficient qty available for order")) {
                log.warn("Not enough quantity available to sell {}. Adjust order size or check current holdings.", ticker.getSymbol());
            } else {
                log.warn("Forbidden action for {}. Please review permissions and account configuration.", ticker.getSymbol());
            }
        }
    }

    private static void getInfo(Ticker ticker, MartingaleConfig config) {
        log.info("No significant price change (>{}%) for {}. Skipping trade.", config.getPriceChangePercentage(), ticker.getSymbol());
    }


    private void saveTradeHistory(Ticker ticker, double price, int quantity, String action, Order order, MarketSession marketSession) {
        TradeHistory tradeHistory = new TradeHistory();
        tradeHistory.setTicker(ticker);
        tradeHistory.setPrice(price);
        tradeHistory.setQuantity(quantity);
        tradeHistory.setAction(action);
        tradeHistory.setTradeTimestamp(LocalDateTime.now());
        tradeHistory.setOrderId(order.getId());
        tradeHistory.setOrderStatus(String.valueOf(order.getStatus()));
        tradeHistory.setMarketSession(marketSession);
        tradeHistoryRepository.save(tradeHistory);

        log.info("Trade history saved for ticker: {}, action: {}, price: {}, quantity: {}, order ID: {}, market session {}",
                ticker.getSymbol(), action, price, quantity, order.getId(), marketSession.name());
    }
}