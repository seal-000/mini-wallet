package com.miri.wallet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.security.SecureRandom;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "accounts")
public class Account {
    private static final SecureRandom RANDOM = new SecureRandom();

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    @Column(updatable = false, length = 10)
    private String accountNumber;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 3)
    private String currency;
    private BigDecimal balance;
    @Version
    private Long version;   // optimistic locking

    public Account() {
    }

    public Account(Long id, Long userId, String currency, BigDecimal balance, Long version) {
        this.id = id;
        this.userId = userId;
        this.currency = currency;
        this.balance = balance;
        this.version = version;
    }

    @PrePersist
    void assignAccountNumber() {
        if (accountNumber == null) {
            accountNumber = String.valueOf(1_000_000_000L + RANDOM.nextLong(9_000_000_000L));
        }
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}