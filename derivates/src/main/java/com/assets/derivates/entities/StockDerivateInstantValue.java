package com.assets.derivates.entities;

import java.math.BigDecimal;
import java.time.Instant;

public class StockDerivateInstantValue {

    private final Instant instant;
    private final BigDecimal currentValue;
    private final BigDecimal shortMeanValue;
    private final BigDecimal longMeanValue;
    private final BigDecimal currentDerivate;
    private final BigDecimal shortMeanDerivate;
    private final BigDecimal longMeanDerivate;

    public StockDerivateInstantValue(Instant instant, BigDecimal currentValue, BigDecimal shortMeanValue, BigDecimal longMeanValue, BigDecimal currentDerivate,
            BigDecimal shortMeanDerivate, BigDecimal longMeanDerivate) {
        super();
        this.instant = instant;
        this.currentValue = currentValue;
        this.shortMeanValue = shortMeanValue;
        this.longMeanValue = longMeanValue;
        this.currentDerivate = currentDerivate;
        this.shortMeanDerivate = shortMeanDerivate;
        this.longMeanDerivate = longMeanDerivate;
    }

    public Instant getInstant() {
        return this.instant;
    }

    public BigDecimal getCurrentValue() {
        return this.currentValue;
    }

    public BigDecimal getShortMeanValue() {
        return this.shortMeanValue;
    }

    public BigDecimal getLongMeanValue() {
        return this.longMeanValue;
    }

    public BigDecimal getCurrentDerivate() {
        return this.currentDerivate;
    }

    public BigDecimal getShortMeanDerivate() {
        return this.shortMeanDerivate;
    }

    public BigDecimal getLongMeanDerivate() {
        return this.longMeanDerivate;
    }

}
