package finance_tracker.controller;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import finance_tracker.model.Transaction;
import finance_tracker.model.User;
import finance_tracker.service.TransactionService;
import finance_tracker.service.UserService;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserService userService;

    private User getUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/monthly-pdf")
    public ResponseEntity<byte[]> generateMonthlyReport(
            @RequestParam(defaultValue = "0") int month,
            @RequestParam(defaultValue = "0") int year) {
        try {
            User user = getUser();
            LocalDate now = LocalDate.now();
            int reportMonth = month == 0 ? now.getMonthValue() : month;
            int reportYear = year == 0 ? now.getYear() : year;

            List<Transaction> transactions = transactionService.getTransactionsByMonth(user, reportMonth, reportYear);

            double totalIncome = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.INCOME)
                .mapToDouble(Transaction::getAmount).sum();

            double totalExpense = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                .mapToDouble(Transaction::getAmount).sum();

            Map<String, Double> categorySpend = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                    Transaction::getCategory,
                    Collectors.summingDouble(Transaction::getAmount)
                ));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 40, 40, 60, 40);
            PdfWriter.getInstance(document, baos);
            document.open();

            // Colors
            BaseColor darkColor = new BaseColor(10, 10, 15);
            BaseColor goldColor = new BaseColor(201, 168, 76);
            BaseColor greenColor = new BaseColor(122, 158, 126);
            BaseColor redColor = new BaseColor(192, 84, 42);
            BaseColor grayColor = new BaseColor(138, 133, 120);
            BaseColor lightGray = new BaseColor(245, 243, 238);

            // Fonts
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD, darkColor);
            Font subtitleFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, grayColor);
            Font sectionFont = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, darkColor);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, darkColor);
            Font boldFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, darkColor);
            Font incomeFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, greenColor);
            Font expenseFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, redColor);
            Font goldFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, goldColor);

            String monthName = now.withMonth(reportMonth).format(DateTimeFormatter.ofPattern("MMMM yyyy"));

            // Header
            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setWidths(new float[]{3, 1});

            PdfPCell titleCell = new PdfPCell();
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.setPadding(10);
            titleCell.setBackgroundColor(darkColor);
            Paragraph titlePara = new Paragraph("FinTrack", titleFont);
            titlePara.getFont().setColor(BaseColor.WHITE);
            titleCell.addElement(titlePara);
            titleCell.addElement(new Paragraph("Monthly Financial Report — " + monthName, subtitleFont));
            header.addCell(titleCell);

            PdfPCell logoCell = new PdfPCell(new Phrase("💰", new Font(Font.FontFamily.HELVETICA, 32)));
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setBackgroundColor(darkColor);
            logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            header.addCell(logoCell);

            document.add(header);
            document.add(Chunk.NEWLINE);

            // Summary Cards
            PdfPTable summaryTable = new PdfPTable(3);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingAfter(20);

            addSummaryCard(summaryTable, "TOTAL INCOME", String.format("Rs %.0f", totalIncome), greenColor, lightGray);
            addSummaryCard(summaryTable, "TOTAL EXPENSES", String.format("Rs %.0f", totalExpense), redColor, lightGray);
            addSummaryCard(summaryTable, "NET BALANCE", String.format("Rs %.0f", totalIncome - totalExpense), goldColor, darkColor);

            document.add(summaryTable);

            // Category Breakdown
            if (!categorySpend.isEmpty()) {
                Paragraph catTitle = new Paragraph("Spending by Category", sectionFont);
                catTitle.setSpacingBefore(10);
                catTitle.setSpacingAfter(10);
                document.add(catTitle);

                PdfPTable catTable = new PdfPTable(3);
                catTable.setWidthPercentage(100);
                catTable.setWidths(new float[]{3, 2, 2});

                addTableHeader(catTable, new String[]{"Category", "Amount", "% of Expenses"}, darkColor);

                for (Map.Entry<String, Double> entry : categorySpend.entrySet()) {
                    double pct = totalExpense > 0 ? (entry.getValue() / totalExpense) * 100 : 0;
                    PdfPCell c1 = new PdfPCell(new Phrase(entry.getKey(), normalFont));
                    PdfPCell c2 = new PdfPCell(new Phrase(String.format("Rs %.0f", entry.getValue()), boldFont));
                    PdfPCell c3 = new PdfPCell(new Phrase(String.format("%.1f%%", pct), normalFont));
                    styleTableCell(c1); styleTableCell(c2); styleTableCell(c3);
                    c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    c3.setHorizontalAlignment(Element.ALIGN_CENTER);
                    catTable.addCell(c1); catTable.addCell(c2); catTable.addCell(c3);
                }
                document.add(catTable);
            }

            document.add(Chunk.NEWLINE);

            // Transactions Table
            Paragraph txTitle = new Paragraph("All Transactions", sectionFont);
            txTitle.setSpacingBefore(10);
            txTitle.setSpacingAfter(10);
            document.add(txTitle);

            if (transactions.isEmpty()) {
                document.add(new Paragraph("No transactions this month.", normalFont));
            } else {
                PdfPTable txTable = new PdfPTable(4);
                txTable.setWidthPercentage(100);
                txTable.setWidths(new float[]{3, 2, 2, 2});
                addTableHeader(txTable, new String[]{"Title", "Category", "Type", "Amount"}, darkColor);

                for (Transaction t : transactions) {
                    boolean isIncome = t.getType() == Transaction.TransactionType.INCOME;
                    PdfPCell c1 = new PdfPCell(new Phrase(t.getTitle(), normalFont));
                    PdfPCell c2 = new PdfPCell(new Phrase(t.getCategory(), normalFont));
                    PdfPCell c3 = new PdfPCell(new Phrase(t.getType().toString(), isIncome ? incomeFont : expenseFont));
                    PdfPCell c4 = new PdfPCell(new Phrase((isIncome ? "+" : "-") + String.format("Rs %.0f", t.getAmount()), isIncome ? incomeFont : expenseFont));
                    styleTableCell(c1); styleTableCell(c2); styleTableCell(c3); styleTableCell(c4);
                    c4.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    txTable.addCell(c1); txTable.addCell(c2); txTable.addCell(c3); txTable.addCell(c4);
                }
                document.add(txTable);
            }

            // Footer
            document.add(Chunk.NEWLINE);
            Paragraph footer = new Paragraph("Generated by FinTrack — " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")), subtitleFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "FinTrack-Report-" + monthName.replace(" ", "-") + ".pdf");

            return ResponseEntity.ok().headers(headers).body(baos.toByteArray());

        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    private void addSummaryCard(PdfPTable table, String label, String value, BaseColor valueColor, BaseColor bgColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bgColor);
        cell.setPadding(16);
        cell.setBorder(Rectangle.NO_BORDER);
        Font lFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, new BaseColor(138, 133, 120));
        Font vFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, valueColor);
        cell.addElement(new Paragraph(label, lFont));
        cell.addElement(new Paragraph(value, vFont));
        table.addCell(cell);
    }

    private void addTableHeader(PdfPTable table, String[] headers, BaseColor bgColor) {
        Font hFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, hFont));
            cell.setBackgroundColor(bgColor);
            cell.setPadding(8);
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);
        }
    }

    private void styleTableCell(PdfPCell cell) {
        cell.setPadding(8);
        cell.setBorderColor(new BaseColor(226, 221, 214));
        cell.setBorderWidth(0.5f);
    }
}   