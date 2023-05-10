package com.hkb;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MapTest {
    @Test
    public void tableSizeForTest(){
        HashMap<String, Object> map = new HashMap<>(17);
    }

    @Test
    public void nodeTest(){
        HashMap<String, Integer> map = new HashMap<>();
        map.put("8",1);map.put("4",2);map.put("7",3);map.put("5",4);
        // [4=2, 5=4, 7=3, 8=1]
        System.out.println(map.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList()));
        // [8=1, 4=2, 7=3, 5=4]
        System.out.println(map.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors.toList()));
        // [8=1, 7=3, 5=4, 4=2]
        System.out.println(map.entrySet().stream().sorted(Map.Entry.comparingByKey((k1, k2) -> -k1.compareTo(k2))).collect(Collectors.toList()));
        // [5=4, 7=3, 4=2, 8=1]
        System.out.println(map.entrySet().stream().sorted(Map.Entry.comparingByValue((v1, v2) -> -v1.compareTo(v2))).collect(Collectors.toList()));
        map.put("9", null);
        // java.lang.NullPointerException
        System.out.println(map.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors.toList()));
    }
}
