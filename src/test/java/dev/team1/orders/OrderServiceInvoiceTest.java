package dev.team1.orders;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.team1.contracts.IInvoiceService;
import dev.team1.delivery.dtos.DeliveryConfirmationDTORequest;
import dev.team1.enums.OrderChannel;
import dev.team1.enums.OrderStatus;
import dev.team1.enums.PaymentMethod;
import dev.team1.enums.PaymentStatus;

// GS-51: cobrar un pedido genera su factura. Separado de OrderServiceTest para
// que cada archivo de test cubra una sola responsabilidad.
@ExtendWith(MockitoExtension.class)
class OrderServiceInvoiceTest {

    private static final Long ORDER_ID = 1L;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private IInvoiceService invoiceService;

    @InjectMocks
    private OrderService service;

    // Pago con tarjeta online (o cobro en sala): al confirmarse se genera la factura.
    @Test
    void markAsPaidCreatesInvoiceForThePaidOrder() {
        OrderEntity order = order(OrderStatus.PLACED, PaymentMethod.ONLINE_CARD);
        order.setChannel(OrderChannel.ONLINE);
        order.setPaymentStatus(PaymentStatus.PENDING_ONLINE_PAYMENT);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        service.markAsPaid(ORDER_ID);

        verify(invoiceService).createForPaidOrder(order);
    }

    @Test
    void markAsPaidDoesNotCreateInvoiceAgainWhenOrderIsAlreadyPaid() {
        OrderEntity order = order(OrderStatus.PAID, PaymentMethod.ONLINE_CARD);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        service.markAsPaid(ORDER_ID);

        verify(invoiceService, never()).createForPaidOrder(any(OrderEntity.class));
    }

    // Efectivo a la entrega: el pago ocurre cuando el repartidor confirma el cobro.
    @Test
    void markAsDeliveredWithCashCollectedCreatesInvoiceAndSetsPaidAt() {
        OrderEntity order = order(OrderStatus.ONTHEWAY, PaymentMethod.CASH_ON_DELIVERY);
        order.setPaymentStatus(PaymentStatus.PENDING_CASH_ON_DELIVERY);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        service.markAsDelivered(ORDER_ID, new DeliveryConfirmationDTORequest(true));

        assertNotNull(order.getPaidAt());
        verify(invoiceService).createForPaidOrder(order);
    }

    // Si se pagó online, la factura ya se creó al pagar: entregar no crea otra.
    @Test
    void markAsDeliveredWithoutCashOnDeliveryDoesNotCreateInvoice() {
        OrderEntity order = order(OrderStatus.ONTHEWAY, PaymentMethod.ONLINE_CARD);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        service.markAsDelivered(ORDER_ID, null);

        verify(invoiceService, never()).createForPaidOrder(any(OrderEntity.class));
    }

    private OrderEntity order(OrderStatus status, PaymentMethod paymentMethod) {
        OrderEntity order = new OrderEntity();
        order.setStatus(status);
        order.setPaymentMethod(paymentMethod);
        return order;
    }
}