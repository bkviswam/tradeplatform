package com.algostrategix.trade.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Data
@Entity
public class Portfolio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ticker_id", nullable = false)
    private Ticker ticker;

    private Integer quantity;             // Current holding quantity
    private Double averagePrice;          // Average price of the holding
    private Double marketPrice;           // Latest market price for calculation

    @Transient
    private Double marketValue;           // Calculated market value
    @Transient
    private Double costBasis;             // Calculated cost basis
    @Transient
    private Double totalPlPercent;        // Total P/L in percentage
    @Transient
    private Double totalPlDollar;         // Total P/L in dollar amount

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime timestamp;      // Tracks the last update time

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.timestamp = LocalDateTime.now();
    }

    public void calculateDerivedFields() {
        if (marketPrice == null || averagePrice == null || quantity == null) {
            log.warn("Cannot calculate derived fields for ticker {} due to null values", ticker.getSymbol());
            return;
        }
        this.marketValue = round(quantity * marketPrice);
        this.costBasis = round(quantity * averagePrice);
        this.totalPlDollar = round(marketValue - costBasis);
        this.totalPlPercent = costBasis != 0 ? round((totalPlDollar / costBasis) * 100) : 0;
    }


    private Double round(Double value) {
        return BigDecimal.valueOf(value).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }
}