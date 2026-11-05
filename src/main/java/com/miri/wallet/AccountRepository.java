package com.miri.wallet;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Long>  {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);

    List<Account> findByUserIdOrderById(Long userId);

    Optional<Account> findFirstByUserIdAndCurrencyOrderById(Long userId, String currency);

    @Query("select a from Account a, User u where u.id = a.userId and u.email = :email and a.currency = :currency")
    Optional<Account> findByUserEmailAndCurrency(@Param("email") String email, @Param("currency") String currency);

}
