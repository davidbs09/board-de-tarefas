package br.com.david.service;

import br.com.david.dto.BoardColumnInfoDTO;
import br.com.david.exception.CardBlockedException;
import br.com.david.exception.CardFinishedException;
import br.com.david.exception.EntityNotFoundException;
import br.com.david.persistence.dao.AuditLogDAO;
import br.com.david.persistence.dao.BlockDAO;
import br.com.david.persistence.dao.CardDAO;
import br.com.david.persistence.entity.CardEntity;
import lombok.AllArgsConstructor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static br.com.david.persistence.entity.BoardColumnKindEnum.CANCEL;
import static br.com.david.persistence.entity.BoardColumnKindEnum.FINAL;


@AllArgsConstructor
public class CardService {

    private final Connection connection;

    public CardEntity create(final CardEntity entity) throws SQLException {
        return create(entity, null);
    }

    public CardEntity create(final CardEntity entity, final Long userId) throws SQLException {
        try {
            var dao = new CardDAO(connection);
            dao.insert(entity);

            // Log da ação
            var auditDao = new AuditLogDAO(connection);
            auditDao.logAction("CREATE", "CARD", entity.getId(), userId,
                "Criado card: " + entity.getTitle());

            connection.commit();
            return entity;
        } catch (SQLException ex){
            connection.rollback();
            throw ex;
        }
    }

    public void assignUser(final Long cardId, final Long userId, final Long actionUserId) throws SQLException {
        try {
            var dao = new CardDAO(connection);
            dao.updateAssignedUser(cardId, userId);

            // Log da ação
            var auditDao = new AuditLogDAO(connection);
            String details = userId != null ? "Atribuído usuário ID: " + userId : "Removido responsável";
            auditDao.logAction("ASSIGN_USER", "CARD", cardId, actionUserId, details);

            connection.commit();
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        }
    }

    public void moveToNextColumn(final Long cardId, final List<BoardColumnInfoDTO> boardColumnsInfo) throws SQLException{
        moveToNextColumn(cardId, boardColumnsInfo, null);
    }

    public void moveToNextColumn(final Long cardId, final List<BoardColumnInfoDTO> boardColumnsInfo, final Long userId) throws SQLException{
        try{
            var dao = new CardDAO(connection);
            var optional = dao.findById(cardId);
            var dto = optional.orElseThrow(
                    () -> new EntityNotFoundException("O card de id %s não foi encontrado".formatted(cardId))
            );
            if (dto.blocked()){
                var message = "O card %s está bloqueado, é necesário desbloquea-lo para mover".formatted(cardId);
                throw new CardBlockedException(message);
            }
            var currentColumn = boardColumnsInfo.stream()
                    .filter(bc -> bc.id().equals(dto.columnId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("O card informado pertence a outro board"));
            if (currentColumn.kind().equals(FINAL)){
                throw new CardFinishedException("O card já foi finalizado");
            }
            var nextColumn = boardColumnsInfo.stream()
                    .filter(bc -> bc.order() == currentColumn.order() + 1)
                    .findFirst().orElseThrow(() -> new IllegalStateException("O card está cancelado"));
            dao.moveToColumn(nextColumn.id(), cardId);

            // Log da ação
            var auditDao = new AuditLogDAO(connection);
            auditDao.logAction("MOVE", "CARD", cardId, userId,
                "Movido para coluna: " + nextColumn.kind());

            connection.commit();
        }catch (SQLException ex){
            connection.rollback();
            throw ex;
        }
    }

    public void cancel(final Long cardId, final Long cancelColumnId ,
                       final List<BoardColumnInfoDTO> boardColumnsInfo) throws SQLException{
        try{
            var dao = new CardDAO(connection);
            var optional = dao.findById(cardId);
            var dto = optional.orElseThrow(
                    () -> new EntityNotFoundException("O card de id %s não foi encontrado".formatted(cardId))
            );
            if (dto.blocked()){
                var message = "O card %s está bloqueado, é necesário desbloquea-lo para mover".formatted(cardId);
                throw new CardBlockedException(message);
            }
            var currentColumn = boardColumnsInfo.stream()
                    .filter(bc -> bc.id().equals(dto.columnId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("O card informado pertence a outro board"));
            if (currentColumn.kind().equals(FINAL)){
                throw new CardFinishedException("O card já foi finalizado");
            }
            boardColumnsInfo.stream()
                    .filter(bc -> bc.order() == currentColumn.order() + 1)
                    .findFirst().orElseThrow(() -> new IllegalStateException("O card está cancelado"));
            dao.moveToColumn(cancelColumnId, cardId);

            // Log da ação
            var auditDao = new AuditLogDAO(connection);
            auditDao.logAction("CANCEL", "CARD", cardId, null,
                "Movido para coluna de cancelamento: " + cancelColumnId);

            connection.commit();
        }catch (SQLException ex){
            connection.rollback();
            throw ex;
        }
    }

    public void block(final Long id, final String reason, final List<BoardColumnInfoDTO> boardColumnsInfo) throws SQLException {
        try{
            var dao = new CardDAO(connection);
            var optional = dao.findById(id);
            var dto = optional.orElseThrow(
                    () -> new EntityNotFoundException("O card de id %s não foi encontrado".formatted(id))
            );
            if (dto.blocked()){
                var message = "O card %s já está bloqueado".formatted(id);
                throw new CardBlockedException(message);
            }
            var currentColumn = boardColumnsInfo.stream()
                    .filter(bc -> bc.id().equals(dto.columnId()))
                    .findFirst()
                    .orElseThrow();
            if (currentColumn.kind().equals(FINAL) || currentColumn.kind().equals(CANCEL)){
                var message = "O card está em uma coluna do tipo %s e não pode ser bloqueado"
                        .formatted(currentColumn.kind());
                throw new IllegalStateException(message);
            }
            var blockDAO = new BlockDAO(connection);
            blockDAO.block(reason, id);

            // Log da ação
            var auditDao = new AuditLogDAO(connection);
            auditDao.logAction("BLOCK", "CARD", id, null,
                "Bloqueado: " + reason);

            connection.commit();
        }catch (SQLException ex) {
            connection.rollback();
            throw ex;
        }
    }

    public void unblock(final Long id, final String reason) throws SQLException {
        try{
            var dao = new CardDAO(connection);
            var optional = dao.findById(id);
            var dto = optional.orElseThrow(
                    () -> new EntityNotFoundException("O card de id %s não foi encontrado".formatted(id))
            );
            if (!dto.blocked()){
                var message = "O card %s não está bloqueado".formatted(id);
                throw new CardBlockedException(message);
            }
            var blockDAO = new BlockDAO(connection);
            blockDAO.unblock(reason, id);

            // Log da ação
            var auditDao = new AuditLogDAO(connection);
            auditDao.logAction("UNBLOCK", "CARD", id, null,
                "Desbloqueado: " + reason);

            connection.commit();
        }catch (SQLException ex) {
            connection.rollback();
            throw ex;
        }
    }

}
