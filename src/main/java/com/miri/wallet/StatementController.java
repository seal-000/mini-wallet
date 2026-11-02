package com.miri.wallet;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statements")
public class StatementController {
    private final StatementService statementService;
    private final UserRepository userRepository;

    public StatementController(StatementService statementService, UserRepository userRepository) {
        this.statementService = statementService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public StatementPage statement(
            Authentication authentication,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        var user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        return statementService.page(user.getId(), accountId, type, from, to, page, size);
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportCsv(
            Authentication authentication,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        var lines = statementService.all(user.getId(), accountId, type, from, to);
        var csv = new StringBuilder("Date,Description,Type,Amount,Currency\r\n");
        for (var line : lines) {
            csv.append(String.join(",", List.of(
                    cell(line.date().toString()), cell(line.description()), cell(line.type()),
                    cell(line.amount().toPlainString()), cell(line.currency())))).append("\r\n");
        }
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("wallet-statement.csv").build().toString())
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String cell(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
