package com.clara.challenge.event.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TraceStatusAuditRepository extends JpaRepository<TraceStatusAuditEntity, UUID> {}
