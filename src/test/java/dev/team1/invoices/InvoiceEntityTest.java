package dev.team1.invoices;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.team1.orders.OrderEntity;

public class InvoiceEntityTest {

	@Test
	void builder_shouldSetInvoiceAmountAndPaidAt() {
		BigDecimal amount = new BigDecimal("37.50");
		Instant paidAt = Instant.parse("2026-10-01T12:00:00Z");

		InvoiceEntity invoice = InvoiceEntity.builder()
				.amount(amount)
				.paidAt(paidAt)
				.build();

		assertThat(invoice.getAmount(), is(equalTo(amount)));
		assertThat(invoice.getPaidAt(), is(equalTo(paidAt)));
		assertThat(invoice.getId(), is(equalTo(null)));
	}

	@Test
	void setters_shouldSetInvoiceNumberAndOrder() {
		InvoiceEntity invoice = InvoiceEntity.builder().build();
		UUID invoiceNumber = UUID.fromString("55555555-5555-5555-5555-555555555555");
		OrderEntity order = new OrderEntity();

		invoice.setInvoiceNumber(invoiceNumber);
		invoice.setOrder(order);

		assertThat(invoice.getInvoiceNumber(), is(equalTo(invoiceNumber)));
		assertThat(invoice.getOrder(), is(equalTo(order)));
	}
}
