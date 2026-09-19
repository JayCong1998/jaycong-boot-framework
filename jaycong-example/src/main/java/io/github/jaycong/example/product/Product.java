package io.github.jaycong.example.product;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.jaycong.mybatis.model.BaseEntity;

@TableName("example_product")
public class Product extends BaseEntity {
    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
