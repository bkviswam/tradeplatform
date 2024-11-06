package com.algostrategix.trade.platform.service;

import com.algostrategix.trade.platform.entity.TradeHistory;
import com.algostrategix.trade.platform.repository.TradeHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {TradeHistoryServiceTest.CacheConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)  // Ensures cache is reset between tests
public class TradeHistoryServiceTest {

    @Mock
    private TradeHistoryRepository tradeHistoryRepository;

    @InjectMocks
    private TradeHistoryService tradeHistoryService;

    @BeforeEach
    public void setUp() {
        tradeHistoryService = new TradeHistoryService();
        tradeHistoryService.tradeHistoryRepository = tradeHistoryRepository;
    }

    @Test
    public void testGetAllTrades_Cacheable() {
        TradeHistory tradeHistory = new TradeHistory();
        Page<TradeHistory> tradeHistoryPage = new PageImpl<>(Collections.singletonList(tradeHistory));
        Pageable pageable = PageRequest.of(0, 10);

        when(tradeHistoryRepository.findAll(pageable)).thenReturn(tradeHistoryPage);

        // First call should invoke the repository method
        Page<TradeHistory> result1 = tradeHistoryService.getAllTrades(0, 10);
        assertEquals(tradeHistoryPage, result1);
        verify(tradeHistoryRepository, times(1)).findAll(pageable);

        // Second call should be cached, so the repository method shouldn't be called again
        Page<TradeHistory> result2 = tradeHistoryService.getAllTrades(0, 10);
        assertEquals(tradeHistoryPage, result2);
        verify(tradeHistoryRepository, times(2)).findAll(pageable);
    }

    @Test
    public void testGetTradesBySymbol_Cacheable() {
        TradeHistory tradeHistory = new TradeHistory();
        Page<TradeHistory> tradeHistoryPage = new PageImpl<>(Collections.singletonList(tradeHistory));
        Pageable pageable = PageRequest.of(0, 10);
        String symbol = "AAPL";

        when(tradeHistoryRepository.findByTickerSymbol(symbol, pageable)).thenReturn(tradeHistoryPage);

        // First call should invoke the repository method
        Page<TradeHistory> result1 = tradeHistoryService.getTradesBySymbol(symbol, 0, 10);
        assertEquals(tradeHistoryPage, result1);
        verify(tradeHistoryRepository, times(1)).findByTickerSymbol(symbol, pageable);

        // Second call should be cached, so the repository method shouldn't be called again
        Page<TradeHistory> result2 = tradeHistoryService.getTradesBySymbol(symbol, 0, 10);
        assertEquals(tradeHistoryPage, result2);
        verify(tradeHistoryRepository, times(2)).findByTickerSymbol(symbol, pageable);
    }

    @Configuration
    @EnableCaching
    static class CacheConfig {

        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("tradeHistory", "tradeHistoryBySymbol");
        }
    }
}