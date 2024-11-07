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
import jakarta.annotation.PreDestroy;
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

    private ExecutorService executorService;

    @PostConstruct
    public void init() {
        int coreCount = Runtime.getRuntime().availableProcessors();
        this.executorService = Executors.newFixedThreadPool(coreCount);
        initializeTickers();
    }

    @PreDestroy
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    private void initializeTickers() {
        log.info("Initializing starter buys for active tickers...");
        List<Ticker> tickers = tickerRepository.findAllByActiveTrue();

        for (Ticker ticker : tickers) {
            TradeHistory lastTrade = tradeHistoryRepository.findTopByTickerOrderByTradeTimestampDesc(ticker);

            if (lastTrade == null || !"BUY".equals(lastTrade.getAction())) {
                performInitialBuy(ticker);
            } else {
                log.info("Ticker {} already has a previous buy in trade history. Skipping initial buy.", ticker.getSymbol());
            }
        }
    }

    private void performInitialBuy(Ticker ticker) {
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
    }

    public void autoTrade(EnvironmentType environmentType) {
        log.info("Starting auto trade for active tickers...");
        List<Ticker> tickers = tickerRepository.findAllByActiveTrue();

        Clock clock = getMarketClock();
        if (clock == null) return;

        LocalTime now = LocalTime.now();
        boolean isPreMarket = now.isBefore(Objects.requireNonNull(clock.getNextOpen()).toLocalTime());
        boolean isAfterMarket = now.isAfter(Objects.requireNonNull(clock.getNextClose()).toLocalTime());

        MarketSession currentSession = isPreMarket ? MarketSession.PRE_MARKET
                : isAfterMarket ? MarketSession.AFTER_MARKET : MarketSession.REGULAR;

        MartingaleConfig config = configRepository.findByEnvironmentTypeAndMarketSession(environmentType, currentSession)
                .orElseThrow(() -> new RuntimeException("Configuration not found for environment: " + environmentType + " and session: " + currentSession));

        for (Ticker ticker : tickers) {
            executorService.submit(() -> {
                try {
                    executeStrategyBasedOnSession(ticker, config, currentSession);
                } catch (Exception e) {
                    log.error("Error executing strategy for ticker {}: {}", ticker.getSymbol(), e.getMessage(), e);
                }
            });
        }
    }

    private Clock getMarketClock() {
        try {
            return alpacaService.getMarketClock();
        } catch (Exception e) {
            log.error("Failed to fetch market clock: {}", e.getMessage(), e);
            return null;
        }
    }

    private void executeStrategyBasedOnSession(Ticker ticker, MartingaleConfig config, MarketSession session) throws Exception {
        log.info("Processing ticker: {} in {} session", ticker.getSymbol(), session);
        executeStrategy(ticker, config, session);
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

    private void executeStrategy(Ticker ticker, MartingaleConfig config, MarketSession session) throws Exception {
        double currentPrice = alpacaService.getMarketPrice(ticker.getSymbol());
        TradeHistory lastTrade = tradeHistoryRepository.findTopByTickerOrderByTradeTimestampDesc(ticker);
        double lastPrice = (lastTrade != null) ? lastTrade.getPrice() : currentPrice;
        double priceChangePercentage = Math.abs((currentPrice - lastPrice) / lastPrice) * 100;

        if (priceChangePercentage > config.getPriceChangePercentage()) {
            if (currentPrice > lastPrice) {
                placeSellOrder(ticker, currentPrice, config, session);
            } else {
                placeBuyOrderIfAffordable(ticker, currentPrice, config, session);
            }
        } else {
            log.info("No significant price change (>{}%) for {}. Skipping trade.", config.getPriceChangePercentage(), ticker.getSymbol());
        }
    }

    private void placeSellOrder(Ticker ticker, double price, MartingaleConfig config, MarketSession session) throws Exception {
        Order order = alpacaService.placeSellOrder(ticker.getSymbol(), config.getInitialQuantity());
        saveTradeHistory(ticker, price, config.getInitialQuantity(), "SELL", order, session);
        portfolioService.updatePortfolioForTicker(ticker);
    }

    private void placeBuyOrderIfAffordable(Ticker ticker, double price, MartingaleConfig config, MarketSession session) throws Exception {
        double buyingPower = Double.parseDouble(alpacaService.getBuyingPower());
        double requiredAmount = price * config.getInitialQuantity();

        if (buyingPower >= requiredAmount) {
            Order order = alpacaService.placeBuyOrder(ticker.getSymbol(), config.getInitialQuantity());
            saveTradeHistory(ticker, price, config.getInitialQuantity(), "BUY", order, session);
            portfolioService.updatePortfolioForTicker(ticker);
        } else {
            log.warn("Insufficient buying power ({}) for {}. Required: {}", buyingPower, ticker.getSymbol(), requiredAmount);
        }
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
}
