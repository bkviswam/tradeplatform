package com.algostrategix.trade.platform.entity;

import com.algostrategix.trade.platform.enums.EnvironmentType;
import com.algostrategix.trade.platform.enums.MarketSession;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class MartingaleConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double threshold = 0.05;  // Default threshold
    private int maxRounds = 6;        // Default max rounds
    private int initialQuantity = 1;
    private long frequency = 60000;   // Default frequency (run every minute)
    private double priceChangePercentage;  //configurable percentage

    @Enumerated(EnumType.STRING)
    private EnvironmentType environmentType;

    @Enumerated(EnumType.STRING)
    private MarketSession marketSession;
}

