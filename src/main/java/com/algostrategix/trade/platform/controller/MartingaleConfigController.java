package com.algostrategix.trade.platform.controller;

import com.algostrategix.trade.platform.entity.MartingaleConfig;
import com.algostrategix.trade.platform.repository.MartingaleConfigRepository;
import com.algostrategix.trade.platform.scheduler.DynamicTradeScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config")
public class MartingaleConfigController {

    @Autowired
    private MartingaleConfigRepository configRepository;

    @Autowired
    private DynamicTradeScheduler schedulerService;

    @GetMapping("/martingale")
    public MartingaleConfig getConfig() {
        return configRepository.findById(1L).orElse(new MartingaleConfig());
    }

    @PostMapping("/martingale")
    public MartingaleConfig updateConfig(@RequestBody MartingaleConfig newConfig) {
        newConfig.setId(1L);  // Ensure we're updating the same config entry
        return configRepository.save(newConfig);
    }

    @PostMapping("/frequency")
    public void updateFrequency(@RequestParam long frequency) {
        schedulerService.updateFrequency(frequency);
    }

    @GetMapping("/frequency")
    public long getFrequency() {
        return schedulerService.getCurrentFrequency();
    }
    @PutMapping("/priceChangePercentage")
    public ResponseEntity<String> updatePriceChangePercentage(@RequestParam double newPercentage) {
        if (newPercentage <= 0 || newPercentage > 100) {
            return ResponseEntity.badRequest().body("Invalid percentage value. It should be between 0 and 100.");
        }

        MartingaleConfig config = configRepository.findById(1L).orElse(new MartingaleConfig());
        config.setPriceChangePercentage(newPercentage);
        configRepository.save(config);

        return ResponseEntity.ok("Price change percentage updated to " + newPercentage + "%");
    }
}

