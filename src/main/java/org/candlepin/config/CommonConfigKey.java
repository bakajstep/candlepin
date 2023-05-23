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
package org.candlepin.config;

import static org.candlepin.config.ConfigurationPrefixes.JPA_CONFIG_PREFIX;

/**
 * Defines a map of default properties used to prepopulate the {@link Configuration}.
 * Also holds static keys for config lookup.
 */
public enum CommonConfigKey implements ConfigKey {

    CANDLEPIN_URL("candlepin.url"),

    /**
     * Whether we allow users to authenticate (e.g. HTTP Basic) over insecure
     * channel such as HTTP.
     * The default is false.
     * A user might set this to 'true' for easier debugging of the Candlepin
     * server.
     * If kept in default (false) setting, then Candlepin will disallow any
     * attempt to authenticate over insecure channel.
     */
    AUTH_OVER_HTTP("candlepin.allow_auth_over_http"),
    CA_KEY("candlepin.ca_key"),
    CA_CERT("candlepin.ca_cert"),
    FAIL_ON_UNKNOWN_IMPORT_PROPERTIES("candlepin.importer.fail_on_unknown"),
    CA_CERT_UPSTREAM("candlepin.upstream_ca_cert"),
    CA_KEY_PASSWORD("candlepin.ca_key_password"),

    /*
     * XXX The actual property key refers to HornetQ which was ActiveMQ's ancestor.  We have to keep the
     * key unchanged for compatibility reasons. These are deprecated, however, and should be replaced by
     * newer configuration values as features are added.
     */
    ACTIVEMQ_ENABLED("candlepin.audit.hornetq.enable"),
    ACTIVEMQ_EMBEDDED("candlepin.audit.hornetq.embedded"),
    ACTIVEMQ_BROKER_URL("candlepin.audit.hornetq.broker_url"),
    ACTIVEMQ_SERVER_CONFIG_PATH("candlepin.audit.hornetq.config_path"),

    /**
     * This number is large message size. Any message
     * that is bigger than this number is regarded as large.
     * That means that ActiveMQ will page this message
     * to a disk. Setting this number to something high
     * e.g. 1 000 0000 will effectively disable the
     * functionality. This is handy when you expect a lot of
     * messages and you do not want to run out of disk space.
     *
     * NOTE: Increasing this property to more than 501760 could cause the following issue:
     *  https://access.redhat.com/solutions/2203791
     * As the article explains, this issue could occur only when the value of journal-buffer-size is less
     * than the value of min-large-message-size, in bytes. We do not currently alter the
     * journal-buffer-size on the artemis broker, which uses the default value (501760 bytes), while the
     * default value of this property (which is used to set the min-large-message-size) is set to 102400
     * bytes. As such, the issue will not occur with our default settings, but only if we set this property
     * to a value larger than 501760 (or if the journal-buffer-size property in broker.xml is changed to be
     * less than the value of this property).
     */
    ACTIVEMQ_LARGE_MSG_SIZE("candlepin.audit.hornetq.large_msg_size"),

    /**
     * The interval (in milliseconds) at which the ActiveMQStatusMonitor will check the connection state
     * once the connection has dropped.
     */
    ACTIVEMQ_CONNECTION_MONITOR_INTERVAL("candlepin.audit.hornetq.monitor.interval"),

    AUDIT_LISTENERS("candlepin.audit.listeners"),
    /**
     * Enables audit event filtering. See documentation of EventFilter
     */
    AUDIT_FILTER_ENABLED("candlepin.audit.filter.enabled"),
    /**
     * Events mentioned in this list will not be filtered. They we be sent into our
     * auditing system
     */
    AUDIT_FILTER_DO_NOT_FILTER("candlepin.audit.filter.donotfilter"),
    /**
     * These events will be dropped.
     */
    AUDIT_FILTER_DO_FILTER("candlepin.audit.filter.dofilter"),
    /**
     * Can be set to DO_FILTER or DO_NOT_FILTER.
     * When set to DO_FILTER, then events that are not either in DO_FILTER nor DO_NOT_FILTER
     * will be filtered, meaning they will not enter ActiveMQ.
     */
    AUDIT_FILTER_DEFAULT_POLICY("candlepin.audit.filter.policy"),

    PRETTY_PRINT("candlepin.pretty_print"),
    ACTIVATION_DEBUG_PREFIX("candlepin.subscription.activation.debug_prefix"),

    // Space separated list of resources to hide in the GET / list:
    HIDDEN_RESOURCES("candlepin.hidden_resources"),

    // Space separated list of resources to hide in GET /status
    HIDDEN_CAPABILITIES("candlepin.hidden_capabilities"),

    HALT_ON_LIQUIBASE_DESYNC("candlepin.db.halt_on_liquibase_desync"),
    DB_MANAGE_ON_START("candlepin.db.database_manage_on_startup"),

    // Authentication
    TRUSTED_AUTHENTICATION("candlepin.auth.trusted.enable"),
    SSL_AUTHENTICATION("candlepin.auth.ssl.enable"),
    OAUTH_AUTHENTICATION("candlepin.auth.oauth.enable"),
    BASIC_AUTHENTICATION("candlepin.auth.basic.enable"),
    KEYCLOAK_AUTHENTICATION("candlepin.auth.keycloak.enable"),
    CLOUD_AUTHENTICATION("candlepin.auth.cloud.enable"),
    ACTIVATION_KEY_AUTHENTICATION("candlepin.auth.activation_key.enable"),

    // JWT configuration
    JWT_ISSUER("candlepin.jwt.issuer"),
    JWT_TOKEN_TTL("candlepin.jwt.token_ttl"),

    /**
     * A possibility to enable Suspend Mode. By default, the suspend mode is enabled
     */
    SUSPEND_MODE_ENABLED("candlepin.suspend_mode_enabled"),

    // Messaging
    CPM_PROVIDER("candlepin.messaging.provider"),

    // OCSP stapling
    SSL_VERIFY("candlepin.sslverifystatus"),

    // TODO:
    // Clean up all the messaging configuration. We have all sorts of prefixes and definitions for
    // common broker options, and stuff which is unique to specific brokers. We should be defining
    // them as "candlepin.messaging.<broker type>.<setting>" instead of the various sections we
    // have now.
    ACTIVEMQ_EMBEDDED_BROKER("candlepin.messaging.activemq.embedded.enabled"),

    ACTIVEMQ_JAAS_INVM_LOGIN_NAME("candlepin.messaging.activemq.embedded.jaas_invm_login_name"),

    ACTIVEMQ_JAAS_CERTIFICATE_LOGIN_NAME("candlepin.messaging.activemq.embedded.jaas_certificate_login_name"),

    // Quartz configurations
    QUARTZ_CLUSTERED_MODE("org.quartz.jobStore.isClustered"),

    // Hibernate
    DB_PASSWORD(ConfigurationPrefixes.JPA_CONFIG_PREFIX + "hibernate.connection.password"),
    // Cache
    CACHE_JMX_STATS("cache.jmx.statistics"),
    CACHE_CONFIG_FILE_URI(ConfigurationPrefixes.JPA_CONFIG_PREFIX + "hibernate.javax.cache.uri"),

    SYNC_WORK_DIR("candlepin.sync.work_dir"),

    /**
     *  Controls which facts will be stored by Candlepin -- facts with keys that do not match this
     *  value will be discarded.
     */
    CONSUMER_FACTS_MATCHER("candlepin.consumer.facts.match_regex"),

    SHARD_USERNAME("candlepin.shard.username"),
    SHARD_PASSWORD("candlepin.shard.password"),
    SHARD_WEBAPP("candlepin.shard.webapp"),

    STANDALONE("candlepin.standalone"),
    ENV_CONTENT_FILTERING("candlepin.environment_content_filtering"),
    USE_SYSTEM_UUID_FOR_MATCHING("candlepin.use_system_uuid_for_matching"),

    CONSUMER_SYSTEM_NAME_PATTERN("candlepin.consumer_system_name_pattern"),
    CONSUMER_PERSON_NAME_PATTERN("candlepin.consumer_person_name_pattern"),

    PREFIX_WEBURL("candlepin.export.prefix.weburl"),
    PREFIX_APIURL("candlepin.export.prefix.apiurl"),
    PASSPHRASE_SECRET_FILE("candlepin.passphrase.path"),

    PRODUCT_CACHE_MAX("candlepin.cache.product_cache_max"),

    INTEGER_FACTS("candlepin.integer_facts"),
    NON_NEG_INTEGER_FACTS("candlepin.positive_integer_facts"),

    INTEGER_ATTRIBUTES("candlepin.integer_attributes"),

    NON_NEG_INTEGER_ATTRIBUTES("candlepin.positive_integer_attributes"),

    LONG_ATTRIBUTES("candlepin.long_attributes"),
    NON_NEG_LONG_ATTRIBUTES("candlepin.positive_long_attributes"),
    BOOLEAN_ATTRIBUTES("candlepin.boolean_attributes"),
    IDENTITY_CERT_YEAR_ADDENDUM("candlepin.identityCert.yr.addendum"),
    /**
     * Identity certificate expiry threshold in days
     */
    IDENTITY_CERT_EXPIRY_THRESHOLD("candlepin.identityCert.expiry.threshold"),

    SWAGGER_ENABLED("candlepin.swagger.enabled"),

    /** Enabled dev page used to interactively login to a Keycloak instance and generate offline token. */
    TOKENPAGE_ENABLED("candlepin.tokenpage.enabled"),

    /** Path to keycloak.json */
    KEYCLOAK_FILEPATH("candlepin.keycloak.config"),

    // Async Job Properties and utilities
    ASYNC_JOBS_NODE_NAME("candlepin.async.node_name"),
    ASYNC_JOBS_THREADS("candlepin.async.threads"),
    ASYNC_JOBS_SCHEDULER_ENABLED("candlepin.async.scheduler.enabled"),

    ASYNC_JOBS_DISPATCH_ADDRESS("candlepin.async.dispatch_address"),
    ASYNC_JOBS_RECEIVE_ADDRESS("candlepin.async.receive_address"),
    ASYNC_JOBS_RECEIVE_FILTER("candlepin.async.receive_filter"),

    // Whether or not we should allow queuing new jobs on this node while the job manager is suspended/paused
    ASYNC_JOBS_QUEUE_WHILE_SUSPENDED("candlepin.async.queue_while_suspended"),

    // "Temporary" configuration to limit the scope of the jobs/schedule endpoint. Only job keys
    // specified in this property will be allowed to be triggered via the schedule endpoint.
    ASYNC_JOBS_TRIGGERABLE_JOBS("candlepin.async.triggerable_jobs"),

    // How long (in seconds) to wait for job threads to finish during a graceful Tomcat shutdown
    ASYNC_JOBS_THREAD_SHUTDOWN_TIMEOUT("candlepin.async.thread.shutdown.timeout"),

    // How many days to allow a product to be orphaned before removing it on the next refresh or
    // manifest import. Default: 30 days
    ORPHANED_ENTITY_GRACE_PERIOD("candlepin.refresh.orphan_entity_grace_period"),


    // Hibernate
    DB_DRIVER_CLASS(JPA_CONFIG_PREFIX + "hibernate.connection.driver_class"),
    DB_URL(JPA_CONFIG_PREFIX + "hibernate.connection.url"),
    DB_USERNAME(JPA_CONFIG_PREFIX + "hibernate.connection.username"),

    IN_OPERATOR_BLOCK_SIZE("db.config.in.operator.block.size"),
    CASE_OPERATOR_BLOCK_SIZE("db.config.case.operator.block.size"),
    BATCH_BLOCK_SIZE("db.config.batch.block.size"),
    QUERY_PARAMETER_LIMIT("db.config.query.parameter.limit"),

    // Used for per-job configuration. The full syntax is "PREFIX.{job_key}.SUFFIX". For instance,
    // to configure the schedule flag for the job TestJob1, the full configuration would be:
    // candlepin.async.jobs.TestJob1.schedule=0 0 0/3 * * ?
    ASYNC_JOBS_JOB_SCHEDULE("schedule"),

    // Special value used to denote a job's schedule should be manual rather than automatic.
    ASYNC_JOBS_MANUAL_SCHEDULE("manual"),
    ENTITLER_BULK_SIZE("entitler.bulk.size"),
    THROTTLE("throttle");


    public static final String ASYNC_JOBS_PREFIX = "candlepin.async.jobs.";

    private final String key;

    CommonConfigKey(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }

}
