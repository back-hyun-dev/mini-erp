package com.github.backhyundev.mini_erp.stock;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    // SELECT ... FOR UPDATE 쿼리가 나가며 조회할 때 DB 락을 겁니다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.id = :id")
    Optional<Stock> findByIdWithPessimisticLock(@Param("id") Long id);
}