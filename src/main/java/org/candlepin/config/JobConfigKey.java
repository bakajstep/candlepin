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


public enum JobConfigKey implements ConfigKey {

    // Used for per-job configuration. The full syntax is "PREFIX.{job_key}.SUFFIX". For instance,
    // to configure the schedule flag for the job TestJob1, the full configuration would be:
    // candlepin.async.jobs.TestJob1.schedule=0 0 0/3 * * ?
    SCHEDULE("schedule"),
    THROTTLE("throttle"),
    KEEP("num_of_records_to_keep"),
    BATCH_SIZE("batch_size"),
    LAST_CHECKED_IN_RETENTION("last_checked_in_retention_in_days"),
    LAST_UPDATED_IN_RETENTION("last_updated_retention_in_days"),
    MAX_TERMINAL_JOB_AGE("max_terminal_job_age"),
    MAX_NON_TERMINAL_JOB_AGE("max_nonterminal_job_age"),
    MAX_RUNNING_JOB_AGE("max_running_job_age"),
    MAX_MANIFEST_AGE("max_age_in_minutes");

    public static final String ASYNC_JOBS_PREFIX = "candlepin.async.jobs.";

    private final String key;

    JobConfigKey(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }

    public ConfigKey keyForJob(String jobKey) {
        return () -> ASYNC_JOBS_PREFIX + jobKey + "." + this.key;
    }

}
