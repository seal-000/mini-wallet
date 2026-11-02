package com.miri.wallet;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);

    List<Transfer> findByFromAccountIdInOrToAccountIdInOrderByCreatedAtDescIdDesc(
            Collection<Long> fromAccountIds, Collection<Long> toAccountIds);
}
