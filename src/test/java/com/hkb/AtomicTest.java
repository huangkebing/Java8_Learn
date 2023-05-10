package com.hkb;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.LongAdder;

/**
 * 原子类测试类
 */
public class AtomicTest {
    @Test
    public void atomicArrayTest(){
        int[] array = new int[2];
        array[0] = 1;
        array[1] = 2;
        AtomicIntegerArray atomicIntegerArray = new AtomicIntegerArray(array);
        System.out.println(atomicIntegerArray.get(0));
    }

    @Test
    public void updaterTest(){
        AtomicIntegerFieldUpdater<Detail> count = AtomicIntegerFieldUpdater.newUpdater(Detail.class, "count");
        Detail detail = new Detail();
        detail.count = 5;
        System.out.println(count.addAndGet(detail, 5));
    }

    @Test
    public void longAdderTest(){
        LongAdder longAdder = new LongAdder();
        longAdder.increment();
        System.out.println(longAdder.longValue());
        longAdder.reset();
        System.out.println(longAdder.longValue());
    }
}

class Detail{
    protected volatile int count;
}
