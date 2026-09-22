package dev.team1.orders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import dev.team1.enums.OrderChannel;
import dev.team1.enums.OrderStatus;
import dev.team1.enums.PaymentMethod;
import dev.team1.orders.dtos.OrderDTORequest;
import dev.team1.orders.dtos.OrderDTOResponse;
import dev.team1.security.SecurityConfiguration;

@WebMvcTest(controllers = OrderController.class, properties = "api-endpoint=api/v1")
@Import(SecurityConfiguration.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService service;

    @Test
    void createOrderReturnsCreatedOrder() throws Exception {
        OrderDTORequest request = new OrderDTORequest(
                List.of(new OrderDTORequest.OrderItemDTORequest(2L, 2)),
                "No onions", OrderChannel.ONSITE, PaymentMethod.CREDITCARD);
        when(service.createOrder(request, "tablet-12")).thenReturn(response(OrderStatus.PLACED));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Device-Identifier", "tablet-12")
                        .content("""
                                {"items":[{"productId":2,"quantity":2}],"chefNote":"No onions","channel":"ONSITE","paymentMethod":"CREDITCARD"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PLACED"))
                                .andExpect(jsonPath("$.channel").value("ONSITE"))
                                .andExpect(jsonPath("$.paymentMethod").value("CREDITCARD"))
                                .andExpect(jsonPath("$.tableNumber").value(12))
                .andExpect(jsonPath("$.total").value(22.0));
        verify(service).createOrder(request, "tablet-12");
    }

    @Test
    void createOrderPassesDeviceIdentifierToService() throws Exception {
        OrderDTORequest request = new OrderDTORequest(
                List.of(new OrderDTORequest.OrderItemDTORequest(2L, 1)),
                null, OrderChannel.ONSITE, PaymentMethod.CASH);
        when(service.createOrder(request, "tablet-7")).thenReturn(response(OrderStatus.PLACED));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Device-Identifier", "tablet-7")
                        .content("""
                                {"items":[{"productId":2,"quantity":1}],"channel":"ONSITE","paymentMethod":"CASH"}
                                """))
                .andExpect(status().isCreated());

        verify(service).createOrder(request, "tablet-7");
    }

    @Test
    void createOnlineOrderPassesMissingDeviceIdentifierToService() throws Exception {
        OrderDTORequest request = new OrderDTORequest(
                List.of(new OrderDTORequest.OrderItemDTORequest(2L, 1)),
                null, OrderChannel.ONLINE, PaymentMethod.CREDITCARD);
        when(service.createOrder(request, null)).thenReturn(response(OrderStatus.PLACED));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"productId":2,"quantity":1}],"channel":"ONLINE","paymentMethod":"CREDITCARD"}
                                """))
                .andExpect(status().isCreated());

        verify(service).createOrder(request, null);
    }

    @Test
    void createOnsiteOrderReturnsBadRequestWhenDeviceIdentifierIsMissing() throws Exception {
        OrderDTORequest request = new OrderDTORequest(
                List.of(new OrderDTORequest.OrderItemDTORequest(2L, 1)),
                null, OrderChannel.ONSITE, PaymentMethod.CASH);
        when(service.createOrder(request, null)).thenThrow(new ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "Device identifier is required"));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"productId":2,"quantity":1}],"channel":"ONSITE","paymentMethod":"CASH"}
                                """))
                .andExpect(status().isBadRequest());

        verify(service).createOrder(request, null);
    }

    @Test
    void createOnsiteOrderReturnsNotFoundWhenDeviceIsUnknown() throws Exception {
        OrderDTORequest request = new OrderDTORequest(
                List.of(new OrderDTORequest.OrderItemDTORequest(2L, 1)),
                null, OrderChannel.ONSITE, PaymentMethod.CASH);
        when(service.createOrder(request, "unknown-device")).thenThrow(new ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "No table found for the given device."));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Device-Identifier", "unknown-device")
                        .content("""
                                {"items":[{"productId":2,"quantity":1}],"channel":"ONSITE","paymentMethod":"CASH"}
                                """))
                .andExpect(status().isNotFound());

        verify(service).createOrder(request, "unknown-device");
    }

        @Test
        void createOrderRejectsMissingChannel() throws Exception {
                mockMvc.perform(post("/api/v1/orders")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {"items":[{"productId":2,"quantity":2}],"paymentMethod":"CASH"}
                                                                """))
                                .andExpect(status().isBadRequest());
                verifyNoInteractions(service);
        }

        @Test
        void createOrderRejectsMissingPaymentMethod() throws Exception {
                mockMvc.perform(post("/api/v1/orders")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {"items":[{"productId":2,"quantity":2}],"channel":"ONSITE"}
                                                                """))
                                .andExpect(status().isBadRequest());
                verifyNoInteractions(service);
        }

        @Test
        void createOrderRejectsInvalidEnumValue() throws Exception {
                mockMvc.perform(post("/api/v1/orders")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {"items":[{"productId":2,"quantity":2}],"channel":"RESTAURANT","paymentMethod":"CASH"}
                                                                """))
                                .andExpect(status().isBadRequest());
                verifyNoInteractions(service);
        }

    @Test
    void markAsPaidReturnsPaidOrder() throws Exception {
        when(service.markAsPaid(1L)).thenReturn(response(OrderStatus.PAID));

        mockMvc.perform(patch("/api/v1/orders/1/paid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PAID"));
        verify(service).markAsPaid(1L);
    }

    @Test
    void getByIdReturnsOrderStatus() throws Exception {
        when(service.getById(1L)).thenReturn(response(OrderStatus.PAID));

        mockMvc.perform(get("/api/v1/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PAID"));
        verify(service).getById(1L);
    }

    @Test
    void getByStatusReturnsPaidOrders() throws Exception {
        when(service.getByStatus(OrderStatus.PAID))
                .thenReturn(List.of(response(OrderStatus.PAID)));

        mockMvc.perform(get("/api/v1/orders").param("status", "PAID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("PAID"));
        verify(service).getByStatus(OrderStatus.PAID);
    }

    private OrderDTOResponse response(OrderStatus orderStatus) {
        return new OrderDTOResponse(1L, new BigDecimal("20.00"), null,
                new BigDecimal("0.00"), 10, new BigDecimal("22.00"),
                new BigDecimal("2.00"), "No onions", orderStatus,
                OrderChannel.ONSITE, PaymentMethod.CREDITCARD, 12);
    }
}
