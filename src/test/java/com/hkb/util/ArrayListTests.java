package com.hkb.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

public class ArrayListTests {

    @Test
    public void cloneAndToArrayTest(){
        ArrayList<Inner> inners = new ArrayList<>();
        inners.add(new Inner(1));
        Object[] objects = inners.toArray();
        // 原先的内容:[Inner{number=1}]
        // 原先的内容:[Inner{number=1}]
        System.out.println("原先的内容:" + inners);
        System.out.println("原先的内容:" + Arrays.toString(objects));
        ArrayList<Inner> clone = (ArrayList<Inner>) inners.clone();
        // 修改clone副本的中对象实例的字段
        Inner inner = clone.get(0);
        inner.setNumber(0);
        // 变更后内容:[Inner{number=0}]
        // 变更后内容:[Inner{number=0}]
        System.out.println("变更后内容:" + inners);
        System.out.println("变更后内容:" + Arrays.toString(objects));
        // 修改clone副本中的对象实例
        clone.remove(inner);
        clone.add(new Inner(2));
        // 变更后内容:[Inner{number=0}]
        // 变更后内容:[Inner{number=0}]
        System.out.println("变更后内容:" + inners);
        System.out.println("变更后内容:" + Arrays.toString(objects));
    }

    private static class Inner{
        private int number;
        public Inner(int number) {
            this.number = number;
        }
        public void setNumber(int number) {
            this.number = number;
        }
        @Override
        public String toString() {
            return "Inner{number=" + number + '}';
        }
    }

    @Test
    public void addTest(){
        Inner[] ints = {new Inner(1),new Inner(2),new Inner(3),new Inner(4),new Inner(5)};
        System.arraycopy(ints, 3, ints, 2, 2);
        ints[4].setNumber(7);
        System.out.println(Arrays.toString(ints));
    }
}
