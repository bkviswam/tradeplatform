package com.algostrategix.trade.platform.entity;

import com.algostrategix.trade.platform.enums.EnvironmentType;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class AlpacaCredentials {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String apiKey;

    @Column(nullable = false)
    private String apiSecret;

    @Column(nullable = false)
    private String baseUrl;  // e.g., https://paper-api.alpaca.markets for paper trading

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnvironmentType environmentType;  // PAPER or LIVE

}

