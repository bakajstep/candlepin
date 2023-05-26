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
package org.candlepin.liquibase;

import org.candlepin.config.ConfigProperties;
import org.candlepin.config.Configuration;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.RuntimeEnvironment;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.ChangeLogIterator;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.changelog.filter.ContextChangeSetFilter;
import liquibase.changelog.filter.DbmsChangeSetFilter;
import liquibase.changelog.filter.IgnoreChangeSetFilter;
import liquibase.changelog.filter.LabelChangeSetFilter;
import liquibase.changelog.filter.ShouldRunChangeSetFilter;
import liquibase.changelog.visitor.ListVisitor;
import liquibase.command.CommandScope;
import liquibase.command.CommandResults;
import liquibase.command.core.UpdateCommandStep;
import liquibase.command.core.helpers.DbUrlConnectionCommandStep;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.parser.core.xml.XMLChangeLogSAXParser;
import liquibase.resource.ClassLoaderResourceAccessor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;


public class LiquibaseWrapper implements AutoCloseable {

    /** The Liquibase change log to use for checking and syncing the database state */
    public static final String DB_CHANGE_LOG = "db/changelog/changelog-update.xml";

    private final JdbcConnection connection;
    private final Database database;

    /**
     * Creates a new LiquibaseWrapper instance using the given configuration. If a Liquibase
     * instance cannot be created from the configuration provided, an exception will be thrown.
     *
     * @param config
     *  the configuration to use for instantiating a new liquibase wrapper
     *
     * @throws LiquibaseWrapperException
     *  if a Liquibase-related exception occurs while attempting to initialize the wrapper
     */
    public LiquibaseWrapper(Configuration config) {
        if (config == null) {
            throw new IllegalArgumentException("config is null");
        }

        this.loadConnectorDriver(config);
        this.connection = this.buildJdbcConnection(config);
        this.database = this.buildDatabaseConnection(this.connection);
    }

    public LiquibaseWrapper(JdbcConnection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("connection is null");
        }

        this.connection = connection;
        this.database = this.buildDatabaseConnection(this.connection);
    }

    private void loadConnectorDriver(Configuration config) {
        try {
            String driver = config.getString(ConfigProperties.DB_DRIVER_CLASS);

            // We don't need to do anything with the driver, just ensure it's been instantiated
            Class.forName(driver)
                .getDeclaredConstructor()
                .newInstance();
        }
        catch (ReflectiveOperationException e) {
            throw new LiquibaseWrapperException(e);
        }
    }

    private JdbcConnection buildJdbcConnection(Configuration config) {
        try {
            String dbUrl = config.getString(ConfigProperties.DB_URL);
            String dbUsername = config.getString(ConfigProperties.DB_USERNAME);
            String dbPassword = config.getString(ConfigProperties.DB_PASSWORD);

            Connection connection = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
            return new JdbcConnection(connection);
        }
        catch (SQLException e) {
            throw new LiquibaseWrapperException(e);
        }
    }

    private Database buildDatabaseConnection(JdbcConnection connection) {
        try {
            return DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(connection);
        }
        catch (LiquibaseException e) {
            throw new LiquibaseWrapperException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        try {
            if (this.database != null) {
                this.database.close();
            }
        }
        catch (DatabaseException e) {
            throw new LiquibaseWrapperException(e);
        }
    }

    /**
     * Sets the schema in which Liquibase will write its internal catalog and tracking tables.
     *
     * @param schema
     *  the schema to use for Liquibase tables
     *
     * @return
     *  a reference to this LiquibaseWrapper
     */
    public LiquibaseWrapper setLiquibaseSchema(String schema) {
        this.database.setLiquibaseSchemaName(schema);
        return this;
    }

    /**
     * Returns a list of changesets that appear to have not been run against the current database.
     * If all of the known changesets have been run, this method returns an empty list.
     *
     * @return
     *  a list of changesets that do not appear to have been run against the current database
     */
    public List<ChangeSet> listUnrunChangeSets(String... contexts) {
        try {
            DatabaseChangeLog changelog = new XMLChangeLogSAXParser()
                .parse(DB_CHANGE_LOG, new ChangeLogParameters(), new ClassLoaderResourceAccessor());

            Contexts contextsWrapper = new Contexts(contexts);

            // Candlepin (currently) doesn't use any label expressions
            LabelExpression labelExpression = null;

            ChangeLogIterator iterator = new ChangeLogIterator(changelog,
                new ShouldRunChangeSetFilter(this.database),
                new ContextChangeSetFilter(contextsWrapper),
                new DbmsChangeSetFilter(this.database),
                new IgnoreChangeSetFilter());

            ListVisitor visitor = new ListVisitor();
            iterator.run(visitor, new RuntimeEnvironment(this.database, contextsWrapper, labelExpression));

            return visitor.getSeenChangeSets();
        }
        catch (LiquibaseException e) {
            throw new LiquibaseWrapperException(e);
        }
    }

    /**
     * Applies any unrun changesets to the database.
     *
     * @return
     *  the status code of the Liquibase update command
     */
    public int executeUpdate(String... contexts) {
        Object statusCode;

        try {
            Contexts contextsWrapper = new Contexts(contexts);

            CommandResults results = new CommandScope(UpdateCommandStep.COMMAND_NAME)
                .addArgumentValue(DbUrlConnectionCommandStep.DATABASE_ARG, this.database)
                .addArgumentValue(UpdateCommandStep.CHANGELOG_FILE_ARG, DB_CHANGE_LOG)
                .addArgumentValue(UpdateCommandStep.CONTEXTS_ARG, contextsWrapper.toString())
                .execute();

            statusCode = results.getResult("statusCode");
        }
        catch (LiquibaseException e) {
            throw new LiquibaseWrapperException(e);
        }

        try {
            if (statusCode instanceof Integer) {
                return (Integer) statusCode;
            }

            return Integer.parseInt(statusCode.toString());
        }
        catch (NumberFormatException e) {
            throw new LiquibaseWrapperException("Unable to parse update command result: " + statusCode, e);
        }
    }

    // add more Liquibase operations here as necessary

}
