package com.miri.wallet;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfers")
public class Transfer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String idempotencyKey;
    private Long fromAccountId;
    private Long toAccountId;
    private BigDecimal amount;
    private BigDecimal toAmount;
    private BigDecimal exchangeRate;
    private BigDecimal fee;
    private LocalDateTime createdAt;

    public Transfer() {
    }

    public Transfer(Long id, String idempotencyKey, Long fromAccountId, Long toAccountId,
                    BigDecimal amount, LocalDateTime createdAt) {
        this(id, idempotencyKey, fromAccountId, toAccountId, amount, amount, BigDecimal.ONE,
                BigDecimal.ZERO, createdAt);
    }

    public Transfer(Long id, String idempotencyKey, Long fromAccountId, Long toAccountId,
                    BigDecimal amount, BigDecimal toAmount, BigDecimal exchangeRate,
                    BigDecimal fee, LocalDateTime createdAt) {
        this.fee = fee;
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.toAmount = toAmount;
        this.exchangeRate = exchangeRate;
        this.createdAt = createdAt;
    }

    public BigDecimal getToAmount() {
        return toAmount;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Long getFromAccountId() {
        return fromAccountId;
    }

    public void setFromAccountId(Long fromAccountId) {
        this.fromAccountId = fromAccountId;
    }

    public Long getToAccountId() {
        return toAccountId;
    }

    public void setToAccountId(Long toAccountId) {
        this.toAccountId = toAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
