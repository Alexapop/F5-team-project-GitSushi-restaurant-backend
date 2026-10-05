package dev.team1.orders;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import dev.team1.orders_products.OrderProductId;

public class OrderProductIdTest {

    @Test
    public void testOrderProductId() {

        OrderProductId oPIdEmpty = new OrderProductId();
        OrderProductId oPId = new OrderProductId(1L, 1L);
        OrderProductId oPIdOther = new OrderProductId(2L, 2L);
        OrderProductId oPIdSame = new OrderProductId(1L, 1L);

        assertThat(oPIdEmpty, is(instanceOf(OrderProductId.class)));
        assertThat(oPIdEmpty.getOrderId(), is(equalTo(null)));
        assertThat(oPIdEmpty.getProductId(), is(equalTo(null)));
        
        assertThat(oPId, is(instanceOf(OrderProductId.class)));
        assertThat(oPId.getOrderId(), is(equalTo(1L)));
        assertThat(oPId.getProductId(), is(equalTo(1L)));
        
        assertThat(oPId.equals(oPIdOther), is(equalTo(false)));
        assertThat(oPId.equals(oPIdSame), is(equalTo(true)));
        assertThat(oPId.hashCode(), is(equalTo(oPIdSame.hashCode())));

    }
    


}
