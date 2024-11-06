package com.algostrategix.trade.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
public class Portfolio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ticker_id", nullable = false)
    private Ticker ticker;

    private Integer quantity;  // Current holding quantity for this ticker
    private Double averagePrice;  // Average price per unit of this holding
    private Double totalValue;  // Current market value of this holding
    private Double unrealizedPnl;  // Unrealized profit/loss
    private Double realizedPnl;  // Realized profit/loss for closed trades

    private LocalDateTime timestamp;  // Last update time for this record
}
