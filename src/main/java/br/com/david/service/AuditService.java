
package br.com.david.service;

import br.com.david.persistence.dao.AuditLogDAO;
import br.com.david.persistence.entity.AuditLogEntity;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class AuditService {
	private final Connection connection;

	public AuditService(Connection connection) {
		this.connection = connection;
	}

	public List<AuditLogEntity> getHistory(Long entityId, String entityType) throws SQLException {
		AuditLogDAO dao = new AuditLogDAO(connection);
		return dao.findByEntityIdAndType(entityId, entityType);
	}
}
