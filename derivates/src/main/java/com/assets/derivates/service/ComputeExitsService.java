package com.assets.derivates.service;

import com.assets.portfolio.correlation.entities.stock.StockList;

public interface ComputeExitsService {

    public StockList computeExits(StockList entryPoints, StockList input);
    
}
