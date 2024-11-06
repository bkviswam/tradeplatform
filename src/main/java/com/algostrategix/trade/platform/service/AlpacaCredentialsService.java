package com.algostrategix.trade.platform.service;

import com.algostrategix.trade.platform.entity.AlpacaCredentials;
import com.algostrategix.trade.platform.enums.EnvironmentType;
import com.algostrategix.trade.platform.repository.AlpacaCredentialsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AlpacaCredentialsService {

    @Autowired
    private AlpacaCredentialsRepository credentialsRepository;

    // Save Alpaca credentials for a given environment (PAPER or LIVE)
    public AlpacaCredentials saveCredentials(AlpacaCredentials credentials) {
        return credentialsRepository.save(credentials);
    }

    // Fetch credentials by environment type (PAPER or LIVE)
    public AlpacaCredentials getCredentials(EnvironmentType environmentType) {
        return credentialsRepository.findByEnvironmentType(environmentType)
                .orElseThrow(() -> new RuntimeException("Credentials not found for environment: " + environmentType));
    }
}
