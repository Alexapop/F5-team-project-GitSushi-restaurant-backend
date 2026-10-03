package dev.team1.orders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jsoup.Jsoup;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import dev.team1.delivery.dtos.DeliveryAddressDTORequest;
import dev.team1.delivery.dtos.DeliveryAddressDTOResponse;
import dev.team1.delivery.dtos.DeliveryConfirmationDTORequest;
import dev.team1.delivery.dtos.DeliveryMetricsDTOResponse;
import dev.team1.enums.OrderChannel;
import dev.team1.enums.OrderStatus;
import dev.team1.enums.PaymentMethod;
import dev.team1.enums.PaymentStatus;
import dev.team1.kitchen.dtos.KitchenChannelCountsDTOResponse;
import dev.team1.kitchen.dtos.KitchenMetricsDTOResponse;
import dev.team1.kitchen.dtos.KitchenOrderDTOResponse;
import dev.team1.kitchen.dtos.KitchenOrderDTOResponse.KitchenOrderItemDTO;
import dev.team1.mail.MailService;
import dev.team1.orders.dtos.OrderDTORequest;
import dev.team1.orders.dtos.OrderDTOResponse;
import dev.team1.orders.dtos.PendingDeliveryDTOResponse;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import dev.team1.orders_products.OrderProductEntity;
import dev.team1.products.ProductEntity;
import dev.team1.products.ProductRepository;
import dev.team1.tables.TableEntity;
import dev.team1.tables.TableRepository;
import dev.team1.tickets.dtos.TicketDTOResponse;
import dev.team1.tickets.dtos.TicketDTOResponse.TicketItemDTO;
import dev.team1.users.UserEntity;
import dev.team1.users.UserRepository;
@Service
public class OrderService {

    // Provisional business rule: product prices exclude VAT.
    private static final int VAT_RATE = 10;

 // Provisional business rule: flat delivery fee for home delivery.
    private static final BigDecimal DELIVERY_FEE = new BigDecimal("2.50");

    private static final int KITCHEN_TARGET_MINUTES = 15;

    private static final List<OrderStatus> ACTIVE_KITCHEN_STATUSES = List.of(
            OrderStatus.PLACED,
            OrderStatus.PAID,
            OrderStatus.PROCESSING,
            OrderStatus.DELAYED);

    private static final Map<OrderChannel, List<PaymentMethod>> ALLOWED_PAYMENT_METHODS = Map.of(
            OrderChannel.ONSITE, List.of(PaymentMethod.CASH_ONSITE, PaymentMethod.CARD_ONSITE),
            OrderChannel.ONLINE, List.of(PaymentMethod.ONLINE_CARD, PaymentMethod.CASH_ON_DELIVERY));

    private static final Map<PaymentMethod, PaymentStatus> PAYMENT_STATUS = Map.of(
            PaymentMethod.CASH_ONSITE, PaymentStatus.PENDING_CASH,
            PaymentMethod.CARD_ONSITE, PaymentStatus.PENDING_CARD_TERMINAL,
            PaymentMethod.ONLINE_CARD, PaymentStatus.PENDING_ONLINE_PAYMENT,
            PaymentMethod.CASH_ON_DELIVERY, PaymentStatus.PENDING_CASH_ON_DELIVERY);

    private final OrderRepository orderRepository;
    private final ProductRepository productsRepository;
    private final TableRepository tableRepository;
    private final UserRepository userRepository;
    private final MailService mailService;

    public OrderService(OrderRepository orderRepository,
            ProductRepository productsRepository,
            TableRepository tableRepository,
            UserRepository userRepository,
            MailService mailService) {
        this.orderRepository = orderRepository;
        this.productsRepository = productsRepository;
        this.tableRepository = tableRepository;
        this.userRepository = userRepository;
        this.mailService = mailService;
    }

    @Transactional
    public OrderDTOResponse createOrder(
            OrderDTORequest request,
            String deviceIdentifier,
            UUID userId) {
        validatePaymentMethod(request.channel(), request.paymentMethod());
        validateDeliveryAddress(request.channel(), request.deliveryAddress());
        String chefNote = prepareChefNote(request.chefNote());

        OrderEntity order = new OrderEntity();
        // GS-341: enlazamos el pedido con el usuario autenticado (los invitados no tienen usuario).
        if (userId != null) {
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED,
                            "Authenticated user no longer exists"));
            order.setUser(user);
        }

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
            op.setUnitPrice(product.getPrice());

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
        BigDecimal deliveryFee = request.channel() == OrderChannel.ONLINE ? DELIVERY_FEE : BigDecimal.ZERO;
        BigDecimal total = subtotalAfterDiscount.add(vatAmount).add(deliveryFee);


        // 3. Save the order and return its data.

        order.setSubtotal(subtotal);
        // Product discounts can differ, so there is no single order discount rate.
        order.setDiscountRate(null);
        order.setDiscountAmount(discountAmount);
        order.setVatRate(VAT_RATE);
        order.setVatAmount(vatAmount);
        order.setTotal(total);
        order.setChefNote(chefNote);
        order.setOrderProducts(ops);
        order.setChannel(request.channel());
        order.setPaymentMethod(request.paymentMethod());
        order.setPaymentStatus(PAYMENT_STATUS.get(request.paymentMethod()));
        order.setStatus(OrderStatus.PLACED);
        order.setTable(resolveTable(request.channel(), deviceIdentifier));
        order.setDeliveryFee(deliveryFee);
        order.setTicketAccessToken(UUID.randomUUID().toString());
        setDeliveryAddress(order, request.deliveryAddress());


        OrderEntity savedOrder = orderRepository.save(order);
        return toResponse(savedOrder);
    }

    // add method for chef note
    private String prepareChefNote(String chefNote) {
        if (chefNote == null) {
            return null;
        }

        if (chefNote.length() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chef note must not exceed 500 characters");
        }

        String cleanedNote = Jsoup.parseBodyFragment(chefNote)
                .body()
                .text();

        if (cleanedNote.isBlank()) {
            return null;
        }

        return cleanedNote;
    }

    public List<PaymentMethod> getAllowedPaymentMethods(OrderChannel channel) {
        return ALLOWED_PAYMENT_METHODS.get(channel);
    }

   private void validateDeliveryAddress (OrderChannel channel, DeliveryAddressDTORequest address){
        if (channel == OrderChannel.ONLINE && address == null){
            throw new ResponseStatusException(
                 HttpStatus.BAD_REQUEST, "Delivery address is required for online orders");
                
        }
   }
   private void setDeliveryAddress (OrderEntity order, DeliveryAddressDTORequest address){
        if (order.getChannel() != OrderChannel.ONLINE){
                return;
        }
        order.setDeliveryStreet(address.deliveryStreet().strip());
        order.setDeliveryCity(address.deliveryCity().strip());
        order.setDeliveryPostalCode(address.deliveryPostalCode().strip());
        String deliveryInstructions = address.deliveryInstructions();
        order.setDeliveryInstructions(deliveryInstructions == null || deliveryInstructions.isBlank() ? null : deliveryInstructions.strip());
   }

    // GS-562: ticket del pedido. Lo ve el admin, el usuario dueño o el invitado con su token.
    @Transactional(readOnly = true)
    public TicketDTOResponse getTicket(Long id, UUID userId, boolean isAdmin, String token) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Order not found: " + id));

        boolean isOwner = userId != null && order.getUser() != null
                && order.getUser().getId().equals(userId);
        boolean hasValidToken = token != null && token.equals(order.getTicketAccessToken());

        if (!isAdmin && !isOwner && !hasValidToken) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "You are not allowed to see this ticket");
        }

        return toTicketResponse(order);
    }

    private TicketDTOResponse toTicketResponse(OrderEntity order) {
        List<TicketItemDTO> items = order.getOrderProducts().stream()
                .map(op -> new TicketItemDTO(
                        op.getProduct().getName(),
                        op.getQuantity(),
                        op.getUnitPrice(),
                        op.getUnitPrice().multiply(op.getQuantity()).setScale(2, RoundingMode.HALF_UP)))
                .toList();

        DeliveryAddressDTOResponse deliveryAddress = order.getChannel() == OrderChannel.ONLINE
                ? new DeliveryAddressDTOResponse(
                        order.getDeliveryStreet(),
                        order.getDeliveryCity(),
                        order.getDeliveryPostalCode(),
                        order.getDeliveryInstructions())
                : null;

        return new TicketDTOResponse(
                order.getId(),
                order.getStatus(),
                order.getChannel(),
                order.getTable() == null ? null : order.getTable().getTableNumber(),
                order.getCreatedAt(),
                order.getPaidAt(),
                items,
                order.getSubtotal(),
                order.getDiscountAmount(),
                order.getVatRate(),
                order.getVatAmount(),
                order.getDeliveryFee(),
                order.getTotal(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                deliveryAddress);
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
        order.setPaidAt(LocalDateTime.now());
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

    // GS-341: un pedido con tarjeta online no se envía a cocina hasta que esté pagado.
    private boolean isAwaitingOnlinePayment(OrderEntity order) {
        return order.getPaymentMethod() == PaymentMethod.ONLINE_CARD
                && order.getStatus() == OrderStatus.PLACED;
    }

    // Incluye PAID para que el pedido pagado online aparezca en cocina,
    // y excluye los que todavía esperan el pago online.
    private List<OrderEntity> findActiveKitchenOrders() {
        return findActiveKitchenOrders(null);
    }

    // GS-686: sin canal devuelve ambos canales ("Todos").
    private List<OrderEntity> findActiveKitchenOrders(OrderChannel channel) {
        List<OrderEntity> orders;

        if (channel == null) {
            orders = orderRepository.findByStatusIn(ACTIVE_KITCHEN_STATUSES);
        } else {
            orders = orderRepository.findByStatusInAndChannel(ACTIVE_KITCHEN_STATUSES, channel);
        }

        return orders.stream()
                .filter(order -> !isAwaitingOnlinePayment(order))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<KitchenOrderDTOResponse> getActiveKitchenOrders() {
        return getActiveKitchenOrders(null);
    }

    // GS-685: filtro opcional por canal (ONSITE = sala, ONLINE = domicilio).
    @Transactional(readOnly = true)
    public List<KitchenOrderDTOResponse> getActiveKitchenOrders(OrderChannel channel) {
        List<OrderEntity> orders = findActiveKitchenOrders(channel);

        return orders.stream()
                .map(this::toKitchenResponse)
                .toList();
    }

    // GS-687: contadores de comandas activas por canal para los filtros.
    @Transactional(readOnly = true)
    public KitchenChannelCountsDTOResponse getKitchenChannelCounts() {
        List<OrderEntity> activeOrders = findActiveKitchenOrders();

        long inStore = activeOrders.stream()
                .filter(order -> order.getChannel() == OrderChannel.ONSITE)
                .count();

        long delivery = activeOrders.stream()
                .filter(order -> order.getChannel() == OrderChannel.ONLINE)
                .count();

        return new KitchenChannelCountsDTOResponse(activeOrders.size(), inStore, delivery);
    }

    @Transactional(readOnly = true)
    public KitchenMetricsDTOResponse getKitchenMetrics() {
        List<OrderEntity> activeOrders = findActiveKitchenOrders();

        long total = activeOrders.size();

        double averageMinutes = activeOrders.stream()
                .mapToLong(order -> ChronoUnit.MINUTES.between(order.getCreatedAt(), LocalDateTime.now()))
                .average()
                .orElse(0.0);

        long processingCount = activeOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.PROCESSING
                        || order.getStatus() == OrderStatus.PLACED
                        || order.getStatus() == OrderStatus.PAID)
                .count();

        long delayedCount = activeOrders.stream()
                .filter(this::isOrderDelayed)
                .count();

        long readyCount = orderRepository.findByStatus(OrderStatus.READY).size();

        return new KitchenMetricsDTOResponse(total, averageMinutes, processingCount, delayedCount,readyCount);
    }

    @Transactional(readOnly = true)
    public DeliveryMetricsDTOResponse getDeliveryMetrics() {
        long readyCount = orderRepository.findByStatus(OrderStatus.READY).size();
        long inTransitCount = orderRepository.findByStatus(OrderStatus.ONTHEWAY).size();

        List<OrderEntity> deliveredToday = orderRepository
                .findByStatusAndDeliveredAtGreaterThanEqual(
                        OrderStatus.DELIVERED, LocalDate.now().atStartOfDay());

        double averageDeliveryMinutes = deliveredToday.stream()
                .mapToLong(order -> ChronoUnit.MINUTES.between(order.getCreatedAt(), order.getDeliveredAt()))
                .average()
                .orElse(0.0);

        return new DeliveryMetricsDTOResponse(
                readyCount, inTransitCount, deliveredToday.size(), averageDeliveryMinutes);
    }

    private KitchenOrderDTOResponse toKitchenResponse(OrderEntity order) {
        List<KitchenOrderItemDTO> items = order.getOrderProducts().stream()
                .map(op -> new KitchenOrderItemDTO(
                        op.getProduct().getName(),
                        op.getQuantity()))
                .toList();

        boolean isDelayed = isOrderDelayed(order);

        String chefNote = order.getChefNote();
        boolean hasPriorityNote = false;

        if (chefNote != null && !chefNote.isBlank()) {
            hasPriorityNote = true;
        } else {
            chefNote = null;
        }

        return new KitchenOrderDTOResponse(
                order.getId(),
                order.getStatus(),
                chefNote,
                order.getCreatedAt(),
                isDelayed,
                items,
                order.getPaymentStatus(),
                hasPriorityNote,
                order.getChannel() != null ? order.getChannel().name() : null);
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

        if (isAwaitingOnlinePayment(order)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Online card payment must be confirmed before preparation");
        }

        validateKitchenStatusTransition(order.getStatus(), newStatus);

        order.setStatus(newStatus);
        OrderEntity savedOrder = orderRepository.save(order);
        return toKitchenResponse(savedOrder);
    }

    private void validateKitchenStatusTransition(OrderStatus currentStatus, OrderStatus newStatus){
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
                savedOrder.getPaymentStatus(),
                savedOrder.getDeliveryFee(),
                savedOrder.getTicketAccessToken());
    }

    // GS-607: el repartidor marca el pedido como "en camino" y se notifica al cliente por email.
    @Transactional
    public OrderDTOResponse markAsInTransit(Long id) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Order not found: " + id));

        if (order.getStatus() != OrderStatus.READY) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot mark as in transit from status: " + order.getStatus());
        }

        if (order.getDeliveryman() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Order must be assigned to a deliveryman before it can be marked as in transit");
        }

        order.setStatus(OrderStatus.ONTHEWAY);
        OrderEntity savedOrder = orderRepository.save(order);

        if (savedOrder.getUser() != null && savedOrder.getUser().getEmail() != null) {
                       mailService.sendOrderInTransitEmail(
                    savedOrder.getUser().getEmail(),
                    savedOrder.getId(),
                    savedOrder.getTicketAccessToken());
        }

        return toResponse(savedOrder);
    }

        @Transactional
    public OrderDTOResponse markAsDelivered(Long id, DeliveryConfirmationDTORequest request) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Order not found: " + id));

        if (order.getStatus() != OrderStatus.ONTHEWAY) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot mark as delivered from status: " + order.getStatus());
        }

        if (order.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY
                && !Boolean.TRUE.equals(request == null ? null : request.cashCollected())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cash collection must be confirmed for cash-on-delivery orders");
        }

        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        if (order.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY) {
            order.setPaymentStatus(null); // el repartidor ya ha cobrado
        }
        OrderEntity savedOrder = orderRepository.save(order);
        return toResponse(savedOrder);
    }
        @Transactional(readOnly = true)
    public List<PendingDeliveryDTOResponse> getPendingDeliveries() {
        List<OrderEntity> orders = orderRepository
                .findByStatusAndChannelAndDeliverymanIsNull(OrderStatus.READY, OrderChannel.ONLINE);

        return orders.stream()
                .map(order -> new PendingDeliveryDTOResponse(
                        order.getId(),
                        order.getUser() == null ? null : order.getUser().getAddress()))
                .toList();
    }

        @Transactional
    public OrderDTOResponse assignDeliveryman(Long id, UUID deliverymanId) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Order not found: " + id));

        if (order.getStatus() != OrderStatus.READY || order.getChannel() != OrderChannel.ONLINE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Order is not available for delivery assignment: " + order.getStatus());
        }

        if (order.getDeliveryman() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Order is already assigned to a deliveryman");
        }

        UserEntity deliveryman = userRepository.findById(deliverymanId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user no longer exists"));

        order.setDeliveryman(deliveryman);

        try {
            OrderEntity savedOrder = orderRepository.save(order);
            return toResponse(savedOrder);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Order was just assigned to another deliveryman");
        }
    }
}