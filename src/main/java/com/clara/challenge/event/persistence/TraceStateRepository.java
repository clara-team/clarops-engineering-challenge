package com.clara.challenge.event.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TraceStateRepository extends JpaRepository<TraceStateEntity, String> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select state from TraceStateEntity state where state.traceId = :traceId")
  Optional<TraceStateEntity> lockByTraceId(@Param("traceId") String traceId);
}
