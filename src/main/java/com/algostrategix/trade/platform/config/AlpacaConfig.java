package com.algostrategix.trade.platform.config;

import com.algostrategix.trade.platform.enums.EnvironmentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AlpacaConfig {

    @Value("${alpaca.environment-type:PAPER}")  // Default to PAPER if not set
    private String environmentType;

    public EnvironmentType getEnvironmentType() {
        return EnvironmentType.valueOf(environmentType);
    }
}
