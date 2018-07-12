package ru.inovus.ms.rdm.rest;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.junit.BeforeClass;
import ru.inovus.util.pg.embeded.PatchedPgBinaryResolver;

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
        EmbeddedPostgres pg = EmbeddedPostgres.builder().setPgBinaryResolver(new PatchedPgBinaryResolver()).setCleanDataDirectory(true).setPort(PORT).start();
        DataSource dataSource = pg.getPostgresDatabase();
        if (!isDbCreated) {
            try(Connection connection = dataSource.getConnection()) {
                PreparedStatement preparedStatement = connection.prepareStatement(
                                "DROP TEXT SEARCH CONFIGURATION IF EXISTS ru; " +
                                "DROP TEXT SEARCH DICTIONARY IF EXISTS ispell_ru; " +
                                "CREATE TEXT SEARCH DICTIONARY ispell_ru (\n" +
                                "template= ispell,\n" +
                                "dictfile= ru,\n" +
                                "afffile=ru,\n" +
                                "stopwords = russian\n" +
                                ");\n" +
                                "CREATE TEXT SEARCH CONFIGURATION ru ( COPY = russian );\n" +
                                "ALTER TEXT SEARCH CONFIGURATION ru\n" +
                                "ALTER MAPPING\n" +
                                "FOR word, hword, hword_part\n" +
                                "WITH ispell_ru, russian_stem; " +
                        "DROP DATABASE IF EXISTS "+DB_NAME+"; CREATE DATABASE " + DB_NAME + ";"
                       );
                preparedStatement.executeUpdate();
                isDbCreated = true;
                try(Connection userDbCon = pg.getDatabase("postgres", DB_NAME).getConnection()) {
                    userDbCon.prepareStatement(
                            "DROP TEXT SEARCH CONFIGURATION IF EXISTS ru; " +
                                    "DROP TEXT SEARCH DICTIONARY IF EXISTS ispell_ru; " +
                                    "CREATE TEXT SEARCH DICTIONARY ispell_ru (\n" +
                                    "template= ispell,\n" +
                                    "dictfile= ru,\n" +
                                    "afffile=ru,\n" +
                                    "stopwords = russian\n" +
                                    ");\n" +
                                    "CREATE TEXT SEARCH CONFIGURATION ru ( COPY = russian );\n" +
                                    "ALTER TEXT SEARCH CONFIGURATION ru\n" +
                                    "ALTER MAPPING\n" +
                                    "FOR word, hword, hword_part\n" +
                                    "WITH ispell_ru, russian_stem; "
                    ).executeUpdate();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);

            }
        }
    }
}
