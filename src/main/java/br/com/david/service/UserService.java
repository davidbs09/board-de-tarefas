package br.com.david.service;

import br.com.david.persistence.dao.UserDAO;
import br.com.david.persistence.entity.UserEntity;
import lombok.AllArgsConstructor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
public class UserService {

    private final Connection connection;

    public UserEntity create(final UserEntity entity) throws SQLException {
        try {
            var dao = new UserDAO(connection);
            dao.insert(entity);
            connection.commit();
            return entity;
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        }
    }

    public Optional<UserEntity> findById(final Long id) throws SQLException {
        var dao = new UserDAO(connection);
        return dao.findById(id);
    }

    public List<UserEntity> findAll() throws SQLException {
        var dao = new UserDAO(connection);
        return dao.findAll();
    }

}
