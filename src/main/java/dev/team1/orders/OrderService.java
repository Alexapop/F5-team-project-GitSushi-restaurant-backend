package dev.team1.orders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import dev.team1.enums.OrderChannel;
import dev.team1.enums.OrderStatus;
import dev.team1.enums.PaymentMethod;
import dev.team1.enums.PaymentStatus;
import dev.team1.orders.dtos.OrderDTORequest;
import dev.team1.orders.dtos.OrderDTOResponse;
import dev.team1.orders.dtos.KitchenOrderDTOResponse;
import dev.team1.orders.dtos.KitchenMetricsDTOResponse;
import dev.team1.orders.dtos.KitchenOrderDTOResponse.KitchenOrderItemDTO;
import dev.team1.orders_products.OrderProductEntity;
import dev.team1.products.ProductEntity;
import dev.team1.products.ProductRepository;
import dev.team1.tables.TableEntity;
import dev.team1.tables.TableRepository;

@Service
public class OrderService {

    // Provisional business rule: product prices exclude VAT.
    private static final int VAT_RATE = 10;
        private static final int KITCHEN_TARGET_MINUTES = 15;

    private static final Map<OrderChannel, List<PaymentMethod>> ALLOWED_PAYMENT_METHODS = Map.of(
            OrderChannel.ONSITE, List.of(PaymentMethod.CASH_ONSITE, PaymentMethod.CARD_ONSITE),
            OrderChannel.ONLINE, List.of(PaymentMethod.ONLINE_CARD, PaymentMethod.CASH_ON_DELIVERY));

    private static final Map<PaymentMethod, PaymentStatus> PAYMENT_STATUS = Map.of(
            PaymentMethod.CASH_ONSITE, PaymentStatus.PENDING_CASH,
            PaymentMethod.CARD_ONSITE, PaymentStatus.PENDING_CARD_TERMINAL);

    private final OrderRepository orderRepository;
    private final ProductRepository productsRepository;
    private final TableRepository tableRepository;

    public OrderService(OrderRepository orderRepository,
            ProductRepository productsRepository,
            TableRepository tableRepository) {
        this.orderRepository = orderRepository;
        this.productsRepository = productsRepository;
        this.tableRepository = tableRepository;
    }

    @Transactional
    public OrderDTOResponse createOrder(OrderDTORequest request, String deviceIdentifier) {
        validatePaymentMethod(request.channel(), request.paymentMethod());

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
        order.setChannel(request.channel());
        order.setPaymentMethod(request.paymentMethod());
        order.setPaymentStatus(PAYMENT_STATUS.get(request.paymentMethod()));
        order.setStatus(OrderStatus.PLACED);
        order.setTable(resolveTable(request.channel(), deviceIdentifier));

        OrderEntity savedOrder = orderRepository.save(order);
        return toResponse(savedOrder);
    }

    public List<PaymentMethod> getAllowedPaymentMethods(OrderChannel channel) {
        return ALLOWED_PAYMENT_METHODS.get(channel);
    }

    private void validatePaymentMethod(OrderChannel channel, PaymentMethod paymentMethod) {
        if (!ALLOWED_PAYMENT_METHODS.get(channel).contains(paymentMethod)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment method " + paymentMethod + " is not allowed for channel " + channel);
        }
    }

    private TableEntity resolveTable(OrderChannel channel, String deviceIdentifier) {
        if (channel != dev.team1.enums.OrderChannel.ONSITE) {
            return null;
        }

        if (deviceIdentifier == null || deviceIdentifier.strip().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Device identifier is required for onsite orders.");
        }

        return tableRepository.findByDeviceIdentifier(deviceIdentifier.strip())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No table found for the given device."));
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

    @Transactional
    public OrderDTOResponse markAsPaid(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found: " + orderId));

        if (order.getStatus() == OrderStatus.PAID) {
            return toResponse(order);
        }

        if (order.getStatus() != OrderStatus.PLACED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Order cannot be marked as paid from status: "
                            + order.getStatus());
        }

        order.setStatus(OrderStatus.PAID);
        order.setPaymentStatus(null);
        OrderEntity savedOrder = orderRepository.save(order);
        return toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderDTOResponse getById(Long id) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found: " + id));

        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderDTOResponse> getByStatus(OrderStatus status) {
        List<OrderEntity> orders = orderRepository.findByStatus(status);

        return orders.stream()
                .map(this::toResponse)
                .toList();
    }

        @Transactional(readOnly = true)
    public List<KitchenOrderDTOResponse> getActiveKitchenOrders() {
        List<OrderEntity> orders = orderRepository.findByStatusIn(
                List.of(OrderStatus.PLACED, OrderStatus.PROCESSING, OrderStatus.DELAYED));

        return orders.stream()
                .map(this::toKitchenResponse)
                .toList();
    }

        @Transactional(readOnly = true)
    public KitchenMetricsDTOResponse getKitchenMetrics() {
        List<OrderEntity> activeOrders = orderRepository.findByStatusIn(
                List.of(OrderStatus.PLACED, OrderStatus.PROCESSING, OrderStatus.DELAYED));

        long total = activeOrders.size();

        double averageMinutes = activeOrders.stream()
                .mapToLong(order -> ChronoUnit.MINUTES.between(order.getCreatedAt(), LocalDateTime.now()))
                .average()
                .orElse(0.0);

        long processingCount = activeOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.PROCESSING || order.getStatus() == OrderStatus.PLACED)
                .count();

        long delayedCount = activeOrders.stream()
                .filter(this::isOrderDelayed)
                .count();

        long readyCount = orderRepository.findByStatus(OrderStatus.READY).size();

        return new KitchenMetricsDTOResponse(total, averageMinutes, processingCount, delayedCount, readyCount);
    }

        private KitchenOrderDTOResponse toKitchenResponse(OrderEntity order) {
        List<KitchenOrderItemDTO> items = order.getOrderProducts().stream()
                .map(op -> new KitchenOrderItemDTO(
                        op.getProduct().getName(),
                        op.getQuantity()))
                .toList();

        boolean isDelayed = isOrderDelayed(order);

        return new KitchenOrderDTOResponse(
                order.getId(),
                order.getStatus(),
                order.getChefNote(),
                order.getCreatedAt(),
                isDelayed,
                items,
                order.getPaymentStatus());
    }

    private boolean isOrderDelayed(OrderEntity order) {
        if (order.getStatus() == OrderStatus.READY
                || order.getStatus() == OrderStatus.ONTHEWAY
                || order.getStatus() == OrderStatus.DELIVERED) {
            return false;
        }

        long minutesElapsed = ChronoUnit.MINUTES.between(order.getCreatedAt(), LocalDateTime.now());
        return minutesElapsed >= KITCHEN_TARGET_MINUTES;
    }



        @Transactional
    public KitchenOrderDTOResponse updateKitchenStatus(Long id, OrderStatus newStatus) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Order not found: " + id));

        validateKitchenStatusTransition(order.getStatus(), newStatus);

        order.setStatus(newStatus);
        OrderEntity savedOrder = orderRepository.save(order);
        return toKitchenResponse(savedOrder);
    }

    private void validateKitchenStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (newStatus == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Kitchen status is required");
        }

        List<OrderStatus> allowedKitchenStatuses = List.of(
                OrderStatus.PROCESSING, OrderStatus.DELAYED, OrderStatus.READY);

        if (!allowedKitchenStatuses.contains(newStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid kitchen status: " + newStatus);
        }

        if (currentStatus == OrderStatus.DELIVERED || currentStatus == OrderStatus.ONTHEWAY) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot change kitchen status once order is " + currentStatus);
        }
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
                savedOrder.getChefNote(),
                savedOrder.getStatus(),
                savedOrder.getChannel(),
                savedOrder.getPaymentMethod(),
                savedOrder.getTable() == null ? null : savedOrder.getTable().getTableNumber(),
                savedOrder.getPaymentStatus());
    }
}
