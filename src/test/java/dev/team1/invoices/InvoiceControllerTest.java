package dev.team1.invoices;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.team1.contracts.IInvoiceService;
import dev.team1.enums.OrderChannel;
import dev.team1.enums.OrderStatus;
import dev.team1.enums.PaymentMethod;
import dev.team1.invoices.dtos.InvoiceDTORequest;
import dev.team1.invoices.dtos.InvoiceDTOResponse;
import dev.team1.invoices.dtos.PaidInvoiceDTOResponse;
import dev.team1.security.JwtFilter;
import dev.team1.security.SecurityConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;


@WebMvcTest(controllers = InvoiceController.class)
@Import(SecurityConfiguration.class)
class InvoiceControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private IInvoiceService invoiceService;

  @MockitoBean
  private JwtFilter jwtFilter;

  @BeforeEach
  void bypassJwtFilter() throws Exception {
    doAnswer(invocation -> {
      ServletRequest request = invocation.getArgument(0);
      ServletResponse response = invocation.getArgument(1);
      FilterChain chain = invocation.getArgument(2);
      chain.doFilter(request, response);
      return null;
    }).when(jwtFilter).doFilter(any(), any(), any());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_shouldReturnCreatedInvoice() throws Exception {
    UUID invoiceNumber = UUID.fromString("33333333-3333-3333-3333-333333333333");
    InvoiceDTOResponse invoice = InvoiceDTOResponse.builder()
        .id(3L)
        .orderId(13L)
        .invoiceNumber(invoiceNumber)
        .amount(new BigDecimal("31.25"))
        .paidAt(Instant.parse("2026-10-01T14:00:00Z"))
        .build();
    when(invoiceService.create(any(InvoiceDTORequest.class))).thenReturn(invoice);

    mockMvc.perform(post("/api/v1/invoices")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"invoiceNumber":"33333333-3333-3333-3333-333333333333","amount":31.25,"paidAt":"2026-10-01T14:00:00Z"}
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(3))
        .andExpect(jsonPath("$.invoiceNumber").value(invoiceNumber.toString()))
        .andExpect(jsonPath("$.amount").value(31.25));

    verify(invoiceService).create(any(InvoiceDTORequest.class));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void findById_shouldReturnInvoice() throws Exception {
    Long invoiceId = 4L;
    InvoiceDTOResponse invoice = InvoiceDTOResponse.builder()
        .id(invoiceId)
        .orderId(14L)
        .invoiceNumber(UUID.fromString("44444444-4444-4444-4444-444444444444"))
        .amount(new BigDecimal("42.00"))
        .paidAt(Instant.parse("2026-10-01T15:00:00Z"))
        .build();
    when(invoiceService.findById(invoiceId)).thenReturn(invoice);

    mockMvc.perform(get("/api/v1/invoices/{id}", invoiceId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(4))
        .andExpect(jsonPath("$.orderId").value(14))
        .andExpect(jsonPath("$.amount").value(42.00));

    verify(invoiceService).findById(invoiceId);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void findAll_shouldReturnInvoicesPage() throws Exception {
    InvoiceDTOResponse invoice = InvoiceDTOResponse.builder()
        .id(1L)
        .orderId(12L)
        .invoiceNumber(UUID.fromString("11111111-1111-1111-1111-111111111111"))
        .amount(new BigDecimal("24.50"))
        .paidAt(Instant.parse("2026-10-01T12:00:00Z"))
        .build();
    Page<InvoiceDTOResponse> invoices = new PageImpl<>(List.of(invoice));
    when(invoiceService.findAll(any(Pageable.class))).thenReturn(invoices);

    mockMvc.perform(get("/api/v1/invoices"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(1))
        .andExpect(jsonPath("$.content[0].orderId").value(12))
        .andExpect(jsonPath("$.content[0].amount").value(24.50))
        .andExpect(jsonPath("$.totalElements").value(1));

    verify(invoiceService).findAll(any(Pageable.class));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void findPaid_shouldReturnPaidInvoicesPage() throws Exception {
    PaidInvoiceDTOResponse invoice = new PaidInvoiceDTOResponse(
        2L,
        UUID.fromString("22222222-2222-2222-2222-222222222222"),
        "Ana Perez",
        4,
        OrderChannel.ONSITE,
        new BigDecimal("18.00"),
        OrderStatus.PAID,
        PaymentMethod.CARD_ONSITE,
        Instant.parse("2026-10-01T13:00:00Z"));
    Page<PaidInvoiceDTOResponse> invoices = new PageImpl<>(List.of(invoice));
    when(invoiceService.findPaid(any(Pageable.class))).thenReturn(invoices);

    mockMvc.perform(get("/api/v1/invoices/paid"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(2))
        .andExpect(jsonPath("$.content[0].customerName").value("Ana Perez"))
        .andExpect(jsonPath("$.content[0].tableNumber").value(4))
        .andExpect(jsonPath("$.content[0].status").value("PAID"))
        .andExpect(jsonPath("$.totalElements").value(1));

    verify(invoiceService).findPaid(any(Pageable.class));
  }

  @Test
  @WithMockUser(roles = "CUSTOMER")
  void findAll_shouldRejectNonAdmin() throws Exception {
    mockMvc.perform(get("/api/v1/invoices"))
        .andExpect(status().isForbidden());
  }
}