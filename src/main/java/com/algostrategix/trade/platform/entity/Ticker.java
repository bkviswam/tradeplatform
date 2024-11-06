package com.algostrategix.trade.platform.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Ticker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String symbol;

    private String name;  // Optional, human-readable name

    private boolean active;  // Is this ticker being actively traded?

}

