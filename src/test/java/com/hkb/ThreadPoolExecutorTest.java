package com.hkb;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolExecutorTest {
    public static void main(String[] args) {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 2, 5,
                TimeUnit.MINUTES, new ArrayBlockingQueue<>(500));
        pool.execute(()->{
            System.out.println(Thread.currentThread());
            System.out.println(1/1);
        });
        /*pool.execute(()->{
            System.out.println(Thread.currentThread());
            System.out.println(1/0);
        });*/
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        pool.execute(()->{
            System.out.println(Thread.currentThread());
            System.out.println(1/1);
        });
    }
}
