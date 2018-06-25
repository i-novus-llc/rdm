package ru.inovus.ms.rdm.rest;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.junit.BeforeClass;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AbstractIntegrationTest {

    public static final int PORT = 5444;
    public static final String DB_NAME = "rdm_test";

    @BeforeClass
    public static void startDb() throws IOException {
        DataSource dataSource = EmbeddedPostgres.builder().setCleanDataDirectory(true).setPort(PORT).start().getPostgresDatabase();
        try(Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE DATABASE " + DB_NAME);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);

        }
    }
}
