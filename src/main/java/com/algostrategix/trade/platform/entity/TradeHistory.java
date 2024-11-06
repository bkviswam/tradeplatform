package com.algostrategix.trade.platform.entity;

import com.algostrategix.trade.platform.enums.MarketSession;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class TradeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Ticker ticker;

    private double price;
    private int quantity;
    private String action;
    private LocalDateTime tradeTimestamp;

    private String orderId;       // Store the order ID
    private String orderStatus;   // Store the order status

    @Enumerated(EnumType.STRING)
    private MarketSession marketSession;
}
