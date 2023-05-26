/*
 * Copyright (c) 2009 - 2023 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.junit;

import org.candlepin.config.ConfigurationException;
import org.candlepin.liquibase.LiquibaseWrapper;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;

import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.internal.PersistenceXmlParser;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Map;

/**
 * The LiquibaseExtension class performs initialization and teardown of a temporary database for use
 * with unit tests that are backed by a pseudo-mocked database.
 *
 * Databases created by this extension exist for the duration of a single test suite. Between each
 * test in a given test suite, the database will be truncated. After all applicable tests in a given
 * suite have been executed, the database will be destroyed, and all filesystem-based resources will
 * be removed.
 */
public class LiquibaseExtension implements BeforeAllCallback, AfterAllCallback, AfterEachCallback {
    private static final String HSQLDB_DIR_PREFIX = "cp_unittest_hsqldb-";
    private static final String HSQLDB_DIR_PROPERTY = "hsqldb_dir";

    private static final String JDBC_PERSISTENCE_UNIT = "testing";
    private static final String JDBC_URL_PROPERTY = "hibernate.connection.url";

    private static final String LIQUIBASE_CHANGELOG_CONTEXT = "test";
    private static final String LIQUIBASE_SCHEMA = "LIQUIBASE";
    private static final String PUBLIC_SCHEMA = "PUBLIC";

    private static final String TRUNCATE_SQL = "TRUNCATE SCHEMA %s RESTART IDENTITY AND COMMIT NO CHECK";
    private static final String CREATE_SCHEMA_SQL = "CREATE SCHEMA IF NOT EXISTS %s";
    private static final String DROP_SCHEMA_SQL = "DROP SCHEMA IF EXISTS %s CASCADE";
    private static final String SHUTDOWN_CMD = "SHUTDOWN";

    private File hsqldbDir;

    private JdbcConnection connection;
    private LiquibaseWrapper liquibaseWrapper;

    public LiquibaseExtension() throws IOException, SQLException {
        this.hsqldbDir = this.setupTempDirectory();
        this.connection = this.buildJdbcConnection();
        this.liquibaseWrapper = new LiquibaseWrapper(this.connection);

        this.initDb();
    }

    private File setupTempDirectory() throws IOException {
        Path tmp = Files.createTempDirectory(HSQLDB_DIR_PREFIX);
        System.setProperty(HSQLDB_DIR_PROPERTY, tmp.toString());

        return tmp.toFile();
    }

    private void tearDownTempFiles(File file) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                for (File child : file.listFiles()) {
                    this.tearDownTempFiles(child);
                }
            }

            file.delete();
        }
    }

    private String getJdbcUrl(String persistenceUnit) {
        ParsedPersistenceXmlDescriptor unit = PersistenceXmlParser.locatePersistenceUnits(Map.of())
            .stream()
            .filter(elem -> persistenceUnit.equals(elem.getName()))
            .findFirst()
            .orElseThrow(() -> {
                String errmsg = String.format("Could not locate persistence unit \"%s\" in persistence.xml",
                    persistenceUnit);

                return new ConfigurationException(errmsg);
            });

        return unit.getProperties()
            .getProperty(JDBC_URL_PROPERTY);
    }

    private JdbcConnection buildJdbcConnection() throws SQLException {
        String jdbcUrl = this.getJdbcUrl(JDBC_PERSISTENCE_UNIT);

        Connection jdbcConnection = DriverManager.getConnection(jdbcUrl, "sa", "");
        return new JdbcConnection(jdbcConnection);
    }

    private void initDb() {
        this.dropLiquibaseSchema();
        this.dropPublicSchema();
    }

    private void executeUpdate(String sql) {
        try (Statement statement = this.connection.createStatement()) {
            statement.executeUpdate(sql);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void createCandlepinSchema() {
        this.executeUpdate(String.format(CREATE_SCHEMA_SQL, LIQUIBASE_SCHEMA));

        this.liquibaseWrapper.setLiquibaseSchema(LIQUIBASE_SCHEMA)
            .executeUpdate(LIQUIBASE_CHANGELOG_CONTEXT);
    }

    private void dropPublicSchema() {
        this.executeUpdate(String.format(DROP_SCHEMA_SQL, PUBLIC_SCHEMA));
    }

    private void dropLiquibaseSchema() {
        this.executeUpdate(String.format(DROP_SCHEMA_SQL, LIQUIBASE_SCHEMA));
    }

    private void truncatePublicSchema() {
        this.executeUpdate(String.format(TRUNCATE_SQL, PUBLIC_SCHEMA));
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        this.createCandlepinSchema();
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        dropPublicSchema();
        dropLiquibaseSchema();
        this.executeUpdate(SHUTDOWN_CMD);

        this.liquibaseWrapper.close();
        this.connection.close();

        this.tearDownTempFiles(this.hsqldbDir);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        truncatePublicSchema();
    }

}
