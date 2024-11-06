package com.algostrategix.trade.platform.service;

import com.algostrategix.trade.platform.config.AlpacaConfig;
import com.algostrategix.trade.platform.entity.MartingaleConfig;
import com.algostrategix.trade.platform.enums.EnvironmentType;
import com.algostrategix.trade.platform.repository.MartingaleConfigRepository;
import com.algostrategix.trade.platform.scheduler.DynamicTradeScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DynamicTradeSchedulerServiceTest {

    @Mock
    private TradeService tradeService;

    @Mock
    private AlpacaService alpacaService;

    @Mock
    private MartingaleConfigRepository configRepository;

    @Mock
    private AlpacaConfig alpacaConfig;

    @Mock
    private ScheduledExecutorService scheduler;

    @InjectMocks
    private DynamicTradeScheduler dynamicTradeSchedulerService;

    private MartingaleConfig config;
    private ScheduledFuture<?> mockScheduledFuture;

    @BeforeEach
    public void setUp() {
        config = new MartingaleConfig();
        config.setFrequency(1000L);
        config.setEnvironmentType(EnvironmentType.PAPER);

        mockScheduledFuture = mock(ScheduledFuture.class);
        when(alpacaConfig.getEnvironmentType()).thenReturn(EnvironmentType.PAPER);
        lenient().when(configRepository.findByEnvironmentType(EnvironmentType.PAPER)).thenReturn(Optional.of(config));
    }

    @Test
    public void testInitSchedulesTask() {
        doReturn(mockScheduledFuture)
                .when(scheduler)
                .scheduleWithFixedDelay(any(Runnable.class), eq(0L), eq(1000L), eq(TimeUnit.MILLISECONDS));


        dynamicTradeSchedulerService.init();

        verify(scheduler, times(1)).scheduleWithFixedDelay(any(Runnable.class), eq(0L), eq(1000L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    public void testScheduleTask_MarketOpen() throws Exception {
        when(alpacaService.isMarketOpen()).thenReturn(true);
        doReturn(mockScheduledFuture)
                .when(scheduler)
                .scheduleWithFixedDelay(any(Runnable.class), eq(0L), eq(1000L), eq(TimeUnit.MILLISECONDS));


        dynamicTradeSchedulerService.scheduleTask();

        verify(tradeService, times(1)).autoTrade(EnvironmentType.PAPER);
    }

    @Test
    public void testScheduleTask_MarketClosed() throws Exception {
        when(alpacaService.isMarketOpen()).thenReturn(false);
        when(alpacaService.isExtendedHours()).thenReturn(false);

        dynamicTradeSchedulerService.scheduleTask();

        verify(tradeService, never()).autoTrade(any(EnvironmentType.class));
    }

    @Test
    public void testUpdateFrequency_ValidFrequency() {
        long newFrequency = 2000L;

        doReturn(mockScheduledFuture)
                .when(scheduler)
                .scheduleWithFixedDelay(any(Runnable.class), eq(0L), eq(2000L), eq(TimeUnit.MILLISECONDS));


        dynamicTradeSchedulerService.updateFrequency(newFrequency);

        verify(configRepository, times(1)).save(config);
        verify(scheduler, times(1)).scheduleWithFixedDelay(any(Runnable.class), eq(0L), eq(newFrequency), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    public void testUpdateFrequency_InvalidFrequency() {
        long invalidFrequency = -1000L;

        dynamicTradeSchedulerService.updateFrequency(invalidFrequency);

        verify(scheduler, never()).scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
        verify(configRepository, never()).save(any(MartingaleConfig.class));
    }

    @Test
    public void testGetCurrentFrequency() {
        long frequency = dynamicTradeSchedulerService.getCurrentFrequency();
        assertEquals(1000L, frequency);
    }

    @Test
    public void testGetCurrentFrequency_NoConfigThrowsException() {
        when(configRepository.findByEnvironmentType(EnvironmentType.PAPER)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> dynamicTradeSchedulerService.getCurrentFrequency());
    }
}
