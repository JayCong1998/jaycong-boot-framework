package io.github.jaycong.stock;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StockMapper extends BaseMapper<Stock> {
    @Update("""
            UPDATE demo_stock SET available_quantity = available_quantity - #{quantity}
            WHERE product_id = #{productId} AND available_quantity >= #{quantity}
            """)
    int deduct(@Param("productId") long productId, @Param("quantity") int quantity);
}
