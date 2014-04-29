package com.assets.derivates.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import com.assets.derivates.service.ComputeExitsService;
import com.assets.portfolio.correlation.entities.FactoryStatisticList;
import com.assets.portfolio.correlation.entities.StatisticList;
import com.assets.portfolio.correlation.entities.enums.StatisticListType;
import com.assets.portfolio.correlation.entities.stock.StockList;
import com.assets.portfolio.correlation.entities.stock.StockPrice;

public class ComputeExitsServiceBasicPercent implements ComputeExitsService {

    private static final BigDecimal PERCENT_UP = BigDecimal.valueOf(10);
    
    @Override
    public StockList computeExits(StockList entryPoints, StockList input) {
        return computeExits(entryPoints, input, PERCENT_UP);
    }

    private StockList computeExits(StockList entryPoints, StockList input,BigDecimal percentUp) {
        StockList exits = new StockList(input.getTicker());
        StockList currentEntries = new StockList(input.getTicker());
        
        StockPrice previousEntry = entryPoints.get(0);
        currentEntries.add(previousEntry);
        for(int i = 1; i < entryPoints.size(); i++){
            
            StockPrice currentEntry = entryPoints.get(i);
            StockPrice exitPrice = computeExit(currentEntries, input, percentUp);
            
            if(exitPrice != null && exitPrice.getInstant().isBefore(currentEntry.getInstant())){
                exits.add(exitPrice);
                currentEntries.clear();
            }
            
            currentEntries.add(currentEntry);
        }
        
        return exits;
    }

    private StockPrice computeExit(StockList currentEntries, StockList input, BigDecimal percentUp) {
        List<BigDecimal> entriesValue = currentEntries.stream().map(x -> x.getValue()).collect(Collectors.toList());
        StatisticList<BigDecimal> sl = FactoryStatisticList.getStatisticList(entriesValue, StatisticListType.LAMBDA);
        BigDecimal targetPrice = sl.getMean().add(sl.getMean().multiply(percentUp.divide(BigDecimal.valueOf(100))));
        Instant from = currentEntries.getLast().getInstant();
        
        for(StockPrice current : input){
            if(current.getInstant().compareTo(from) > 0 && current.getValue().compareTo(targetPrice) >= 0){
                return new StockPrice(current.getTicker(), current.getInstant(), targetPrice);
            }
        }
        
        return null;
    }

}
