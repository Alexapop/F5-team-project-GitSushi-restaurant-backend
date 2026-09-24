package dev.team1.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import dev.team1.enums.OrderChannel;
import dev.team1.enums.OrderStatus;
import dev.team1.enums.PaymentMethod;
import dev.team1.orders.dtos.KitchenOrderDTOResponse;
import dev.team1.orders.dtos.OrderDTORequest;
import dev.team1.orders.dtos.OrderDTOResponse;
import dev.team1.orders_products.OrderProductEntity;
import dev.team1.orders.dtos.KitchenMetricsDTOResponse;
import dev.team1.products.ProductEntity;
import dev.team1.products.ProductRepository;
import dev.team1.tables.TableEntity;
import dev.team1.tables.TableRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private TableRepository tableRepository;

    @InjectMocks
    private OrderService service;

    @Test
    void createOrderWithoutDiscountCalculatesTotalsAndSavesOrderLines() {
        ProductEntity product = product(null);
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));
        when(tableRepository.findByDeviceIdentifier("tablet-12")).thenReturn(Optional.of(table(12)));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(call -> call.getArgument(0));
        OrderDTORequest request = new OrderDTORequest(
            List.of(new OrderDTORequest.OrderItemDTORequest(2L, 2)),
            "No onions", OrderChannel.ONSITE, PaymentMethod.CREDITCARD);

        OrderDTOResponse response = service.createOrder(request, "tablet-12");

        assertEquals(new BigDecimal("20.00"), response.subtotal());
        assertEquals(new BigDecimal("0.00"), response.discountAmount());
        assertEquals(new BigDecimal("2.00"), response.vatAmount());
        assertEquals(new BigDecimal("22.00"), response.total());
        assertEquals(12, response.tableNumber());
        assertEquals(OrderStatus.PLACED, response.status());
        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(captor.capture());
        OrderEntity savedOrder = captor.getValue();
        assertEquals("No onions", savedOrder.getChefNote());
        assertEquals(OrderChannel.ONSITE, savedOrder.getChannel());
        assertEquals(PaymentMethod.CREDITCARD, savedOrder.getPaymentMethod());
        assertEquals(1, savedOrder.getOrderProducts().size());
        assertSame(product, savedOrder.getOrderProducts().get(0).getProduct());
        assertSame(savedOrder, savedOrder.getOrderProducts().get(0).getOrder());
        assertEquals(new BigDecimal("2"), savedOrder.getOrderProducts().get(0).getQuantity());
    }

    @Test
    void createOrderAppliesDiscountBeforeVat() {
        when(productRepository.findById(2L)).thenReturn(Optional.of(product(new BigDecimal("10"))));
        when(tableRepository.findByDeviceIdentifier("tablet-12")).thenReturn(Optional.of(table(12)));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(call -> call.getArgument(0));
        OrderDTORequest request = new OrderDTORequest(
            List.of(new OrderDTORequest.OrderItemDTORequest(2L, 2)),
            null, OrderChannel.ONSITE, PaymentMethod.CASH);

        OrderDTOResponse response = service.createOrder(request, "tablet-12");

        assertEquals(new BigDecimal("20.00"), response.subtotal());
        assertEquals(new BigDecimal("2.00"), response.discountAmount());
        assertEquals(new BigDecimal("1.80"), response.vatAmount());
        assertEquals(new BigDecimal("19.80"), response.total());
        assertEquals(OrderStatus.PLACED, response.status());
        assertEquals(OrderChannel.ONSITE, response.channel());
        assertEquals(PaymentMethod.CASH, response.paymentMethod());
    }

    @Test
    void createOnsiteOrderRejectsAbsentDeviceIdentifier() {
        OrderDTORequest request = onsiteRequest();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.createOrder(request, null));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    void createOnsiteOrderRejectsBlankDeviceIdentifier() {
        OrderDTORequest request = onsiteRequest();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.createOrder(request, "   "));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    void createOnsiteOrderRejectsUnknownDeviceWithoutSaving() {
        when(tableRepository.findByDeviceIdentifier("unknown-device"))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.createOrder(onsiteRequest(), "unknown-device"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    void createOnlineOrderDoesNotAssociateTable() {
        ProductEntity product = product(null);
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(call -> call.getArgument(0));
        OrderDTORequest request = new OrderDTORequest(
                List.of(new OrderDTORequest.OrderItemDTORequest(2L, 1)),
                null, OrderChannel.ONLINE, PaymentMethod.CREDITCARD);

        OrderDTOResponse response = service.createOrder(request, null);

        assertEquals(OrderChannel.ONLINE, response.channel());
        assertEquals(null, response.tableNumber());
        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(captor.capture());
        assertEquals(null, captor.getValue().getTable());
    }

    @Test
    void markAsPaidUpdatesAndSavesPlacedOrder() {
        OrderEntity order = new OrderEntity();
        order.setStatus(OrderStatus.PLACED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        OrderDTOResponse response = service.markAsPaid(1L);

        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals(OrderStatus.PAID, response.status());
        verify(orderRepository).save(order);
    }

    @Test
    void markAsPaidRejectsDeliveredOrderWithoutSaving() {
        OrderEntity order = new OrderEntity();
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.markAsPaid(1L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    private ProductEntity product(BigDecimal discount) {
        return new ProductEntity(2L, "Sushi", null, "Salmon sushi", "sushi.png",
                new BigDecimal("10.00"), discount, true, false, new ArrayList<>());
    }

    private OrderDTORequest onsiteRequest() {
        ProductEntity product = product(null);
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));
        return new OrderDTORequest(
                List.of(new OrderDTORequest.OrderItemDTORequest(2L, 1)),
                null, OrderChannel.ONSITE, PaymentMethod.CASH);
    }

    private TableEntity table(int tableNumber) {
        TableEntity table = new TableEntity();
        table.setTableNumber(tableNumber);
        table.setDeviceIdentifier("tablet-12");
        return table;
    }
        @Test
    void getActiveKitchenOrdersReturnsOrdersMappedToKitchenDTO() {
        OrderEntity order = new OrderEntity();
        order.setStatus(OrderStatus.PROCESSING);
        order.setChefNote("Extra spicy");
        order.setCreatedAt(LocalDateTime.now());
        ProductEntity product = product(null);
        OrderProductEntity op = OrderProductEntity.builder()
                .order(order).product(product).quantity(new BigDecimal("3")).build();
        order.setOrderProducts(List.of(op));

        when(orderRepository.findByStatusIn(
                List.of(OrderStatus.PLACED, OrderStatus.PROCESSING, OrderStatus.DELAYED)))
                .thenReturn(List.of(order));

        List<KitchenOrderDTOResponse> result = service.getActiveKitchenOrders();

        assertEquals(1, result.size());
        KitchenOrderDTOResponse dto = result.get(0);
        assertEquals(OrderStatus.PROCESSING, dto.status());
        assertEquals("Extra spicy", dto.chefNote());
        assertEquals(1, dto.items().size());
        assertEquals("Sushi", dto.items().get(0).productName());
        assertEquals(new BigDecimal("3"), dto.items().get(0).quantity());
    }

    @Test
    void getActiveKitchenOrdersRecentOrderIsNotDelayed() {
        OrderEntity order = new OrderEntity();
        order.setStatus(OrderStatus.PROCESSING);
        order.setCreatedAt(LocalDateTime.now());
        order.setOrderProducts(new ArrayList<>());
        when(orderRepository.findByStatusIn(any())).thenReturn(List.of(order));

        List<KitchenOrderDTOResponse> result = service.getActiveKitchenOrders();

        assertEquals(false, result.get(0).isDelayed());
    }

    @Test
    void getActiveKitchenOrdersOldOrderIsDelayed() {
        OrderEntity order = new OrderEntity();
        order.setStatus(OrderStatus.PROCESSING);
        order.setCreatedAt(LocalDateTime.now().minusMinutes(20));
        order.setOrderProducts(new ArrayList<>());
        when(orderRepository.findByStatusIn(any())).thenReturn(List.of(order));

        List<KitchenOrderDTOResponse> result = service.getActiveKitchenOrders();

        assertEquals(true, result.get(0).isDelayed());
    }

    @Test
    void updateKitchenStatusValidTransitionUpdatesAndSaves() {
        OrderEntity order = new OrderEntity();
        order.setStatus(OrderStatus.PLACED);
        order.setCreatedAt(LocalDateTime.now());
        order.setOrderProducts(new ArrayList<>());
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        KitchenOrderDTOResponse response = service.updateKitchenStatus(1L, OrderStatus.PROCESSING);

        assertEquals(OrderStatus.PROCESSING, order.getStatus());
        assertEquals(OrderStatus.PROCESSING, response.status());
        verify(orderRepository).save(order);
    }

    @Test
    void updateKitchenStatusInvalidStatusThrowsBadRequest() {
        OrderEntity order = new OrderEntity();
        order.setStatus(OrderStatus.PLACED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.updateKitchenStatus(1L, OrderStatus.PAID));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    void updateKitchenStatusDeliveredOrderThrowsConflict() {
        OrderEntity order = new OrderEntity();
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.updateKitchenStatus(1L, OrderStatus.READY));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    void updateKitchenStatusOrderNotFoundThrowsNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.updateKitchenStatus(99L, OrderStatus.READY));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
        @Test
    void getActiveKitchenOrdersReturnsEmptyListWhenNoActiveOrders() {
        when(orderRepository.findByStatusIn(any())).thenReturn(List.of());

        List<KitchenOrderDTOResponse> result = service.getActiveKitchenOrders();

        assertEquals(0, result.size());
    }

    @Test
    void updateKitchenStatusRejectsNullStatus() {
        OrderEntity order = new OrderEntity();
        order.setStatus(OrderStatus.PLACED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.updateKitchenStatus(1L, null));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    void updateKitchenStatusOntheWayOrderThrowsConflict() {
        OrderEntity order = new OrderEntity();
        order.setStatus(OrderStatus.ONTHEWAY);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.updateKitchenStatus(1L, OrderStatus.READY));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    void getKitchenMetricsReturnsCorrectCountsAndAverage() {
        OrderEntity processingOrder = new OrderEntity();
        processingOrder.setStatus(OrderStatus.PROCESSING);
        processingOrder.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        processingOrder.setOrderProducts(new ArrayList<>());

        OrderEntity delayedOrder = new OrderEntity();
        delayedOrder.setStatus(OrderStatus.PROCESSING);
        delayedOrder.setCreatedAt(LocalDateTime.now().minusMinutes(20));
        delayedOrder.setOrderProducts(new ArrayList<>());

        when(orderRepository.findByStatusIn(
                List.of(OrderStatus.PLACED, OrderStatus.PROCESSING, OrderStatus.DELAYED)))
                .thenReturn(List.of(processingOrder, delayedOrder));
        when(orderRepository.findByStatus(OrderStatus.READY)).thenReturn(List.of());

        KitchenMetricsDTOResponse metrics = service.getKitchenMetrics();

        assertEquals(2, metrics.totalActiveOrders());
        assertEquals(2, metrics.processingCount());
        assertEquals(1, metrics.delayedCount());
        assertEquals(0, metrics.readyCount());
    }

    @Test
    void getKitchenMetricsReturnsZeroWhenNoActiveOrders() {
        when(orderRepository.findByStatusIn(any())).thenReturn(List.of());
        when(orderRepository.findByStatus(OrderStatus.READY)).thenReturn(List.of());

        KitchenMetricsDTOResponse metrics = service.getKitchenMetrics();

        assertEquals(0, metrics.totalActiveOrders());
        assertEquals(0.0, metrics.averagePreparationMinutes());
        assertEquals(0, metrics.processingCount());
        assertEquals(0, metrics.delayedCount());
        assertEquals(0, metrics.readyCount());
    }

    @Test
    void getKitchenMetricsCountsReadyOrdersSeparately() {
        OrderEntity readyOrder = new OrderEntity();
        readyOrder.setStatus(OrderStatus.READY);

        when(orderRepository.findByStatusIn(any())).thenReturn(List.of());
        when(orderRepository.findByStatus(OrderStatus.READY)).thenReturn(List.of(readyOrder));

        KitchenMetricsDTOResponse metrics = service.getKitchenMetrics();

        assertEquals(1, metrics.readyCount());
        assertEquals(0, metrics.totalActiveOrders());
    }
        @Test
    void getKitchenMetricsCountsPlacedOrdersAsProcessing() {
        OrderEntity placedOrder = new OrderEntity();
        placedOrder.setStatus(OrderStatus.PLACED);
        placedOrder.setCreatedAt(LocalDateTime.now());
        placedOrder.setOrderProducts(new ArrayList<>());

        when(orderRepository.findByStatusIn(any())).thenReturn(List.of(placedOrder));
        when(orderRepository.findByStatus(OrderStatus.READY)).thenReturn(List.of());

        KitchenMetricsDTOResponse metrics = service.getKitchenMetrics();

        assertEquals(1, metrics.processingCount());
        assertEquals(0, metrics.delayedCount());
    }

    @Test
    void getKitchenMetricsCalculatesAverageForSingleOrder() {
        OrderEntity order = new OrderEntity();
        order.setStatus(OrderStatus.PROCESSING);
        order.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        order.setOrderProducts(new ArrayList<>());

        when(orderRepository.findByStatusIn(any())).thenReturn(List.of(order));
        when(orderRepository.findByStatus(OrderStatus.READY)).thenReturn(List.of());

        KitchenMetricsDTOResponse metrics = service.getKitchenMetrics();

        assertEquals(10.0, metrics.averagePreparationMinutes(), 0.5);
    }

    @Test
    void getKitchenMetricsHandlesMultipleReadyOrders() {
        OrderEntity readyOrder1 = new OrderEntity();
        readyOrder1.setStatus(OrderStatus.READY);
        OrderEntity readyOrder2 = new OrderEntity();
        readyOrder2.setStatus(OrderStatus.READY);

        when(orderRepository.findByStatusIn(any())).thenReturn(List.of());
        when(orderRepository.findByStatus(OrderStatus.READY))
                .thenReturn(List.of(readyOrder1, readyOrder2));

        KitchenMetricsDTOResponse metrics = service.getKitchenMetrics();

        assertEquals(2, metrics.readyCount());
    }
}
