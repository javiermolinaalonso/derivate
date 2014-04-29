package com.assets.derivates.service;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.assets.derivates.service.impl.ComputeBasicEntranceServiceImpl;
import com.assets.derivates.service.impl.ComputeExitsServiceBasicPercent;
import com.assets.portfolio.correlation.entities.investment.InvestmentActions;
import com.assets.portfolio.correlation.entities.stock.StockList;
import com.assets.portfolio.correlation.entities.stock.StockPrice;
import com.assets.portfolio.data.loader.DataLoader;
import com.assets.portfolio.data.loader.impl.DataLoaderCsv;
import com.assets.trades.service.impl.BuyFixedAmountOfMoney;
import com.assets.trades.service.impl.BuyOneStockStrategy;

public class TestIntegrationIntel {

    private static final String TICKER = "INTC";
    private static final String FROM = "2011-01-01T00:00:00Z";
    private static final String TO = "2015-01-01T00:00:00Z";
    
    Instant from;
    Instant to;
    ComputeEntranceService computeEntranceService;
    ComputeExitsService computeExitsService;
    StockList inputData;
    
    StockList entryPoints;
    StockList exitPoints;
    @Before
    public void setUp() throws Exception {
        computeEntranceService = new ComputeBasicEntranceServiceImpl();
        computeExitsService = new ComputeExitsServiceBasicPercent();
        
        DataLoader loader = new DataLoaderCsv(TestIntegrationIntel.class.getResource("/table_intc.csv").getFile());
        from = Instant.parse(FROM);
        to = Instant.parse(TO);
        inputData = new StockList(loader.loadData().get(TICKER).parallelStream().filter(p -> (p.getInstant().compareTo(from) >= 0 && p.getInstant().compareTo(to) <= 0)).sequential().collect(Collectors.toList()), TICKER);
        entryPoints = computeEntranceService.computeEntrances(inputData);
        exitPoints = computeExitsService.computeExits(entryPoints, inputData);
    }

    @Test
    public void testBuyOneStockAtATime() {
        InvestmentActions actions = new InvestmentActions(entryPoints, exitPoints, new BuyOneStockStrategy());
        
        assertEquals(16.86d, actions.getPercentBenefit().doubleValue(), 0.001d);
        assertEquals(363.452d, actions.getAmountInvested().doubleValue(), 0.001d);
        assertEquals(201.67d, actions.getMaximumAmountInvestedInOneTradeSerie().doubleValue(), 0.1d);
        assertEquals(1, actions.getCurrentShares().intValue());
        assertEquals(18.02d, actions.getPercentBenefitSellingWith(new StockPrice(TICKER, to, new BigDecimal(25.81d))).doubleValue(), 0.1d);
    }
    
    @Test
    public void testBuyMaximumAmountAtATime() {
        InvestmentActions actions = new InvestmentActions(entryPoints, exitPoints, new BuyFixedAmountOfMoney());
        
        assertEquals(18.95d, actions.getPercentBenefit().doubleValue(), 0.001d);
        assertEquals(16810.31, actions.getAmountInvested().doubleValue(), 10d);
        assertEquals(8915.63d, actions.getMaximumAmountInvestedInOneTradeSerie().doubleValue(), 10d);
        assertEquals(42, actions.getCurrentShares().intValue());
        assertEquals(18.45d, actions.getPercentBenefitSellingWith(new StockPrice(TICKER, to, new BigDecimal(22.41d))).doubleValue(), 0.1d);
    }

}
