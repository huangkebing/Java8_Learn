package com.hkb;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListTest {

    @Test
    public void toArrayReturnTest(){
        /*
            [1, 2]
            class [Ljava.lang.String;
            [1, 2]
            class [Ljava.lang.Object;
         */
        String[] array = {"1", "2"};
        /*
            Arrays中的ArrayList，实际存的是泛型E类数组，toArray方法返回的也是
            因此当执行arrayArray[0] = new Object()语句时，出现向下转型而报错
            ArrayList(E[] array) {
                a = Objects.requireNonNull(array);
            }
            @Override
            public Object[] toArray() {
                return a.clone();
            }
        */
        List<String> arrayList = Arrays.asList(array);
        Object[] arrayArray = arrayList.toArray();
        // java.lang.ArrayStoreException: java.lang.Object
        // arrayArray[0] = new Object();
        System.out.println(Arrays.toString(arrayArray));
        System.out.println(arrayArray.getClass());

        List<String> newList = new ArrayList<>();
        newList.add("1");
        newList.add("2");
        Object[] newArray = newList.toArray();
        System.out.println(Arrays.toString(newArray));
        System.out.println(newArray.getClass());
    }

    /**
     * List.toArray(T[] a)方法
     * 返回T[]，若a.length > list的size，将下标=size元素赋null
     */
    @Test
    public void toArrayTest(){
        List<String> list = new ArrayList<>();
        // List<String> list = new CopyOnWriteArrayList<>();
        list.add("1");
        list.add("2");
        list.add("3");
        String[] array1 = {"10", "11"};
        String[] array2 = {"20", "21", "22", "23", "24"};
        // [1, 2, 3]
        System.out.println(Arrays.toString(list.toArray(array1)));
        // [1, 2, 3, null, 24]
        System.out.println(Arrays.toString(list.toArray(array2)));
    }

    /**
     * label:{} 定义标签，用于逻辑跳转
     * break loop;表示结束loop所在逻辑
     *
     */
    @Test
    public void goToTest(){
        label:
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 8; j++) {
                System.out.println(j);
                if (j % 2 != 0) {
                    //continue label;
                    break label;
                }
            }
        }
    }
}
