package dev.team1.products;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

import org.junit.jupiter.api.Test;

import dev.team1.enums.ProductCategory;
import dev.team1.orders_products.OrderProductEntity;

public class ProductEntityTest {

    @Test
    void testProductEntity() {

        ProductEntity productEmpty = new ProductEntity();
        
        List<OrderProductEntity> ops = new ArrayList<>();

        ProductEntity product = new ProductEntity(
            1L,
            "name", 
            ProductCategory.BEBIDAS,
            "description", 
            "imageUrl", 
            BigDecimal.valueOf(5.0),
            BigDecimal.valueOf(0.0),
            true, 
            false, 
            ops 
        );

        assertThat(productEmpty, is(instanceOf(ProductEntity.class)));
        assertThat(product, is(instanceOf(ProductEntity.class)));
        assertThat(product.getClass().getDeclaredFields().length, is(equalTo(10)));

        assertThat(product.getName(), is(equalTo("name")));
        assertThat(product.getDescription(), is(equalTo("description")));
        assertThat(product.getPrice(), is(equalTo(BigDecimal.valueOf(5.0))));
        assertThat(product.getDiscount(), is(equalTo(BigDecimal.valueOf(0.0))));
        assertThat(product.getId(), is(equalTo(1L)));
        assertThat(product.getCategory(), is(equalTo(ProductCategory.BEBIDAS)));
        
        assertThat(product.isAvailable(), is(equalTo(true)));
        product.setAvailable(false);
        assertThat(product.isAvailable(), is(equalTo(false)));
        
        assertThat(product.isExclusive(), is(equalTo(false)));
        product.setExclusive(true);
        assertThat(product.isExclusive(), is(equalTo(true)));
    }

}
