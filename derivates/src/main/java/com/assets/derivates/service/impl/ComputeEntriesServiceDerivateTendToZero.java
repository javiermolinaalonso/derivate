package com.assets.derivates.service.impl;

import java.math.BigDecimal;

import com.assets.derivates.service.ComputeEntranceService;
import com.assets.portfolio.correlation.entities.stock.StockList;

public class ComputeEntriesServiceDerivateTendToZero implements ComputeEntranceService {

    private static final Integer RANGE_TO_CHECK = 2;
    
    @Override
    public StockList computeEntrances(StockList input) {
        StockList monthDerivate = input.getMean(30).getFirstDerivate();
        StockList exits = new StockList(input.getTicker());
        
        for(int i = RANGE_TO_CHECK; i < monthDerivate.size() - RANGE_TO_CHECK; i++){
            Boolean filterSucceeded = true;
            for(int j = RANGE_TO_CHECK * -1; j < 0; j++){
                filterSucceeded = filterSucceeded && monthDerivate.get(i+j).getValue().compareTo(BigDecimal.ZERO) < 0;
            }
            if(filterSucceeded){
                for(int j = 0; j < RANGE_TO_CHECK; j++){
                    filterSucceeded = filterSucceeded && monthDerivate.get(i+j).getValue().compareTo(BigDecimal.ZERO) >= 0;
                }
            }
            
            if(filterSucceeded){
                exits.add(input.getByInstant(monthDerivate.get(i+RANGE_TO_CHECK-1).getInstant()));
            }
            
        }
        return exits;
    }

}
