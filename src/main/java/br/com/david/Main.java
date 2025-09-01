package br.com.david;

import br.com.david.persistence.migration.MigrationStrategy;
import br.com.david.ui.MainMenu;

import java.sql.SQLException;

import static br.com.david.persistence.config.ConnectionConfig.getConnection;

public class Main {

    public static void main(String[] args) throws SQLException {
        try(var connection = getConnection()){
            new MigrationStrategy(connection).executeMigration();
        }
        new MainMenu().execute();
    }

}
