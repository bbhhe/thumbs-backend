package com.bbhhe.thumbsbackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileWriter;
import java.io.IOException;

@SpringBootTest
class ThumbsBackendApplicationTests {

    @Test
    void contextLoads() {
    }

    public static void main(String[] args) {
        // 定义文件路径
        String filePath = "insert_users.sql";
        try (FileWriter writer = new FileWriter(filePath)) {
            // 生成 1000 条 SQL 插入语句
            for (int i = 3; i <= 5002; i++) {
                String username = "user" + i; // 生成用户名，例如 user3, user4, ..., user1002
                String sql = String.format("INSERT INTO thumbsdb.user (id, username) VALUES (%d, '%s');\n", i, username);
                writer.write(sql); // 将 SQL 语句写入文件
            }
            System.out.println("SQL 文件生成成功，路径: " + filePath);
        } catch (IOException e) {
            System.err.println("生成文件时出错: " + e.getMessage());
        }


    }

}
