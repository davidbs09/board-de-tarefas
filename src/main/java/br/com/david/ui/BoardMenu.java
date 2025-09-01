package br.com.david.ui;

import br.com.david.dto.BoardColumnInfoDTO;
import br.com.david.persistence.entity.BoardColumnEntity;
import br.com.david.persistence.entity.BoardEntity;
import br.com.david.persistence.entity.CardEntity;
import br.com.david.service.AuditService;
import br.com.david.service.BoardColumnQueryService;
import br.com.david.service.BoardQueryService;
import br.com.david.service.CardQueryService;
import br.com.david.service.UserService;
import br.com.david.service.CardService;
import lombok.AllArgsConstructor;

import java.sql.SQLException;
import java.util.Scanner;

import static br.com.david.persistence.config.ConnectionConfig.getConnection;

@AllArgsConstructor
public class BoardMenu {

    private final Scanner scanner = new Scanner(System.in);

    private final BoardEntity entity;

    public void execute() {
        try {
            int option = -1;
            while (option != 10) {
                System.out.println("1 - Criar um card");
                System.out.println("2 - Mover um card");
                System.out.println("3 - Bloquear um card");
                System.out.println("4 - Desbloquear um card");
                System.out.println("5 - Cancelar um card");
                System.out.println("6 - Ver board");
                System.out.println("7 - Ver coluna com cards");
                System.out.println("8 - Ver card");
                System.out.println("9 - Atribuir responsável a um card");
                System.out.println("10 - Ver histórico de um card");
                System.out.println("11 - Listar usuários disponíveis");
                System.out.println("12 - Sair");
                String input = scanner.nextLine();
                try {
                    option = Integer.parseInt(input.trim());
                } catch (NumberFormatException e) {
                    System.out.println("Opção inválida, informe uma opção do menu");
                    continue;
                }
                switch (option) {
                    case 1 -> createCard();
                    case 2 -> moveCardToNextColumn();
                    case 3 -> blockCard();
                    case 4 -> unblockCard();
                    case 5 -> cancelCard();
                    case 6 -> showBoard();
                    case 7 -> showColumn();
                    case 8 -> showCard();
                    case 9 -> assignUserToCard();
                    case 10 -> showCardHistory();
                    case 11 -> listUsers();
                    case 12 -> System.exit(0);
                    default -> System.out.println("Opção inválida, informe uma opção do menu");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            System.exit(0);
        }
    }

    private void createCard() throws SQLException{
        var card = new CardEntity();
        System.out.println("Informe o título do card");
        card.setTitle(scanner.nextLine().trim());
        System.out.println("Informe a descrição do card");
        card.setDescription(scanner.nextLine().trim());
        card.setBoardColumn(entity.getInitialColumn());
        try(var connection = getConnection()){
            new CardService(connection).create(card);
        }
    }

    private void moveCardToNextColumn() throws SQLException {
        System.out.println("Informe o id do card que deseja mover para a próxima coluna");
        Long cardId = readLong();
        var boardColumnsInfo = entity.getBoardColumns().stream()
                .map(bc -> new BoardColumnInfoDTO(bc.getId(), bc.getOrder(), bc.getKind()))
                .toList();
        try(var connection = getConnection()){
            new CardService(connection).moveToNextColumn(cardId, boardColumnsInfo);
        } catch (RuntimeException ex){
            System.out.println(ex.getMessage());
        }
    }

    private void blockCard() throws SQLException {
        System.out.println("Informe o id do card que será bloqueado");
        Long cardId = readLong();
        System.out.println("Informe o motivo do bloqueio do card");
        var reason = scanner.nextLine().trim();
        var boardColumnsInfo = entity.getBoardColumns().stream()
                .map(bc -> new BoardColumnInfoDTO(bc.getId(), bc.getOrder(), bc.getKind()))
                .toList();
        try(var connection = getConnection()){
            new CardService(connection).block(cardId, reason, boardColumnsInfo);
        } catch (RuntimeException ex){
            System.out.println(ex.getMessage());
        }
    }

    private void unblockCard() throws SQLException {
        System.out.println("Informe o id do card que será desbloqueado");
        Long cardId = readLong();
        System.out.println("Informe o motivo do desbloqueio do card");
        var reason = scanner.nextLine().trim();
        try(var connection = getConnection()){
            new CardService(connection).unblock(cardId, reason);
        } catch (RuntimeException ex){
            System.out.println(ex.getMessage());
        }
    }

    private void cancelCard() throws SQLException {
        System.out.println("Informe o id do card que deseja mover para a coluna de cancelamento");
        Long cardId = readLong();
        var cancelColumn = entity.getCancelColumn();
        var boardColumnsInfo = entity.getBoardColumns().stream()
                .map(bc -> new BoardColumnInfoDTO(bc.getId(), bc.getOrder(), bc.getKind()))
                .toList();
        try(var connection = getConnection()){
            new CardService(connection).cancel(cardId, cancelColumn.getId(), boardColumnsInfo);
        } catch (RuntimeException ex){
            System.out.println(ex.getMessage());
        }
    }

    private void showBoard() throws SQLException {
        try(var connection = getConnection()){
            var optional = new BoardQueryService(connection).showBoardDetails(entity.getId());
            optional.ifPresent(b -> {
                System.out.printf("Board [%s,%s]\n", b.id(), b.name());
                b.columns().forEach(c ->
                        System.out.printf("Coluna [%s] tipo: [%s] tem %s cards\n", c.name(), c.kind(), c.cardsAmount())
                );
            });
        }
    }

    private void showColumn() throws SQLException {
        var columnsIds = entity.getBoardColumns().stream().map(BoardColumnEntity::getId).toList();
        Long selectedColumnId = null;
        while (selectedColumnId == null || !columnsIds.contains(selectedColumnId)){
            System.out.printf("Escolha uma coluna do board %s pelo id\n", entity.getName());
            entity.getBoardColumns().forEach(c -> System.out.printf("%s - %s [%s]\n", c.getId(), c.getName(), c.getKind()));
            selectedColumnId = readLong();
        }
        try(var connection = getConnection()){
            var column = new BoardColumnQueryService(connection).findById(selectedColumnId);
            column.ifPresent(co -> {
                System.out.printf("Coluna %s tipo %s\n", co.getName(), co.getKind());
                co.getCards().forEach(ca -> System.out.printf("Card %s - %s\nDescrição: %s",
                        ca.getId(), ca.getTitle(), ca.getDescription()));
            });
        }
    }

    private void showCard() throws SQLException {
        System.out.println("Informe o id do card que deseja visualizar");
        Long selectedCardId = readLong();
        try(var connection  = getConnection()){
            new CardQueryService(connection).findById(selectedCardId)
                    .ifPresentOrElse(
                            c -> {
                                System.out.printf("Card %s - %s.\n", c.id(), c.title());
                                System.out.printf("Descrição: %s\n", c.description());
                                System.out.println(c.blocked() ?
                                        "Está bloqueado. Motivo: " + c.blockReason() :
                                        "Não está bloqueado");
                                System.out.printf("Já foi bloqueado %s vezes\n", c.blocksAmount());
                                System.out.printf("Está no momento na coluna %s - %s\n", c.columnId(), c.columnName());
                            },
                            () -> System.out.printf("Não existe um card com o id %s\n", selectedCardId));
        }
    }

    private void assignUserToCard() throws SQLException {
        System.out.println("Informe o id do card que deseja atribuir um responsável:");
        Long cardId = readLong();

        // Listar usuários disponíveis
        try(var connection = getConnection()){
            var userService = new UserService(connection);
            var users = userService.findAll();

            if (users.isEmpty()) {
                System.out.println("Nenhum usuário cadastrado no sistema.");
                return;
            }

            System.out.println("Usuários disponíveis:");
            users.forEach(u -> System.out.printf("%d - %s (%s)\n", u.getId(), u.getName(), u.getEmail()));

            System.out.println("Informe o id do usuário que será o responsável (ou 0 para remover responsável):");
            Long userId = readLong();

            if (userId == 0) {
                new CardService(connection).assignUser(cardId, null, 1L); // 1L = admin user
                System.out.println("Responsável removido com sucesso!");
            } else {
                new CardService(connection).assignUser(cardId, userId, 1L); // 1L = admin user
                System.out.println("Responsável atribuído com sucesso!");
            }
        } catch (RuntimeException ex){
            System.out.println("Erro: " + ex.getMessage());
        }
    }

    private void showCardHistory() throws SQLException {
        System.out.println("Informe o id do card que deseja ver o histórico:");
        Long cardId = readLong();

        try(var connection = getConnection()){
            var auditService = new AuditService(connection);
            var history = auditService.getHistory(cardId, "CARD");

            if (history.isEmpty()) {
                System.out.println("Nenhum histórico encontrado para este card.");
                return;
            }

            System.out.println("Histórico do card:");
            history.forEach(log -> {
                System.out.printf("[%s] %s - %s\n",
                    log.getTimestamp(),
                    log.getAction(),
                    log.getDetails());
                if (log.getUserId() != null) {
                    System.out.printf("  Usuário ID: %d\n", log.getUserId());
                }
                System.out.println();
            });
        } catch (RuntimeException ex){
            System.out.println("Erro: " + ex.getMessage());
        }
    }
    // Método utilitário para ler um Long do usuário com validação
    private Long readLong() {
        while (true) {
            String input = scanner.nextLine();
            try {
                return Long.parseLong(input.trim());
            } catch (NumberFormatException e) {
                System.out.println("Por favor, informe um número válido.");
            }
        }
    }

    private void listUsers() throws SQLException {
        try(var connection = getConnection()){
            var userService = new UserService(connection);
            var users = userService.findAll();

            if (users.isEmpty()) {
                System.out.println("Nenhum usuário cadastrado no sistema.");
                return;
            }

            System.out.println("Usuários cadastrados:");
            users.forEach(u -> System.out.printf("ID: %d | Nome: %s | Email: %s\n",
                u.getId(), u.getName(), u.getEmail()));
        } catch (RuntimeException ex){
            System.out.println("Erro: " + ex.getMessage());
        }
    }
}
