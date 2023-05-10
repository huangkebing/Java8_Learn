package com.hkb;

import org.junit.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 多线程测试类
 */
public class ThreadTest {
    public static void main(String[] args) {
        ReentrantLock lock = new ReentrantLock();
        new Thread(() -> {
            lock.lock();
            try {
                Thread.sleep(50000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            lock.unlock();
        }).start();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Thread(() -> {
            System.out.println(Thread.currentThread().isInterrupted());
            lock.lock();
            System.out.println(Thread.currentThread().isInterrupted());
            lock.unlock();
        }).start();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Thread(() -> {
            System.out.println(Thread.currentThread().isInterrupted());
            Thread.currentThread().interrupt();
            lock.lock();
            System.out.println(Thread.currentThread().isInterrupted());
            lock.unlock();
        }).start();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Thread(() -> {
            System.out.println(Thread.currentThread().isInterrupted());
            lock.lock();
            System.out.println(Thread.currentThread().isInterrupted());
            lock.unlock();
        }).start();
    }

    @Test
    public void readWriteLock(){
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

        new Thread(()-> {
            readLock.lock();
            try {
                Thread.sleep(50000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            readLock.unlock();
        }).start();


        readLock.lock();
        readLock.unlock();
    }

    @Test
    public void aliveTest(){
        Thread thread = new Thread(()-> System.out.println(1));
        System.out.println(thread.isAlive());
        thread.start();
        System.out.println(thread.isAlive());
    }

    @Test
    public void interruptTest(){
        Thread test = new Thread(() -> {
            System.out.println("thread sout");
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("thread sout 2");
        });
        System.out.println(test.isInterrupted());
        test.start();
        test.interrupt();
        System.out.println(test.isInterrupted());
    }
}