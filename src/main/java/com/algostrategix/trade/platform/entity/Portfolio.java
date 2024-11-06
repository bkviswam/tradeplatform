package com.algostrategix.trade.platform.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Portfolio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ticker_id", nullable = false)
    private Ticker ticker;

    private Integer quantity;  // Current holding

    private Double averagePrice;  // Average price of the holding

    // Getters, Setters, Constructors
}

