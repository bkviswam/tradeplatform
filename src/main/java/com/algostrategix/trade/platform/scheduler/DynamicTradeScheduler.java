package com.algostrategix.trade.platform.scheduler;

import com.algostrategix.trade.platform.config.AlpacaConfig;
import com.algostrategix.trade.platform.entity.MartingaleConfig;
import com.algostrategix.trade.platform.enums.EnvironmentType;
import com.algostrategix.trade.platform.enums.MarketSession;
import com.algostrategix.trade.platform.repository.MartingaleConfigRepository;
import com.algostrategix.trade.platform.service.AlpacaService;
import com.algostrategix.trade.platform.service.TradeService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DynamicTradeScheduler {

    private final TradeService tradeService;
    private final AlpacaService alpacaService;
    private final MartingaleConfigRepository configRepository;
    private final AlpacaConfig alpacaConfig;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> scheduledTask;
    private MarketSession currentSession;

    @Autowired
    public DynamicTradeScheduler(TradeService tradeService,
                                 AlpacaService alpacaService,
                                 MartingaleConfigRepository configRepository,
                                 AlpacaConfig alpacaConfig) {
        this.tradeService = tradeService;
        this.alpacaService = alpacaService;
        this.configRepository = configRepository;
        this.alpacaConfig = alpacaConfig;
    }

    @PostConstruct
    public void init() {
        // Initial session setup and task scheduling
        updateMarketSession();
        scheduleTask();
    }

    /**
     * Schedules or reschedules the trading task based on the current market session.
     */
    public void scheduleTask() {
        EnvironmentType environmentType = alpacaConfig.getEnvironmentType();
        MartingaleConfig config = getSessionConfig(environmentType, currentSession);

        long frequency = config.getFrequency();
        if (frequency <= 0) {
            log.warn("Invalid frequency value: {}. Using default frequency: 60000 ms.", frequency);
            frequency = 60000; // Default to 60 seconds
        }

        // Cancel any existing scheduled task
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(false);
        }

        // Schedule the task with the session-specific frequency and configuration
        scheduledTask = scheduler.scheduleWithFixedDelay(() -> {
            try {
                if (alpacaService.isMarketOpen() || alpacaService.isExtendedHours()) {
                    log.info("Market session: {}, starting trade operations for {} environment...", currentSession, environmentType);
                    tradeService.autoTrade(environmentType);
                } else {
                    log.info("Market is closed, skipping trading operations.");
                }
            } catch (Exception e) {
                log.error("Error during trading execution: {}", e.getMessage(), e);
            }
        }, 0, frequency, TimeUnit.MILLISECONDS);

        log.info("Trading task scheduled with frequency: {} ms for {} environment in {} session", frequency, environmentType, currentSession);
    }

    /**
     * Updates the current market session based on the Alpaca service.
     * Reschedules the task if the session has changed.
     */
    private void updateMarketSession() {
        MarketSession newSession = alpacaService.getCurrentMarketSession();
        if (newSession != currentSession) {
            currentSession = newSession;
            log.info("Market session updated to: {}", currentSession);
            scheduleTask(); // Reschedule task for the new session
        }
    }

    /**
     * Retrieves the Martingale configuration for the given environment and market session.
     * @param environmentType The trading environment type (PAPER or LIVE).
     * @param session The current market session (PRE_MARKET, REGULAR, AFTER_MARKET).
     * @return The corresponding MartingaleConfig.
     */
    private MartingaleConfig getSessionConfig(EnvironmentType environmentType, MarketSession session) {
        return configRepository.findByEnvironmentTypeAndMarketSession(environmentType, session)
                .orElseThrow(() -> new RuntimeException("Configuration not found for environment: " + environmentType + " and session: " + session));
    }

    /**
     * Updates the trading task frequency dynamically for the current environment and session.
     * @param newFrequency New frequency for task execution.
     */
    public void updateFrequency(long newFrequency) {
        if (newFrequency <= 0) {
            log.warn("Attempted to set invalid frequency: {}. Skipping update.", newFrequency);
            return; // Do not allow invalid frequencies
        }

        EnvironmentType environmentType = alpacaConfig.getEnvironmentType();
        MartingaleConfig config = getSessionConfig(environmentType, currentSession);

        config.setFrequency(newFrequency);
        configRepository.save(config);
        log.info("Frequency updated to {} ms for {} environment in {} session, rescheduling task...", newFrequency, environmentType, currentSession);

        scheduleTask();
    }

    /**
     * Retrieves the current trading task frequency for the current environment and session.
     * @return Current task frequency in milliseconds.
     */
    public long getCurrentFrequency() {
        EnvironmentType environmentType = alpacaConfig.getEnvironmentType();
        MartingaleConfig config = getSessionConfig(environmentType, currentSession);
        return config.getFrequency();
    }
}