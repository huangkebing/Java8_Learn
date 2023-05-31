package com.hkb.util;

import org.junit.Test;

import java.util.*;

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
    public void subListTest(){
        ArrayList<Integer> inners = new ArrayList<>();
        inners.add(1);
        inners.add(2);
        inners.add(3);
        inners.add(4);
        inners.add(5);
        List<Integer> list = inners.subList(0, 3);
        System.out.println(list.getClass());
        Set<Integer> set = new HashSet<>();
        set.add(1);
        list.removeAll(set);
        System.out.println(list);
        System.out.println(inners);
    }
}
