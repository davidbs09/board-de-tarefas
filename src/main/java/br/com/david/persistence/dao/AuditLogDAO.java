package br.com.david.persistence.dao;

import br.com.david.persistence.entity.AuditLogEntity;
import com.mysql.cj.jdbc.StatementImpl;
import lombok.AllArgsConstructor;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static br.com.david.persistence.converter.OffsetDateTimeConverter.toOffsetDateTime;
import static br.com.david.persistence.converter.OffsetDateTimeConverter.toTimestamp;

@AllArgsConstructor
public class AuditLogDAO {

    private Connection connection;

    public AuditLogEntity insert(final AuditLogEntity entity) throws SQLException {
        var sql = "INSERT INTO AUDIT_LOG (action, entity_type, entity_id, user_id, timestamp, details) VALUES (?, ?, ?, ?, ?, ?)";
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, entity.getAction());
            statement.setString(2, entity.getEntityType());
            statement.setLong(3, entity.getEntityId());
            if (entity.getUserId() != null) {
                statement.setLong(4, entity.getUserId());
            } else {
                statement.setNull(4, java.sql.Types.BIGINT);
            }
            statement.setTimestamp(5, toTimestamp(entity.getTimestamp()));
            statement.setString(6, entity.getDetails());
            statement.executeUpdate();
            if (statement instanceof StatementImpl impl) {
                entity.setId(impl.getLastInsertID());
            }
        }
        return entity;
    }

    public List<AuditLogEntity> findByEntityIdAndType(final Long entityId, final String entityType) throws SQLException {
        var sql = """
            SELECT al.id, al.action, al.entity_type, al.entity_id, al.user_id, al.timestamp, al.details,
                   u.name as user_name
            FROM AUDIT_LOG al
            LEFT JOIN USERS u ON al.user_id = u.id
            WHERE al.entity_id = ? AND al.entity_type = ?
            ORDER BY al.timestamp DESC
            """;
        List<AuditLogEntity> logs = new ArrayList<>();
        try (var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, entityId);
            statement.setString(2, entityType);
            statement.executeQuery();
            var resultSet = statement.getResultSet();
            while (resultSet.next()) {
                var entity = new AuditLogEntity();
                entity.setId(resultSet.getLong("id"));
                entity.setAction(resultSet.getString("action"));
                entity.setEntityType(resultSet.getString("entity_type"));
                entity.setEntityId(resultSet.getLong("entity_id"));
                entity.setUserId(resultSet.getLong("user_id"));
                entity.setTimestamp(toOffsetDateTime(resultSet.getTimestamp("timestamp")));
                entity.setDetails(resultSet.getString("details"));
                logs.add(entity);
            }
        }
        return logs;
    }

    public void logAction(String action, String entityType, Long entityId, Long userId, String details) throws SQLException {
        var auditLog = new AuditLogEntity();
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setUserId(userId);
        auditLog.setTimestamp(OffsetDateTime.now());
        auditLog.setDetails(details);
        insert(auditLog);
    }

}
