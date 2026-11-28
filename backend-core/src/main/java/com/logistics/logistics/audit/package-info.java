/**
 * Auditoria append-only das alterações de dados de negócio (spec 001, TRIP-010 a TRIP-013). API pública:
 * {@link com.logistics.logistics.audit.AuditService}, chamada na transação do caso de uso. Dono do schema
 * {@code audit}.
 */
@ApplicationModule(displayName = "Auditoria")
package com.logistics.logistics.audit;

import org.springframework.modulith.ApplicationModule;
