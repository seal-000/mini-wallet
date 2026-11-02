package com.miri.wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    @Query("""
            select e from LedgerEntry e
            where e.accountId in :accountIds and e.direction in :directions
              and e.createdAt >= :from and e.createdAt < :to""")
    Page<LedgerEntry> search(@Param("accountIds") Collection<Long> accountIds,
                             @Param("directions") Collection<String> directions,
                             @Param("from") LocalDateTime from,
                             @Param("to") LocalDateTime to,
                             Pageable pageable);

    @Query("""
            select coalesce(sum(case when e.direction = 'CREDIT' then e.amount else -e.amount end), 0)
            from LedgerEntry e where e.accountId = :accountId""")
    BigDecimal balanceOf(@Param("accountId") Long accountId);
}
