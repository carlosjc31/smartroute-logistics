package com.logistics.logistics.audit.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import com.logistics.logistics.audit.AuditAction;
import com.logistics.logistics.audit.AuditEntry;
import com.logistics.logistics.audit.application.AuditLogPort;
import com.logistics.logistics.shared.application.PageResult;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Auditoria em SQL puro: só INSERT e SELECT (UPDATE/DELETE são bloqueados por trigger na V3). */
@Component
public class AuditLogJdbcAdapter implements AuditLogPort {

  private static final TypeReference<Map<String, Object>> STATE = new TypeReference<>() {};

  private final JdbcTemplate jdbc;
  private final ObjectMapper json;

  public AuditLogJdbcAdapter(JdbcTemplate jdbc, ObjectMapper json) {
    this.jdbc = jdbc;
    this.json = json;
  }

  @Override
  public void append(AuditEntry entry) {
    jdbc.update("""
        INSERT INTO audit.audit_log (id, occurred_at, actor, action, entity_type, entity_id, before_state, after_state)
        VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb)""",
        entry.id(), entry.occurredAt(), entry.actor(), entry.action().name(), entry.entityType(), entry.entityId(),
        toJson(entry.before()), toJson(entry.after()));
  }

  @Override
  public PageResult<AuditEntry> findByEntity(String entityType, UUID entityId, int page, int size) {
    Long total = jdbc.queryForObject("SELECT count(*) FROM audit.audit_log WHERE entity_type = ? AND entity_id = ?",
        Long.class, entityType, entityId);
    List<AuditEntry> items = jdbc.query("""
        SELECT * FROM audit.audit_log WHERE entity_type = ? AND entity_id = ?
        ORDER BY occurred_at DESC, id LIMIT ? OFFSET ?""",
        rowMapper(), entityType, entityId, size, (long) page * size);
    long totalItems = total == null ? 0 : total;
    return new PageResult<>(items, page, size, totalItems, (int) Math.ceil((double) totalItems / size));
  }

  private RowMapper<AuditEntry> rowMapper() {
    return (ResultSet rs, int row) -> new AuditEntry(
        rs.getObject("id", UUID.class),
        rs.getObject("occurred_at", OffsetDateTime.class),
        rs.getString("actor"),
        AuditAction.valueOf(rs.getString("action")),
        rs.getString("entity_type"),
        rs.getObject("entity_id", UUID.class),
        fromJson(rs, "before_state"),
        fromJson(rs, "after_state"));
  }

  private String toJson(Map<String, Object> state) {
    if (state == null)
      return null;
    try {
      return json.writeValueAsString(state);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Estado de auditoria não serializável", e);
    }
  }

  private Map<String, Object> fromJson(ResultSet rs, String column) throws SQLException {
    String value = rs.getString(column);
    if (value == null)
      return null;
    try {
      return json.readValue(value, STATE);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Estado de auditoria corrompido em " + column, e);
    }
  }
}
