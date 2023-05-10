package com.hkb;

import org.junit.Test;

public class LockTest {
    @Test
    public void readWriteLockInitTest(){
        int SHARED_SHIFT   = 16;
        System.out.println(SHARED_SHIFT);
        int SHARED_UNIT    = (1 << SHARED_SHIFT);
        System.out.println(SHARED_UNIT);
        int MAX_COUNT      = (1 << SHARED_SHIFT) - 1;
        System.out.println(MAX_COUNT);
        int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;
        System.out.println(EXCLUSIVE_MASK);
    }

}
