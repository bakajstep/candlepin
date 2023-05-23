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

import org.candlepin.async.tasks.ActiveEntitlementJob;
import org.candlepin.async.tasks.CertificateCleanupJob;
import org.candlepin.async.tasks.EntitlerJob;
import org.candlepin.async.tasks.ExpiredPoolsCleanupJob;
import org.candlepin.async.tasks.ImportRecordCleanerJob;
import org.candlepin.async.tasks.InactiveConsumerCleanerJob;
import org.candlepin.async.tasks.JobCleaner;
import org.candlepin.async.tasks.ManifestCleanerJob;
import org.candlepin.async.tasks.OrphanCleanupJob;
import org.candlepin.async.tasks.UnmappedGuestEntitlementCleanerJob;
import org.candlepin.guice.DBManagementLevel;

import java.util.HashMap;
import java.util.Map;



/**
 * Defines a map of default properties used to prepopulate the {@link Configuration}.
 * Also holds static keys for config lookup.
 */
public class ConfigProperties {
    private ConfigProperties() {
    }

    public static final String DEFAULT_CONFIG_FILE = "/etc/candlepin/candlepin.conf";

    public static final String[] ENCRYPTED_PROPERTIES = new String[] {
        CommonConfigKey.DB_PASSWORD.key(),
    };

    private static final String INTEGER_FACT_LIST = "";

    private static final String NON_NEG_INTEGER_FACT_LIST =
        "cpu.core(s)_per_socket," +
        "cpu.cpu(s)," +
        "cpu.cpu_socket(s)," +
        "lscpu.core(s)_per_socket," +
        "lscpu.cpu(s)," +
        "lscpu.numa_node(s)," +
        "lscpu.socket(s)," +
        "lscpu.thread(s)_per_core";

    private static final String INTEGER_ATTRIBUTE_LIST = "";

    private static final String NON_NEG_INTEGER_ATTRIBUTE_LIST =
        "sockets," +
        "warning_period," +
        "ram," +
        "cores";

    private static final String LONG_ATTRIBUTE_LIST = "";

    private static final String NON_NEG_LONG_ATTRIBUTE_LIST = "metadata_expire";

    private static final String BOOLEAN_ATTRIBUTE_LIST =
        "management_enabled," +
        "virt_only";

    // Special value used to denote a job's schedule should be manual rather than automatic.
    public static final String ASYNC_JOBS_MANUAL_SCHEDULE = "manual";

    public static final String[] ASYNC_JOBS_TRIGGERABLE_JOBS_LIST = new String[] {
        ActiveEntitlementJob.JOB_KEY,
        CertificateCleanupJob.JOB_KEY,
        ExpiredPoolsCleanupJob.JOB_KEY,
        ImportRecordCleanerJob.JOB_KEY,
        JobCleaner.JOB_KEY,
        ManifestCleanerJob.JOB_KEY,
        OrphanCleanupJob.JOB_KEY,
        UnmappedGuestEntitlementCleanerJob.JOB_KEY,
        InactiveConsumerCleanerJob.JOB_KEY
    };

    public static final Map<String, String> DEFAULT_PROPERTIES = new HashMap<>() {
        private static final long serialVersionUID = 1L;

        {
            this.put(CommonConfigKey.CANDLEPIN_URL.key(), "https://localhost");

            this.put(CommonConfigKey.JWT_ISSUER.key(), "Candlepin");
            this.put(CommonConfigKey.JWT_TOKEN_TTL.key(), "600"); // seconds

            this.put(CommonConfigKey.CA_KEY.key(), "/etc/candlepin/certs/candlepin-ca.key");
            this.put(CommonConfigKey.CA_CERT.key(), "/etc/candlepin/certs/candlepin-ca.crt");
            this.put(CommonConfigKey.CA_CERT_UPSTREAM.key(), "/etc/candlepin/certs/upstream");

            this.put(CommonConfigKey.CPM_PROVIDER.key(), "artemis");

            this.put(CommonConfigKey.ACTIVEMQ_ENABLED.key(), "true");
            this.put(CommonConfigKey.ACTIVEMQ_EMBEDDED.key(), "true");

            // TODO: Delete the above configuration and only use EMBEDDED_BROKER going forward.
            // This is currently blocked by complexity in supporting moving configuration paths,
            // and should coincide with an update to the Configuration object
            this.put(CommonConfigKey.ACTIVEMQ_EMBEDDED_BROKER.key(), "true");
            this.put(CommonConfigKey.ACTIVEMQ_JAAS_INVM_LOGIN_NAME.key(), "InVMLogin");
            this.put(CommonConfigKey.ACTIVEMQ_JAAS_CERTIFICATE_LOGIN_NAME.key(), "CertificateLogin");

            // By default, connect to embedded artemis (InVM)
            this.put(CommonConfigKey.ACTIVEMQ_BROKER_URL.key(), "vm://0");

            // By default use the broker.xml file that is packaged in the war.
            this.put(CommonConfigKey.ACTIVEMQ_SERVER_CONFIG_PATH.key(), "");
            this.put(CommonConfigKey.ACTIVEMQ_LARGE_MSG_SIZE.key(), Integer.toString(100 * 1024));
            this.put(CommonConfigKey.ACTIVEMQ_CONNECTION_MONITOR_INTERVAL.key(), "5000"); // milliseconds

            this.put(CommonConfigKey.AUDIT_LISTENERS.key(),
                "org.candlepin.audit.LoggingListener," +
                "org.candlepin.audit.ActivationListener");
            this.put(CommonConfigKey.AUDIT_FILTER_ENABLED.key(), "false");

            this.put(CommonConfigKey.ENTITLER_BULK_SIZE.key(), "1000");

            // These default DO_NOT_FILTER events are those events needed by other Satellite components.
            this.put(CommonConfigKey.AUDIT_FILTER_DO_NOT_FILTER.key(),
                "CREATED-ENTITLEMENT," +
                "DELETED-ENTITLEMENT," +
                "CREATED-POOL," +
                "DELETED-POOL," +
                "CREATED-COMPLIANCE");

            this.put(CommonConfigKey.AUDIT_FILTER_DO_FILTER.key(), "");
            this.put(CommonConfigKey.AUDIT_FILTER_DEFAULT_POLICY.key(), "DO_FILTER");

            this.put(CommonConfigKey.PRETTY_PRINT.key(), "false");

            this.put(CommonConfigKey.SYNC_WORK_DIR.key(), "/var/cache/candlepin/sync");
            this.put(CommonConfigKey.CONSUMER_FACTS_MATCHER.key(), ".*");
            this.put(CommonConfigKey.TRUSTED_AUTHENTICATION.key(), "false");
            this.put(CommonConfigKey.SSL_AUTHENTICATION.key(), "true");
            this.put(CommonConfigKey.OAUTH_AUTHENTICATION.key(), "false");
            this.put(CommonConfigKey.KEYCLOAK_AUTHENTICATION.key(), "false");
            this.put(CommonConfigKey.BASIC_AUTHENTICATION.key(), "true");
            this.put(CommonConfigKey.CLOUD_AUTHENTICATION.key(), "false");
            this.put(CommonConfigKey.ACTIVATION_KEY_AUTHENTICATION.key(), "true");

            this.put(CommonConfigKey.AUTH_OVER_HTTP.key(), "false");
            // By default, environments should be hidden so clients do not need to
            // submit one when registering.
            this.put(CommonConfigKey.HIDDEN_RESOURCES.key(), "environments");
            this.put(CommonConfigKey.HIDDEN_CAPABILITIES.key(), "");
            this.put(CommonConfigKey.HALT_ON_LIQUIBASE_DESYNC.key(), "true");
            this.put(CommonConfigKey.DB_MANAGE_ON_START.key(), DBManagementLevel.NONE.getName());
            this.put(CommonConfigKey.SSL_VERIFY.key(), "false");

            this.put(CommonConfigKey.FAIL_ON_UNKNOWN_IMPORT_PROPERTIES.key(), "false");

            this.put(CommonConfigKey.CACHE_JMX_STATS.key(), "false");
            this.put(CommonConfigKey.CACHE_CONFIG_FILE_URI.key(), "ehcache.xml");

            this.put(CommonConfigKey.SUSPEND_MODE_ENABLED.key(), "true");

            this.put(CommonConfigKey.IDENTITY_CERT_YEAR_ADDENDUM.key(), "5");
            this.put(CommonConfigKey.IDENTITY_CERT_EXPIRY_THRESHOLD.key(), "90");
            this.put(CommonConfigKey.SHARD_WEBAPP.key(), "candlepin");

            // defaults
            // FIXME unused
            this.put(CommonConfigKey.SHARD_USERNAME.key(), "admin");
            this.put(CommonConfigKey.SHARD_PASSWORD.key(), "admin");
            this.put(CommonConfigKey.STANDALONE.key(), "true");

            this.put(CommonConfigKey.ENV_CONTENT_FILTERING.key(), "true");
            this.put(CommonConfigKey.USE_SYSTEM_UUID_FOR_MATCHING.key(), "true");

            // what constitutes a valid consumer name
            this.put(CommonConfigKey.CONSUMER_SYSTEM_NAME_PATTERN.key(),
                "[\\#\\?\\'\\`\\!@{}()\\[\\]\\?&\\w-\\.]+");
            this.put(CommonConfigKey.CONSUMER_PERSON_NAME_PATTERN.key(),
                "[\\#\\?\\'\\`\\!@{}()\\[\\]\\?&\\w-\\.]+");

            this.put(CommonConfigKey.PREFIX_WEBURL.key(), "localhost:8443/candlepin");
            this.put(CommonConfigKey.PREFIX_APIURL.key(), "localhost:8443/candlepin");
            // FIXME unused
            this.put(CommonConfigKey.PASSPHRASE_SECRET_FILE.key(), "/etc/katello/secure/passphrase");
            this.put(CommonConfigKey.KEYCLOAK_FILEPATH.key(), "/etc/candlepin/keycloak.json");

            /**
             *  Defines the maximum number of products allowed in the product cache.
             *  On deployments with a large number of products, it might be better
             *  to set this to a large number, keeping in mind that it will yield
             *  a larger memory footprint as the cache fills up.
             */
            this.put(CommonConfigKey.PRODUCT_CACHE_MAX.key(), "100");

            /** As we do math on some facts and attributes, we need to constrain some values */
            this.put(CommonConfigKey.INTEGER_FACTS.key(), INTEGER_FACT_LIST);
            this.put(CommonConfigKey.NON_NEG_INTEGER_FACTS.key(), NON_NEG_INTEGER_FACT_LIST);
            this.put(CommonConfigKey.INTEGER_ATTRIBUTES.key(), INTEGER_ATTRIBUTE_LIST);
            this.put(CommonConfigKey.NON_NEG_INTEGER_ATTRIBUTES.key(), NON_NEG_INTEGER_ATTRIBUTE_LIST);
            this.put(CommonConfigKey.LONG_ATTRIBUTES.key(), LONG_ATTRIBUTE_LIST);
            this.put(CommonConfigKey.NON_NEG_LONG_ATTRIBUTES.key(), NON_NEG_LONG_ATTRIBUTE_LIST);
            this.put(CommonConfigKey.BOOLEAN_ATTRIBUTES.key(), BOOLEAN_ATTRIBUTE_LIST);

            this.put(CommonConfigKey.SWAGGER_ENABLED.key(), Boolean.toString(true));
            this.put(CommonConfigKey.TOKENPAGE_ENABLED.key(), Boolean.toString(true));

            // Async job defaults and scheduling
            // Quartz scheduling bits
            this.put("org.quartz.scheduler.skipUpdateCheck", "true");
            this.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
            this.put("org.quartz.threadPool.threadCount", "15");
            this.put("org.quartz.threadPool.threadPriority", "5");

            this.put(CommonConfigKey.ASYNC_JOBS_THREADS.key(), "10");
            this.put(CommonConfigKey.ASYNC_JOBS_QUEUE_WHILE_SUSPENDED.key(), "true");
            this.put(CommonConfigKey.ASYNC_JOBS_SCHEDULER_ENABLED.key(), "true");
            this.put(CommonConfigKey.ASYNC_JOBS_THREAD_SHUTDOWN_TIMEOUT.key(), "600"); // 10 minutes

            this.put(CommonConfigKey.ASYNC_JOBS_DISPATCH_ADDRESS.key(), "job");
            this.put(CommonConfigKey.ASYNC_JOBS_RECEIVE_ADDRESS.key(), "jobs");
            this.put(CommonConfigKey.ASYNC_JOBS_RECEIVE_FILTER.key(), "");

            // ActiveEntitlementJob
            this.put(JobConfigKey.SCHEDULE.keyForJob(ActiveEntitlementJob.JOB_KEY).key(),
                ActiveEntitlementJob.DEFAULT_SCHEDULE);

            // CertificateCleanupJob
            this.put(JobConfigKey.SCHEDULE.keyForJob(CertificateCleanupJob.JOB_KEY).key(),
                CertificateCleanupJob.DEFAULT_SCHEDULE);

            // EntitlerJob
            this.put(JobConfigKey.THROTTLE.keyForJob(EntitlerJob.JOB_KEY).key(),
                EntitlerJob.DEFAULT_THROTTLE);

            // ExpiredPoolsCleanupJob
            this.put(JobConfigKey.SCHEDULE.keyForJob(ExpiredPoolsCleanupJob.JOB_KEY).key(),
                ExpiredPoolsCleanupJob.DEFAULT_SCHEDULE);

            // ImportRecordCleanerJob
            this.put(JobConfigKey.SCHEDULE.keyForJob(ImportRecordCleanerJob.JOB_KEY).key(),
                ImportRecordCleanerJob.DEFAULT_SCHEDULE);
            this.put(JobConfigKey.KEEP.keyForJob(ImportRecordCleanerJob.JOB_KEY).key(),
                ImportRecordCleanerJob.DEFAULT_KEEP);

            // InactiveConsumeCleanerJob
            this.put(JobConfigKey.SCHEDULE.keyForJob(InactiveConsumerCleanerJob.JOB_KEY).key(),
                ConfigProperties.ASYNC_JOBS_MANUAL_SCHEDULE);
            this.put(JobConfigKey.BATCH_SIZE.keyForJob(InactiveConsumerCleanerJob.JOB_KEY).key(),
                InactiveConsumerCleanerJob.DEFAULT_BATCH_SIZE);
            this.put(JobConfigKey.LAST_CHECKED_IN_RETENTION.keyForJob(InactiveConsumerCleanerJob.JOB_KEY).key(),
                Integer.toString(InactiveConsumerCleanerJob.DEFAULT_LAST_CHECKED_IN_RETENTION_IN_DAYS));
            this.put(JobConfigKey.LAST_UPDATED_IN_RETENTION.keyForJob(InactiveConsumerCleanerJob.JOB_KEY).key(),
                Integer.toString(InactiveConsumerCleanerJob.DEFAULT_LAST_UPDATED_IN_RETENTION_IN_DAYS));

            // JobCleaner
            this.put(JobConfigKey.SCHEDULE.keyForJob(JobCleaner.JOB_KEY).key(),
                JobCleaner.DEFAULT_SCHEDULE);
            this.put(JobConfigKey.MAX_TERMINAL_JOB_AGE.keyForJob(JobCleaner.JOB_KEY).key(),
                JobCleaner.DEFAULT_MAX_TERMINAL_AGE);
            this.put(JobConfigKey.MAX_NON_TERMINAL_JOB_AGE.keyForJob(JobCleaner.JOB_KEY).key(),
                JobCleaner.DEFAULT_MAX_NONTERMINAL_AGE);
            this.put(JobConfigKey.MAX_RUNNING_JOB_AGE.keyForJob(JobCleaner.JOB_KEY).key(),
                JobCleaner.DEFAULT_MAX_RUNNING_AGE);

            // ManifestCleanerJob
            this.put(JobConfigKey.SCHEDULE.keyForJob(ManifestCleanerJob.JOB_KEY).key(),
                ManifestCleanerJob.DEFAULT_SCHEDULE);
            this.put(JobConfigKey.MAX_MANIFEST_AGE.keyForJob(ManifestCleanerJob.JOB_KEY).key(),
                Integer.toString(ManifestCleanerJob.DEFAULT_MAX_AGE_IN_MINUTES));

            // OrphanCleanupJob
            this.put(JobConfigKey.SCHEDULE.keyForJob(OrphanCleanupJob.JOB_KEY).key(),
                OrphanCleanupJob.DEFAULT_SCHEDULE);

            // UnmappedGuestEntitlementCleanerJob
            this.put(JobConfigKey.SCHEDULE.keyForJob(UnmappedGuestEntitlementCleanerJob.JOB_KEY).key(),
                UnmappedGuestEntitlementCleanerJob.DEFAULT_SCHEDULE);

            // Set the triggerable jobs list
            this.put(CommonConfigKey.ASYNC_JOBS_TRIGGERABLE_JOBS.key(),
                String.join(", ", ASYNC_JOBS_TRIGGERABLE_JOBS_LIST));

            this.put(CommonConfigKey.ORPHANED_ENTITY_GRACE_PERIOD.key(), "30");

            // Based on testing with the hypervisor check in process, and going a bit conservative
            this.put(CommonConfigKey.IN_OPERATOR_BLOCK_SIZE.key(), "15000");
            this.put(CommonConfigKey.CASE_OPERATOR_BLOCK_SIZE.key(), "100");
            this.put(CommonConfigKey.BATCH_BLOCK_SIZE.key(), "500");
            this.put(CommonConfigKey.QUERY_PARAMETER_LIMIT.key(), "32000");

            // FIXME
            this.put(CommonConfigKey.ASYNC_JOBS_NODE_NAME.key(), "");
        }
    };
}
