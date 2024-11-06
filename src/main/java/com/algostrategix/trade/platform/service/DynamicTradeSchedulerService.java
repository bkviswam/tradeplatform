package com.algostrategix.trade.platform.service;

import com.algostrategix.trade.platform.config.AlpacaConfig;
import com.algostrategix.trade.platform.entity.MartingaleConfig;
import com.algostrategix.trade.platform.enums.EnvironmentType;
import com.algostrategix.trade.platform.repository.MartingaleConfigRepository;
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
public class DynamicTradeSchedulerService {

    private final TradeService tradeService;
    private final AlpacaService alpacaService;
    private final MartingaleConfigRepository configRepository;
    private final AlpacaConfig alpacaConfig;  // Configuration to get environment type

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> scheduledTask;

    @Autowired
    public DynamicTradeSchedulerService(TradeService tradeService,
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
        scheduleTask(); // Initialize the task scheduling after the bean is created
    }

    /**
     * Schedules the trading task based on the configured frequency, while also
     * checking if the market is open or in extended hours.
     */
    public void scheduleTask() {
        // Get environment type (PAPER or LIVE) from configuration
        EnvironmentType environmentType = alpacaConfig.getEnvironmentType();

        // Fetch configuration based on environment type
        MartingaleConfig config = configRepository.findByEnvironmentType(environmentType)
                .orElseThrow(() -> new RuntimeException("Configuration not found for environment: " + environmentType));

        long frequency = config.getFrequency();
        if (frequency <= 0) {
            log.warn("Invalid frequency value: {}. Using default frequency: 60000 ms.", frequency);
            frequency = 60000; // Fallback to a default frequency of 60 seconds
        }

        // Cancel any existing scheduled task
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(false);
        }

        // Schedule the task with the given frequency and ensure it only runs during trading hours
        scheduledTask = scheduler.scheduleWithFixedDelay(() -> {
            try {
                if (alpacaService.isMarketOpen() || alpacaService.isExtendedHours()) {
                    log.info("Market is open or in extended hours, starting trading operations for {} environment...", environmentType);
                    tradeService.autoTrade(environmentType);  // Execute trading strategy based on environment type
                } else {
                    log.info("Market is closed, skipping trading operations.");
                }
            } catch (Exception e) {
                log.error("Error during trading execution: {}", e.getMessage(), e);
            }
        }, 0, frequency, TimeUnit.MILLISECONDS);

        log.info("Trading task scheduled with frequency: {} ms for {} environment", frequency, environmentType);
    }

    /**
     * Updates the trading task frequency dynamically for the current environment.
     * @param newFrequency New frequency for task execution
     */
    public void updateFrequency(long newFrequency) {
        if (newFrequency <= 0) {
            log.warn("Attempted to set invalid frequency: {}. Skipping update.", newFrequency);
            return; // Do not allow invalid frequencies
        }

        // Get environment type and fetch config for updating frequency
        EnvironmentType environmentType = alpacaConfig.getEnvironmentType();
        MartingaleConfig config = configRepository.findByEnvironmentType(environmentType)
                .orElseThrow(() -> new RuntimeException("Configuration not found for environment: " + environmentType));

        // Update frequency in configuration and reschedule the task
        config.setFrequency(newFrequency);
        configRepository.save(config);

        log.info("Frequency updated to {} ms for {} environment, rescheduling task...", newFrequency, environmentType);
        scheduleTask();
    }

    /**
     * Retrieves the current frequency of the trading task from the configuration.
     * @return Current task frequency in milliseconds for the current environment
     */
    public long getCurrentFrequency() {
        EnvironmentType environmentType = alpacaConfig.getEnvironmentType();
        MartingaleConfig config = configRepository.findByEnvironmentType(environmentType)
                .orElseThrow(() -> new RuntimeException("Configuration not found for environment: " + environmentType));
        return config.getFrequency();
    }
}