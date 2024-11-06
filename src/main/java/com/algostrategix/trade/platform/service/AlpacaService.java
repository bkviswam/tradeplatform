package com.algostrategix.trade.platform.service;

import com.algostrategix.trade.platform.config.AlpacaConfig;
import com.algostrategix.trade.platform.entity.AlpacaCredentials;
import com.algostrategix.trade.platform.enums.EnvironmentType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.model.util.apitype.MarketDataWebsocketSourceType;
import net.jacobpeterson.alpaca.model.util.apitype.TraderAPIEndpointType;
import net.jacobpeterson.alpaca.openapi.marketdata.model.StockFeed;
import net.jacobpeterson.alpaca.openapi.trader.ApiException;
import net.jacobpeterson.alpaca.openapi.trader.model.*;
import net.jacobpeterson.alpaca.openapi.marketdata.model.StockTrade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.Objects;

@Slf4j
@Service
public class AlpacaService {

    private AlpacaAPI alpacaAPI;

    @Autowired
    private AlpacaCredentialsService credentialsService;

    @Autowired
    private AlpacaConfig alpacaConfig;

    @PostConstruct
    public void initializeAlpacaAPI() {
        // Fetch the environment type from configuration
        EnvironmentType environmentType = alpacaConfig.getEnvironmentType();

        // Get credentials based on environment type
        AlpacaCredentials credentials = credentialsService.getCredentials(environmentType);

        // Determine API endpoint type based on the environment
        TraderAPIEndpointType apiEndpointType = environmentType == EnvironmentType.LIVE
                ? TraderAPIEndpointType.LIVE
                : TraderAPIEndpointType.PAPER;

        log.info("Initializing Alpaca API for environment: {}", environmentType);
        log.info("Using API Key: {}, Base URL: {}", credentials.getApiKey(), credentials.getBaseUrl());

        // Initialize AlpacaAPI instance
        this.alpacaAPI = new AlpacaAPI(
                credentials.getApiKey(),
                credentials.getApiSecret(),
                apiEndpointType,
                MarketDataWebsocketSourceType.IEX);
    }

    // Place a market order to buy a stock
    public Order placeBuyOrder(String symbol, int quantity) throws Exception {
        try {
            // Determine if it's extended hours
            boolean isExtendedHours = !isMarketOpen() && isExtendedHours();

            // Create the order request
            PostOrderRequest orderRequest = new PostOrderRequest()
                    .symbol(symbol)
                    .qty(String.valueOf(quantity))
                    .side(OrderSide.BUY)
                    .timeInForce(TimeInForce.DAY);

            if (isExtendedHours) {
                // For extended hours, use a limit order and round the price to two decimal places
                double currentPrice = getMarketPrice(symbol);
                BigDecimal roundedPrice = BigDecimal.valueOf(currentPrice).setScale(2, RoundingMode.HALF_UP);
                orderRequest.type(OrderType.LIMIT)
                        .limitPrice(String.valueOf(roundedPrice.doubleValue()))
                        .extendedHours(true); // Enable extended hours for limit order

                log.info("Placing extended hours limit buy order for {} at rounded price {}", symbol, roundedPrice);
            } else {
                // During regular hours, place a market order
                orderRequest.type(OrderType.MARKET);
                log.info("Placing market buy order for {}", symbol);
            }

            return alpacaAPI.trader().orders().postOrder(orderRequest);
        } catch (Exception e) {
            throw new Exception("Failed to place a buy order for " + symbol + ": " + e.getMessage(), e);
        }
    }



    // Place a market order to sell a stock
    public Order placeSellOrder(String symbol, int quantity) throws Exception {
        try {
            // Determine if it's extended hours
            boolean isExtendedHours = !isMarketOpen() && isExtendedHours();

            // Create a PostOrderRequest with common parameters
            PostOrderRequest orderRequest = new PostOrderRequest()
                    .symbol(symbol)
                    .qty(String.valueOf(quantity))
                    .side(OrderSide.SELL)
                    .timeInForce(TimeInForce.DAY);

            if (isExtendedHours) {
                // Place a limit order during extended hours, with rounded current price as the limit price
                double currentPrice = getMarketPrice(symbol);
                BigDecimal roundedPrice = BigDecimal.valueOf(currentPrice).setScale(2, RoundingMode.HALF_UP);
                orderRequest.type(OrderType.LIMIT)
                        .limitPrice(String.valueOf(roundedPrice.doubleValue()))
                        .extendedHours(true); // Enable extended hours for limit order

                log.info("Placing extended hours limit sell order for {} at rounded price {}", symbol, roundedPrice);
            } else {
                // During regular trading hours, place a market order
                orderRequest.type(OrderType.MARKET);
                log.info("Placing market sell order for {}", symbol);
            }

            return alpacaAPI.trader().orders().postOrder(orderRequest);
        } catch (Exception e) {
            throw new Exception("Failed to place a sell order for " + symbol + ": " + e.getMessage(), e);
        }
    }


    // Fetch the current market price (latest trade price) for a ticker
    public double getMarketPrice(String symbol) throws Exception {
        try {
            // Fetch the latest trade for the stock symbol
            StockTrade latestTrade = alpacaAPI.marketData().stock().stockLatestTradeSingle(symbol, StockFeed.IEX, null).getTrade();
            return latestTrade.getP();  // Get the price of the latest trade
        } catch (Exception e) {
            throw new Exception("Failed to fetch market price for " + symbol + ": " + e.getMessage(), e);
        }
    }

    public boolean isMarketOpen() throws Exception {
        // Get current market clock data
        Clock clock = alpacaAPI.trader().clock().getClock();
        return Boolean.TRUE.equals(clock.getIsOpen());  // Returns true if the market is open
    }

    public boolean isExtendedHours() throws Exception {
        // Get current time and compare with market open/close time
        Clock clock = alpacaAPI.trader().clock().getClock();
        LocalTime currentTime = LocalTime.now();

        // Assuming you know the market hours (can be configured)
        LocalTime regularMarketOpen = Objects.requireNonNull(clock.getNextOpen()).toLocalTime();
        LocalTime regularMarketClose = Objects.requireNonNull(clock.getNextClose()).toLocalTime();

        return currentTime.isBefore(regularMarketOpen) || currentTime.isAfter(regularMarketClose);
    }

    public Clock getMarketClock() throws Exception {
        try {
            return alpacaAPI.trader().clock().getClock();
        } catch (Exception e) {
            throw new Exception("Failed to fetch market clock: " + e.getMessage(), e);
        }
    }
    public String getBuyingPower() throws Exception {
        return alpacaAPI.trader().accounts().getAccount().getBuyingPower();
    }


    public String getAvailableQty(String symbol) throws Exception {
        try {
            // Retrieve the position for the given ticker symbol
            Position position = alpacaAPI.trader().positions().getOpenPosition(symbol);
            return position.getQty();
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                // Log and return 0 if no position exists for this symbol
                log.warn("No position found for symbol: {}", symbol);
                return "0";
            }
            // Throw exception if there's another API-related error
            throw new Exception("Error retrieving available quantity for " + symbol + ": " + e.getMessage(), e);
        }
    }

}
