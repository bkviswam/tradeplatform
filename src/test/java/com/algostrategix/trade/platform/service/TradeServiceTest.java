package com.algostrategix.trade.platform.service;

import com.algostrategix.trade.platform.entity.MartingaleConfig;
import com.algostrategix.trade.platform.entity.Ticker;
import com.algostrategix.trade.platform.entity.TradeHistory;
import com.algostrategix.trade.platform.enums.EnvironmentType;
import com.algostrategix.trade.platform.repository.MartingaleConfigRepository;
import com.algostrategix.trade.platform.repository.TickerRepository;
import com.algostrategix.trade.platform.repository.TradeHistoryRepository;
import net.jacobpeterson.alpaca.openapi.trader.model.Clock;
import net.jacobpeterson.alpaca.openapi.trader.model.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TradeServiceTest {

    @Mock
    private AlpacaService alpacaService;

    @Mock
    private TickerRepository tickerRepository;

    @Mock
    private TradeHistoryRepository tradeHistoryRepository;

    @Mock
    private MartingaleConfigRepository configRepository;

    @InjectMocks
    private TradeService tradeService;

    @Mock
    private ExecutorService executorService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testInit_initializesExecutorAndPlacesInitialBuyOrder() throws Exception {
        Ticker ticker = new Ticker();
        ticker.setSymbol("AAPL");
        ticker.setActive(true);

        MartingaleConfig config = new MartingaleConfig();
        config.setInitialQuantity(10);

        Order mockOrder = new Order();
        mockOrder.setId("mockOrderId");

        when(tickerRepository.findAllByActiveTrue()).thenReturn(Collections.singletonList(ticker));
        when(tradeHistoryRepository.findTopByTickerOrderByTradeTimestampDesc(any(Ticker.class))).thenReturn(null);
        when(alpacaService.getMarketPrice("AAPL")).thenReturn(150.0);
        when(configRepository.findById(1L)).thenReturn(Optional.of(config));
        when(alpacaService.placeBuyOrder("AAPL", 10)).thenReturn(mockOrder);

        tradeService.init();

        verify(alpacaService, times(1)).placeBuyOrder("AAPL", 10);
        verify(tradeHistoryRepository, times(1)).save(any(TradeHistory.class));
    }

    @Test
    public void testAutoTrade_withValidEnvironmentType() throws Exception {
        Ticker ticker = new Ticker();
        ticker.setSymbol("AAPL");
        ticker.setActive(true);

        MartingaleConfig config = new MartingaleConfig();
        config.setInitialQuantity(10);
        config.setThreshold(0.05);

        Clock mockClock = new Clock();
        mockClock.setIsOpen(true);
        mockClock.setNextOpen(OffsetDateTime.now().minusHours(1));
        mockClock.setNextClose(OffsetDateTime.now().plusHours(1));

        when(tickerRepository.findAllByActiveTrue()).thenReturn(Collections.singletonList(ticker));
        when(configRepository.findByEnvironmentType(EnvironmentType.PAPER)).thenReturn(Optional.of(config));
        when(alpacaService.getMarketClock()).thenReturn(mockClock);

        // Stubbing executorService.submit() for this test only
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(executorService).submit(any(Runnable.class));

        tradeService.autoTrade(EnvironmentType.PAPER);

        verify(alpacaService, atLeastOnce()).getMarketClock();
    }


    @Test
    public void testExecuteMartingaleStrategy_buyOrderOnPriceDrop() throws Exception {
        Ticker ticker = new Ticker();
        ticker.setSymbol("AAPL");

        MartingaleConfig config = new MartingaleConfig();
        config.setInitialQuantity(10);
        config.setThreshold(0.05);

        TradeHistory lastTrade = new TradeHistory();
        lastTrade.setPrice(150.0);
        lastTrade.setQuantity(10);

        Order mockOrder = new Order();
        mockOrder.setId("mockOrderId");

        when(tradeHistoryRepository.findTopByTickerOrderByTradeTimestampDesc(any(Ticker.class))).thenReturn(lastTrade);
        when(alpacaService.getMarketPrice("AAPL")).thenReturn(140.0);
        when(alpacaService.placeBuyOrder("AAPL", 20)).thenReturn(mockOrder);

        tradeService.executeMartingaleStrategy(ticker, config);

        verify(alpacaService, times(1)).placeBuyOrder("AAPL", 20);
        verify(tradeHistoryRepository, times(1)).save(any(TradeHistory.class));
    }

    @Test
    public void testExecuteMartingaleStrategy_sellOrderOnPriceIncrease() throws Exception {
        Ticker ticker = new Ticker();
        ticker.setSymbol("AAPL");

        MartingaleConfig config = new MartingaleConfig();
        config.setInitialQuantity(10);

        TradeHistory lastTrade = new TradeHistory();
        lastTrade.setPrice(140.0);
        lastTrade.setQuantity(10);

        Order mockOrder = new Order();
        mockOrder.setId("mockOrderId");

        when(tradeHistoryRepository.findTopByTickerOrderByTradeTimestampDesc(any(Ticker.class))).thenReturn(lastTrade);
        when(alpacaService.getMarketPrice("AAPL")).thenReturn(150.0);
        when(alpacaService.placeSellOrder("AAPL", 20)).thenReturn(mockOrder);

        tradeService.executeMartingaleStrategy(ticker, config);

        verify(alpacaService, times(1)).placeSellOrder("AAPL", 20);
        verify(tradeHistoryRepository, times(1)).save(any(TradeHistory.class));
    }

    @Test
    public void testAutoTrade_noConfigThrowsException() {
        when(configRepository.findByEnvironmentType(EnvironmentType.PAPER)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> tradeService.autoTrade(EnvironmentType.PAPER));
    }
}