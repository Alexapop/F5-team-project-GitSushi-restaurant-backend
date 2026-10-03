package dev.team1.reports;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import dev.team1.enums.OrderChannel;
import dev.team1.reports.dtos.ChannelSales;
import dev.team1.reports.dtos.SalesReport;

// Genera el PDF del resumen de ventas con OpenPDF.
@Component
public class SalesReportPdfGenerator {

    private static final Locale SPANISH = Locale.of("es", "ES");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final Color MUTED = new Color(102, 102, 102);
    private static final Color HEADER_FILL = new Color(242, 242, 242);

    private static final Map<OrderChannel, String> CHANNEL_LABELS = Map.of(
        OrderChannel.ONSITE, "Sala",
        OrderChannel.ONLINE, "A domicilio");

    private final Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private final Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 12, MUTED);
    private final Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, 10, MUTED);
    private final Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
    private final Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private final Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private final Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED);

    public byte[] generate(SalesReport report) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);

        try {
            PdfWriter.getInstance(document, output);
            document.open();

            document.add(new Paragraph("GitSushi · Resumen de ventas", titleFont));
            document.add(new Paragraph(periodText(report), subtitleFont));
            document.add(spacer());

            document.add(totalsTable(report));
            document.add(spacer());

            document.add(new Paragraph("Por canal", headerFont));
            document.add(channelsTable(report));
            document.add(spacer());

            document.add(new Paragraph(
                "Generado el " + report.generatedAt().format(DATE)
                    + " a las " + report.generatedAt().format(TIME)
                    + " · Solo pedidos pagados",
                footerFont));
        } catch (DocumentException exception) {
            throw new IllegalStateException("No se ha podido generar el PDF del resumen de ventas", exception);
        } finally {
            document.close();
        }

        return output.toByteArray();
    }

    private String periodText(SalesReport report) {
        String range = report.firstDay().equals(report.lastDay())
            ? report.firstDay().format(DATE)
            : report.firstDay().format(DATE) + " – " + report.lastDay().format(DATE);
        return "Periodo: " + report.period().getLabel() + " · " + range;
    }

    private PdfPTable totalsTable(SalesReport report) {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.addCell(labelCell("Ventas"));
        table.addCell(labelCell("Pedidos"));
        table.addCell(labelCell("Ticket medio"));
        table.addCell(valueCell(formatCurrency(report.revenue())));
        table.addCell(valueCell(String.valueOf(report.orders())));
        table.addCell(valueCell(formatCurrency(report.averageTicket())));
        return table;
    }

    private PdfPTable channelsTable(SalesReport report) {
        PdfPTable table = new PdfPTable(new float[] { 2, 1, 1 });
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        table.addCell(headerCell("Canal", Element.ALIGN_LEFT));
        table.addCell(headerCell("Pedidos", Element.ALIGN_RIGHT));
        table.addCell(headerCell("Ventas", Element.ALIGN_RIGHT));

        for (ChannelSales channel : report.channels()) {
            table.addCell(bodyCell(CHANNEL_LABELS.getOrDefault(channel.channel(), "—"), Element.ALIGN_LEFT));
            table.addCell(bodyCell(String.valueOf(channel.orders()), Element.ALIGN_RIGHT));
            table.addCell(bodyCell(formatCurrency(channel.revenue()), Element.ALIGN_RIGHT));
        }
        return table;
    }

    private PdfPCell labelCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, labelFont));
        cell.setBackgroundColor(HEADER_FILL);
        cell.setPadding(6);
        return cell;
    }

    private PdfPCell valueCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, valueFont));
        cell.setPadding(8);
        return cell;
    }

    private PdfPCell headerCell(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, headerFont));
        cell.setBackgroundColor(HEADER_FILL);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(6);
        return cell;
    }

    private PdfPCell bodyCell(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, bodyFont));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(6);
        return cell;
    }

    private Paragraph spacer() {
        return new Paragraph(" ", bodyFont);
    }

    private String formatCurrency(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(SPANISH).format(amount);
    }
}