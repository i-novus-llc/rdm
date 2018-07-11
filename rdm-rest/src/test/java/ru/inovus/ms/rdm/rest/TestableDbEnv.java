package ru.inovus.ms.rdm.rest;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.junit.BeforeClass;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TestableDbEnv {

    public static final int PORT = 5444;
    public static final String DB_NAME = "rdm_test";
    private static volatile boolean isDbCreated = false;

    @BeforeClass
    public static void startDb() throws IOException {
        DataSource dataSource = EmbeddedPostgres.builder().setCleanDataDirectory(true).setPort(PORT).start().getPostgresDatabase();
        if (!isDbCreated) {
            try(Connection connection = dataSource.getConnection()) {
                PreparedStatement preparedStatement = connection.prepareStatement("DROP DATABASE IF EXISTS "+DB_NAME+"; CREATE DATABASE " + DB_NAME);
                preparedStatement.executeUpdate();
                isDbCreated = true;
            } catch (SQLException e) {
                throw new RuntimeException(e);

            }
        }
    }
}
