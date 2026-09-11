package dev.team1.orders;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import dev.team1.orders.dtos.OrderDTORequest;
import dev.team1.orders.dtos.OrderDTOResponse;
import dev.team1.orders_products.OrderProductEntity;
import dev.team1.products.ProductEntity;
import dev.team1.products.ProductRepository;

@Service
public class OrderService {

    // Provisional business rule: product prices exclude VAT.
    private static final int VAT_RATE = 10;

    private final OrderRepository orderRepository;
    private final ProductRepository productsRepository;

    public OrderService(OrderRepository orderRepository,
            ProductRepository productsRepository) {
        this.orderRepository = orderRepository;
        this.productsRepository = productsRepository;
    }

    @Transactional
    public OrderDTOResponse createOrder(OrderDTORequest request) {
        OrderEntity order = new OrderEntity();
        List<OrderProductEntity> ops = new ArrayList<>();

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;

        // 1. Calculate the subtotal and discounts from the cart.
        for (OrderDTORequest.OrderItemDTORequest item : request.items()) {
            ProductEntity product = getAvailableProduct(item.productId());
            BigDecimal quantity = BigDecimal.valueOf(item.quantity());

            OrderProductEntity op = OrderProductEntity.builder()
                .order(order)
                .product(product)
                .quantity(quantity)
                .build();
            ops.add(op);
            
            BigDecimal productSubtotal = product.getPrice().multiply(quantity)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal productDiscount = calculateDiscount(product, productSubtotal);

            subtotal = subtotal.add(productSubtotal);
            discountAmount = discountAmount.add(productDiscount);
        }

        // 2. Calculate VAT after subtracting the discounts.
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        discountAmount = discountAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotalAfterDiscount = subtotal.subtract(discountAmount);
        BigDecimal vatAmount = subtotalAfterDiscount.multiply(BigDecimal.valueOf(VAT_RATE))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = subtotalAfterDiscount.add(vatAmount);

        // 3. Save the order and return its data.
        
        order.setSubtotal(subtotal);
        // Product discounts can differ, so there is no single order discount rate.
        order.setDiscountRate(null);
        order.setDiscountAmount(discountAmount);
        order.setVatRate(VAT_RATE);
        order.setVatAmount(vatAmount);
        order.setTotal(total);
        order.setChefNote(request.chefNote());
        order.setOrderProducts(ops);

        // Order lines must be persisted once the OrderProduct mapping exists.
        OrderEntity savedOrder = orderRepository.save(order);
        return toResponse(savedOrder);
    }

    private ProductEntity getAvailableProduct(Long productId) {
        ProductEntity product = productsRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product not found: " + productId));

        if (!product.isAvailable()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Product is unavailable: " + productId);
        }

        return product;
    }

    private BigDecimal calculateDiscount(ProductEntity product, BigDecimal productSubtotal) {
        BigDecimal discountRate = product.getDiscount();

        if (discountRate == null) {
            return BigDecimal.ZERO;
        }

        if (discountRate.compareTo(BigDecimal.ZERO) < 0
                || discountRate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid product discount: " + product.getId());
        }

        return productSubtotal.multiply(discountRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private OrderDTOResponse toResponse(OrderEntity savedOrder) {
        return new OrderDTOResponse(
                savedOrder.getId(),
                savedOrder.getSubtotal(),
                savedOrder.getDiscountRate(),
                savedOrder.getDiscountAmount(),
                savedOrder.getVatRate(),
                savedOrder.getTotal(),
                savedOrder.getVatAmount(),
                savedOrder.getChefNote());
    }
}
