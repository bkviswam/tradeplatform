package com.algostrategix.trade.platform.controller;

import com.algostrategix.trade.platform.entity.AlpacaCredentials;
import com.algostrategix.trade.platform.enums.EnvironmentType;
import com.algostrategix.trade.platform.service.AlpacaCredentialsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/credentials")
public class AlpacaCredentialsController {

    @Autowired
    private AlpacaCredentialsService credentialsService;

    // Save credentials for paper or live trading
    @PostMapping
    public ResponseEntity<AlpacaCredentials> saveCredentials(@RequestBody AlpacaCredentials credentials) {
        return ResponseEntity.ok(credentialsService.saveCredentials(credentials));
    }

    // Get credentials for paper or live trading
    @GetMapping
    public ResponseEntity<AlpacaCredentials> getCredentials(@RequestParam EnvironmentType environmentType) {
        return ResponseEntity.ok(credentialsService.getCredentials(environmentType));
    }
}

