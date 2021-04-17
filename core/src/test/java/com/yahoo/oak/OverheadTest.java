/*
 * Copyright 2020, Verizon Media.
 * Licensed under the terms of the Apache 2.0 license.
 * Please see LICENSE file in the project root for terms.
 */

package com.yahoo.oak;

import com.yahoo.oak.common.OakCommonBuildersFactory;
import com.yahoo.oak.common.integer.OakIntSerializer;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

public class OverheadTest {
    private static final int K = 1024;
    private static final int M = K * K;
    private static final int NUM_OF_ENTRIES = 2_000_000;
    private static final int KEY_SIZE = 100;
    private static final int VALUE_SIZE = 1000;
    private static final double MAX_ON_HEAP_OVERHEAD_PERCENTAGE = 0.05;
    private static OakMap<Integer, Integer> oak;

    @Before
    public void init() {
        OakMapBuilder<Integer, Integer> builder = OakCommonBuildersFactory.getDefaultIntBuilder()
                .setChunkMaxItems(100)
                .setKeySerializer(new OakIntSerializer(KEY_SIZE))
                .setValueSerializer(new OakIntSerializer(VALUE_SIZE));

        oak = builder.build();
    }

    @Test
    public void main() {

        long heapSizeBefore = Runtime.getRuntime().totalMemory(); // Get current size of heap in bytes
        long heapFreeSizeBefore = Runtime.getRuntime().freeMemory();
        double usedHeapMemoryMBBefore = (double) (heapSizeBefore - heapFreeSizeBefore) / M;
        double usedOffHeapMemoryMBBefore = (double) (oak.getValuesMemoryManager().allocated()) / M;

        Random r = new Random();
        for (int i = 0; i < (int) Math.round(NUM_OF_ENTRIES * 0.5); ) {
            Integer key = r.nextInt(NUM_OF_ENTRIES);
            if (oak.putIfAbsent(key, 8) == null) {
                i++;
            }
        }

        System.gc();
        long heapSize = Runtime.getRuntime().totalMemory(); // Get current size of heap in bytes
        long heapFreeSize = Runtime.getRuntime().freeMemory();

        double usedHeapMemoryMB = (double) (heapSize - heapFreeSize) / M;
        double usedOffHeapMemoryMB = (double) (oak.getValuesMemoryManager().allocated()) / M;

        double heapOverhead = usedHeapMemoryMB / (usedHeapMemoryMB + usedOffHeapMemoryMB);
        System.out.println("Observed On Heap Overhead: " + heapOverhead + "\n" +
            "-------------------------------------------------------------\n" +
            "Total Memory Before " + heapSizeBefore + "   ||  Total Memory After " + heapSize + "\n" +
            "Free Memory Before " + heapFreeSizeBefore + "||  Free Memory After " + heapFreeSize
            + "\n" +
            "Used Heap Memory Before in MB " + usedHeapMemoryMBBefore
            + "||  Used Heap Memory After in MB " + usedHeapMemoryMB + "\n" +
            "Used Off-Heap Memory Before in MB " + usedOffHeapMemoryMBBefore
            + "||  Used Off-Heap Memory After in MB " + usedOffHeapMemoryMB + "\n" +
            "-------------------------------------------------------------\n");

        assert heapOverhead < MAX_ON_HEAP_OVERHEAD_PERCENTAGE;
    }
}
