package com.wealthix.service;

import com.wealthix.entity.Transaction;
import com.wealthix.repository.TransactionRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class BulkImportService {

    private static final Logger log = LoggerFactory.getLogger(BulkImportService.class);
    private static final int MAX_RECORDS = 1000;

    private final TransactionRepository transactionRepository;

    public BulkImportService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public List<Transaction> importTransactions(MultipartFile file, java.util.UUID userId) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        List<Transaction> transactions = new ArrayList<>();

        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), "UTF-8"));
             CSVParser csvParser = new CSVParser(fileReader,
                     CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

            Iterable<CSVRecord> csvRecords = csvParser.getRecords();

            int count = 0;
            for (CSVRecord csvRecord : csvRecords) {
                if (++count > MAX_RECORDS) {
                    throw new IllegalArgumentException("Maximum of " + MAX_RECORDS + " records allowed per import");
                }

                String description = sanitize(csvRecord.get("description"));
                Double amount = Double.valueOf(csvRecord.get("amount"));
                LocalDate date = LocalDate.parse(csvRecord.get("date"));
                String category = csvRecord.isMapped("category") ? csvRecord.get("category") : "MISC";

                Transaction transaction = Transaction.builder()
                        .description(description)
                        .amount(amount)
                        .transactionDate(date)
                        .category(category)
                        .userId(userId)
                        .accountType("IMPORT")
                        .build();

                transactions.add(transaction);
            }

            return transactionRepository.saveAll(transactions);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse CSV file: {}", e.getMessage());
            throw new RuntimeException("Failed to parse CSV file: " + e.getMessage());
        }
    }

    private String sanitize(String input) {
        if (input == null) return "";
        // Simple sanitization: remove HTML/Script tags to prevent XSS in UI
        return input.replaceAll("<[^>]*>", "").trim();
    }
}
