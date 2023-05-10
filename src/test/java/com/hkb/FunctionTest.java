package com.hkb;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Java8 function包测试类
 */
public class FunctionTest {
    @Test
    public void consumerTest(){
        // consumer.andThen()
        ArrayList<Integer> integers = new ArrayList<>();
        integers.add(1);
        integers.add(3);
        integers.add(4);
        integers.add(2);
        integers.forEach(((Consumer<Integer>) System.out::println).andThen(x -> {
            int a = x + 4;
            System.out.println(a);
        }));

        // BiConsumer.andThen()
        HashMap<String, String> map = new HashMap<>();
        map.put("1","aaa");
        map.put("2","bbb");
        map.put("3","ccc");
        map.forEach(((BiConsumer<String,String>)(k, v) -> System.out.println(k))
                .andThen((k,v) -> System.out.println(v)));
    }

    @Test
    public void predicateTest(){
        ArrayList<Integer> integers = new ArrayList<>();
        integers.add(5);
        integers.add(6);
        integers.add(7);
        integers.add(8);
        integers.add(9);
        integers.add(10);
        integers.add(11);
        integers.add(12);
        System.out.println("-----------------------");
        List<Integer> collect1 = integers.stream()
                // Predicate.and() 配合filter实现 与操作，过滤出3和4的公倍数
                .filter(((Predicate<Integer>) i -> i % 4 == 0).and(i-> i % 3 == 0))
                .collect(Collectors.toList());
        collect1.forEach(System.out::println);
        System.out.println("-----------------------");
        List<Integer> collect2 = integers.stream()
                // Predicate.or() 配合filter实现 或操作，过滤出3的倍数和4的倍数
                .filter(((Predicate<Integer>) i -> i % 4 == 0).or(i-> i % 3 == 0))
                .collect(Collectors.toList());
        collect2.forEach(System.out::println);
        System.out.println("-----------------------");
        List<Integer> collect3 = integers.stream()
                // Predicate.negate() 配合filter实现 非操作，过滤掉3的倍数
                // 和i -> i % 3 != 0 效果相同，感觉用处不大
                .filter(((Predicate<Integer>) i -> i % 3 == 0).negate())
                .collect(Collectors.toList());
        collect3.forEach(System.out::println);
        System.out.println("-----------------------");
        List<Integer> collect4 = integers.stream()
                // isEqual() 方法实现 等值过滤
                // 和i -> i == 12 效果相同
                .filter(Predicate.isEqual(12))
                .collect(Collectors.toList());
        collect4.forEach(System.out::println);
        System.out.println("-----------------------");
    }

    @Test
    public void FunctionInterfaceTest(){
        ArrayList<Integer> integers = new ArrayList<>();
        integers.add(5);
        integers.add(6);
        integers.add(7);
        System.out.println("-----------------------");
        // Function.apply()
        List<String> collect1 = integers.stream().map(String::valueOf).collect(Collectors.toList());
        collect1.forEach(System.out::println);
        System.out.println("-----------------------");
        // Function.compose() 在apply方法执行前，先执行compose方法
        List<String> collect2 = integers.stream()
                .map(((Function<Integer, String>) String::valueOf).compose(i -> i += 1))
                .collect(Collectors.toList());
        collect2.forEach(System.out::println);
        System.out.println("-----------------------");
        // Function.andThen() 在apply方法后执行
        List<String> collect3 = integers.stream()
                .map(((Function<Integer, Integer>) i -> i += 1).andThen(String::valueOf))
                .collect(Collectors.toList());
        collect3.forEach(System.out::println);
        System.out.println("-----------------------");
        // compose 和 andThen也可以组合使用
        List<String> collect4 = integers.stream()
                .map(((Function<Integer, Integer>) i -> i += 1).andThen(String::valueOf).compose(i -> i + 1))
                .collect(Collectors.toList());
        collect4.forEach(System.out::println);
        System.out.println("-----------------------");
    }

}
