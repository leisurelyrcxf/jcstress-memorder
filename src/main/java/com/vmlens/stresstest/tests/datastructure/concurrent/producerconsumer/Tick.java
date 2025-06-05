package com.vmlens.stresstest.tests.datastructure.concurrent.producerconsumer;

/**
 * @author sadtheslayer
 */
public class Tick {
    private final String market;
    private final double price;

    public Tick(String market, double price) {
        this.market = market;
        this.price = price;
    }

    public String getMarket() {
        return market;
    }

    public double getPrice() {
        return price;
    }
}