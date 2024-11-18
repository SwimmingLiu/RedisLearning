package com.swimmingliu.redislearning;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class DemoTest {
        @Test
    void lambdaTest(){
        // 示例 1：使用 Lambda 遍历列表
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie");

        // 使用 Lambda 表达式打印每个名字
        names.forEach(name -> System.out.println("Hello, " + name));

        // 示例 2：使用 Lambda 表达式实现自定义逻辑
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);

        // 使用 Lambda 表达式计算每个数字的平方并打印
        numbers.forEach(num -> System.out.println(num + " squared is " + (num * num)));

        // 示例 3：使用 Lambda 表达式进行排序
        List<String> unsortedNames = Arrays.asList("Charlie", "Alice", "Bob");

        // 使用 Lambda 表达式按字母顺序排序
        unsortedNames.sort((a, b) -> a.compareTo(b));

        // 打印排序后的列表
        System.out.println("Sorted names: " + unsortedNames);
    }
}
