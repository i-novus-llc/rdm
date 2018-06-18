package ru.inovus.ms.rdm.rest;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by tnurdinov on 18.06.2018.
 */
public class DbServer {
    public static void startDb(int port, String dbName) throws IOException {
        DataSource dataSource = EmbeddedPostgres.builder().setCleanDataDirectory(true).setPort(5444).start().getPostgresDatabase();
        try(Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE DATABASE " + dbName);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);

        }
    }
}
