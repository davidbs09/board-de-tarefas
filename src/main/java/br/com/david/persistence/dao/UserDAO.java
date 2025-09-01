package br.com.david.persistence.dao;

import br.com.david.persistence.entity.UserEntity;
import com.mysql.cj.jdbc.StatementImpl;
import lombok.AllArgsConstructor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
public class UserDAO {

    private Connection connection;

    public UserEntity insert(final UserEntity entity) throws SQLException {
        var sql = "INSERT INTO USERS (name, email) VALUES (?, ?)";
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, entity.getName());
            statement.setString(2, entity.getEmail());
            statement.executeUpdate();
            if (statement instanceof StatementImpl impl) {
                entity.setId(impl.getLastInsertID());
            }
        }
        return entity;
    }

    public Optional<UserEntity> findById(final Long id) throws SQLException {
        var sql = "SELECT id, name, email FROM USERS WHERE id = ?";
        try (var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeQuery();
            var resultSet = statement.getResultSet();
            if (resultSet.next()) {
                var entity = new UserEntity();
                entity.setId(resultSet.getLong("id"));
                entity.setName(resultSet.getString("name"));
                entity.setEmail(resultSet.getString("email"));
                return Optional.of(entity);
            }
        }
        return Optional.empty();
    }

    public List<UserEntity> findAll() throws SQLException {
        var sql = "SELECT id, name, email FROM USERS ORDER BY name";
        List<UserEntity> users = new ArrayList<>();
        try (var statement = connection.prepareStatement(sql)) {
            statement.executeQuery();
            var resultSet = statement.getResultSet();
            while (resultSet.next()) {
                var entity = new UserEntity();
                entity.setId(resultSet.getLong("id"));
                entity.setName(resultSet.getString("name"));
                entity.setEmail(resultSet.getString("email"));
                users.add(entity);
            }
        }
        return users;
    }

}
