package com.vmlens.stresstest.tests.datastructure.concurrent.producerconsumer;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author sadtheslayer
 */
@AllArgsConstructor
public class Tick {
    @Getter
    private final String market;
    @Getter
    private final double price;
}