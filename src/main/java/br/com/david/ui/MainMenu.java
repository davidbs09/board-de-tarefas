package br.com.david.ui;

import br.com.david.persistence.entity.BoardColumnEntity;
import br.com.david.persistence.entity.BoardColumnKindEnum;
import br.com.david.persistence.entity.BoardEntity;
import br.com.david.service.BoardQueryService;
import br.com.david.service.BoardService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static br.com.david.persistence.config.ConnectionConfig.getConnection;
import static br.com.david.persistence.entity.BoardColumnKindEnum.CANCEL;
import static br.com.david.persistence.entity.BoardColumnKindEnum.FINAL;
import static br.com.david.persistence.entity.BoardColumnKindEnum.INITIAL;
import static br.com.david.persistence.entity.BoardColumnKindEnum.PENDING;

public class MainMenu {

    private final Scanner scanner = new Scanner(System.in);

    public void execute() throws SQLException {
        System.out.println("Bem vindo ao gerenciador de boards, escolha a opção desejada");
        while (true) {
            System.out.println("1 - Criar um novo board");
            System.out.println("2 - Buscar boards por nome");
            System.out.println("3 - Selecionar um board existente");
            System.out.println("4 - Excluir um board");
            System.out.println("5 - Sair");

            String input = scanner.nextLine();
            int option;
            try {
                option = Integer.parseInt(input.trim());
            } catch (NumberFormatException e) {
                System.out.println("Opção inválida, informe uma opção do menu");
                continue;
            }
            switch (option) {
                case 1 -> createBoard();
                case 2 -> searchBoardsByName();
                case 3 -> selectBoard();
                case 4 -> deleteBoard();
                case 5 -> System.exit(0);
                default -> System.out.println("Opção inválida, informe uma opção do menu");
            }
        }
    }

    private void createBoard() throws SQLException {
        var entity = new BoardEntity();
        System.out.println("Informe o nome do seu board");
        entity.setName(scanner.nextLine().trim());

        int additionalColumns = 0;
        while (true) {
            System.out.println("Seu board terá colunas além das 3 padrões? Se sim informe quantas, senão digite '0'");
            String colInput = scanner.nextLine();
            try {
                additionalColumns = Integer.parseInt(colInput.trim());
                if (additionalColumns < 0) throw new NumberFormatException();
                break;
            } catch (NumberFormatException e) {
                System.out.println("Por favor, informe um número válido maior ou igual a zero.");
            }
        }

        List<BoardColumnEntity> columns = new ArrayList<>();

        System.out.println("Informe o nome da coluna inicial do board");
        var initialColumnName = scanner.nextLine().trim();
        var initialColumn = createColumn(initialColumnName, INITIAL, 0);
        columns.add(initialColumn);

        for (int i = 0; i < additionalColumns; i++) {
            System.out.println("Informe o nome da coluna de tarefa pendente do board");
            var pendingColumnName = scanner.nextLine().trim();
            var pendingColumn = createColumn(pendingColumnName, PENDING, i + 1);
            columns.add(pendingColumn);
        }

        System.out.println("Informe o nome da coluna final");
        var finalColumnName = scanner.nextLine().trim();
        var finalColumn = createColumn(finalColumnName, FINAL, additionalColumns + 1);
        columns.add(finalColumn);

        System.out.println("Informe o nome da coluna de cancelamento do board");
        var cancelColumnName = scanner.nextLine().trim();
        var cancelColumn = createColumn(cancelColumnName, CANCEL, additionalColumns + 2);
        columns.add(cancelColumn);

        entity.setBoardColumns(columns);
        try(var connection = getConnection()){
            var service = new BoardService(connection);
            service.insert(entity);
        }

    }

    private void selectBoard() throws SQLException {
        System.out.println("Informe o id do board que deseja selecionar");
        Long id = null;
        while (id == null) {
            String input = scanner.nextLine();
            try {
                id = Long.parseLong(input.trim());
            } catch (NumberFormatException e) {
                System.out.println("Por favor, informe um id válido.");
            }
        }
        final Long finalId = id;
        try(var connection = getConnection()){
            var queryService = new BoardQueryService(connection);
            var optional = queryService.findById(finalId);
            optional.ifPresentOrElse(
                    b -> new BoardMenu(b).execute(),
                    () -> System.out.printf("Não foi encontrado um board com id %s\n", finalId)
            );
        }
    }

    private void deleteBoard() throws SQLException {
        System.out.println("Informe o id do board que será excluido");
        Long id = null;
        while (id == null) {
            String input = scanner.nextLine();
            try {
                id = Long.parseLong(input.trim());
            } catch (NumberFormatException e) {
                System.out.println("Por favor, informe um id válido.");
            }
        }
        try(var connection = getConnection()){
            var service = new BoardService(connection);
            if (service.delete(id)){
                System.out.printf("O board %s foi excluido\n", id);
            } else {
                System.out.printf("Não foi encontrado um board com id %s\n", id);
            }
        }
    }

    private void searchBoardsByName() throws SQLException {
        System.out.println("Informe o nome (ou parte do nome) do board para buscar:");
        var name = scanner.nextLine().trim();
        try (var connection = getConnection()) {
            var queryService = new BoardQueryService(connection);
            var boards = queryService.findByName(name);
            if (boards.isEmpty()) {
                System.out.println("Nenhum board encontrado.");
            } else {
                System.out.println("Boards encontrados:");
                for (var board : boards) {
                    System.out.printf("ID: %d | Nome: %s%n", board.getId(), board.getName());
                }
            }
        }
    }

    private BoardColumnEntity createColumn(final String name, final BoardColumnKindEnum kind, final int order){
        var boardColumn = new BoardColumnEntity();
        boardColumn.setName(name);
        boardColumn.setKind(kind);
        boardColumn.setOrder(order);
        return boardColumn;
    }

}
