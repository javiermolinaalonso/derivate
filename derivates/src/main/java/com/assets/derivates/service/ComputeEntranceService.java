package com.assets.derivates.service;

import com.assets.portfolio.correlation.entities.stock.StockList;

public interface ComputeEntranceService {

    public StockList computeEntrances(StockList input);
    
}
