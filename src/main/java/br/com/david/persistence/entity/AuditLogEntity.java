package br.com.david.persistence.entity;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class AuditLogEntity {

    private Long id;
    private String action; // CREATE, UPDATE, MOVE, BLOCK, UNBLOCK, CANCEL, ASSIGN_USER
    private String entityType; // CARD, BOARD
    private Long entityId;
    private Long userId;
    private OffsetDateTime timestamp;
    private String details; // JSON ou texto com detalhes da ação

}
