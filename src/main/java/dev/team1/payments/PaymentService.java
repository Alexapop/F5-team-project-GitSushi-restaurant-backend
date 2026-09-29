package dev.team1.payments;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import dev.team1.enums.OrderChannel;
import dev.team1.enums.OrderStatus;
import dev.team1.enums.PaymentMethod;
import dev.team1.orders.OrderEntity;
import dev.team1.orders.OrderRepository;
import dev.team1.orders.OrderService;
import dev.team1.orders.dtos.OrderDTOResponse;
import dev.team1.payments.dtos.PaymentDTORequest;
import dev.team1.payments.dtos.PaymentDTOResponse;

@Service
public class PaymentService {

    private static final String CURRENCY = "eur";

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Value("${stripe.api.key:}")
    private String stripeApiKey;

    @Value("${stripe.success.url}")
    private String successUrl;

    @Value("${stripe.cancel.url}")
    private String cancelUrl;

    public PaymentService(OrderRepository orderRepository, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    // 1. Creates the Stripe payment page for an order
    @Transactional(readOnly = true)
    public PaymentDTOResponse createCheckoutSession(PaymentDTORequest request) {
        // a) find the order in the DB (the amount comes from here, NOT from the frontend)
        OrderEntity order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Order not found: " + request.orderId()));

        // b) check that this order is actually meant to be paid online
        if (order.getChannel() != OrderChannel.ONLINE
                || order.getPaymentMethod() != PaymentMethod.ONLINE_CARD) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Order must use online card payment for delivery");
        }
        if (order.getStatus() != OrderStatus.PLACED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Order cannot be paid from status: " + order.getStatus());
        }

        // logged-in customers: use the account email; guests: use the email from the request
        String email = order.getUser() != null ? order.getUser().getEmail() : request.email();

        // c) describe what Stripe should show on the payment page
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .addPaymentMethodType(
                        SessionCreateParams.PaymentMethodType.CARD)
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl)
                .setClientReferenceId(order.getId().toString())
                .setCustomerEmail(email == null || email.isBlank() ? null : email)
                .addLineItem(buildLineItem(order))
                .build();

        // d) send it to Stripe
        setStripeApiKey();
        try {
            Session session = Session.create(params);
            return new PaymentDTOResponse(
                    session.getId(),
                    session.getUrl(),
                    session.getPaymentStatus(),
                    order.getId(),
                    order.getTotal());
        } catch (StripeException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Unable to create the payment session", e);
        }
    }

    // 2. After payment, ask Stripe whether it really was paid, then mark the order as PAID
    public OrderDTOResponse confirmPayment(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Payment session ID is required");
        }

        setStripeApiKey();

        try {
            Session session = Session.retrieve(sessionId);

            if (!"paid".equals(session.getPaymentStatus())) {
                throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Payment not completed");
            }

            Long orderId;
            try {
                orderId = Long.valueOf(session.getClientReferenceId());
            } catch (NumberFormatException exception) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Payment session has no valid order reference");
            }

            if (orderId <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Payment session has no valid order reference");
            }

            OrderEntity order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Order not found: " + orderId));

            if (order.getChannel() != OrderChannel.ONLINE
                    || order.getPaymentMethod() != PaymentMethod.ONLINE_CARD) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Payment session does not reference an online card order");
            }

            Long paidAmount = session.getAmountTotal();
            long expectedAmount = toCents(order.getTotal());

            if (paidAmount == null
                    || paidAmount.longValue() != expectedAmount
                    || !CURRENCY.equals(session.getCurrency())
                    || !"payment".equals(session.getMode())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Payment session does not match the order");
            }

            return orderService.markAsPaid(orderId);
        } catch (StripeException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Unable to verify the payment session", e);
        }
    }

    // the single product line on the Stripe page: "GitSushi - Order #5", total amount
    private SessionCreateParams.LineItem buildLineItem(OrderEntity order) {
        SessionCreateParams.LineItem.PriceData.ProductData product =
                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName("GitSushi - Order #" + order.getId())
                        .build();

        SessionCreateParams.LineItem.PriceData price =
                SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency(CURRENCY)
                        .setUnitAmount(toCents(order.getTotal()))
                        .setProductData(product)
                        .build();

        return SessionCreateParams.LineItem.builder()
                .setQuantity(1L)
                .setPriceData(price)
                .build();
    }

    private void setStripeApiKey() {
        if (stripeApiKey == null || stripeApiKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Online payment is not configured");
        }
        Stripe.apiKey = stripeApiKey;
    }

    // Stripe works in cents: 12.50 € -> 1250
    private long toCents(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Order total must be greater than zero");
        }

        try {
            return amount.movePointRight(2).longValueExact();
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Order total cannot be represented in cents");
        }
    }
}
