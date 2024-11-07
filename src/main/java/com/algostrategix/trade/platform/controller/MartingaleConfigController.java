package com.algostrategix.trade.platform.controller;

import com.algostrategix.trade.platform.entity.MartingaleConfig;
import com.algostrategix.trade.platform.enums.EnvironmentType;
import com.algostrategix.trade.platform.enums.MarketSession;
import com.algostrategix.trade.platform.repository.MartingaleConfigRepository;
import com.algostrategix.trade.platform.scheduler.DynamicTradeScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/config")
public class MartingaleConfigController {

    @Autowired
    private MartingaleConfigRepository configRepository;

    @Autowired
    private DynamicTradeScheduler schedulerService;

    // Get MartingaleConfig based on EnvironmentType and MarketSession
    @GetMapping("/martingale")
    public ResponseEntity<MartingaleConfig> getConfig(@RequestParam EnvironmentType environmentType,
                                                      @RequestParam MarketSession marketSession) {
        Optional<MartingaleConfig> config = configRepository.findByEnvironmentTypeAndMarketSession(environmentType, marketSession);
        return config.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Update or create MartingaleConfig for a specific EnvironmentType and MarketSession
    @PostMapping("/martingale")
    public ResponseEntity<MartingaleConfig> updateConfig(@RequestBody MartingaleConfig newConfig) {
        // Ensure the config entry is for the specified environment and market session
        Optional<MartingaleConfig> existingConfig = configRepository.findByEnvironmentTypeAndMarketSession(
                newConfig.getEnvironmentType(), newConfig.getMarketSession());

        existingConfig.ifPresent(existing -> newConfig.setId(existing.getId()));  // Use existing ID if present
        MartingaleConfig savedConfig = configRepository.save(newConfig);

        return ResponseEntity.ok(savedConfig);
    }

    // Update trading frequency
    @PostMapping("/frequency")
    public void updateFrequency(@RequestParam long frequency) {
        schedulerService.updateFrequency(frequency);
    }

    // Get current trading frequency
    @GetMapping("/frequency")
    public long getFrequency() {
        return schedulerService.getCurrentFrequency();
    }

    // Update the price change percentage in the configuration for a specific EnvironmentType and MarketSession
    @PutMapping("/priceChangePercentage")
    public ResponseEntity<String> updatePriceChangePercentage(
            @RequestParam EnvironmentType environmentType,
            @RequestParam MarketSession marketSession,
            @RequestParam double newPercentage) {

        if (newPercentage <= 0 || newPercentage > 100) {
            return ResponseEntity.badRequest().body("Invalid percentage value. It should be between 0 and 100.");
        }

        Optional<MartingaleConfig> configOpt = configRepository.findByEnvironmentTypeAndMarketSession(environmentType, marketSession);
        if (configOpt.isPresent()) {
            MartingaleConfig config = configOpt.get();
            config.setPriceChangePercentage(newPercentage);
            configRepository.save(config);
            return ResponseEntity.ok("Price change percentage updated to " + newPercentage + "%");
        } else {
            return ResponseEntity.badRequest().body("Configuration not found for the specified environment and session.");
        }
    }
}