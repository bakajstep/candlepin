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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;

public class RyeConfigTest {

    private enum TestKey implements ConfigKey {
        STRING("test.string"),
        STRING_TRIM("test.string.trim"),
        BOOL("test.bool"),
        INT("test.int"),
        LONG("test.long"),
        LIST("test.list"),
        SET("test.set");

        private final String key;

        TestKey(String key) {
            this.key = key;
        }

        @Override
        public String key() {
            return this.key;
        }
    }

    private static final Map<String, String> DEFAULTS = Map.ofEntries(
        Map.entry(TestKey.STRING.key(), "str"),
        Map.entry(TestKey.STRING_TRIM.key(), " a "),
        Map.entry(TestKey.BOOL.key(), "true"),
        Map.entry(TestKey.INT.key(), "123"),
        Map.entry(TestKey.LONG.key(), Long.toString(Long.MAX_VALUE)),
        Map.entry(TestKey.LIST.key(), "a,b, c"),
        Map.entry(TestKey.SET.key(), "a,b, c")
    );
    public static final String PREFIX = "test";

    @Test
    public void valueByPrefixFound() {
        RyeConfig ryeConfig = buildConfig(DEFAULTS);

        Map<String, String> values = ryeConfig.getValuesByPrefix(PREFIX);

        assertThat(values)
            .isNotEmpty()
            .containsEntry("test.string", "str");
    }

    @Test
    public void valueByPrefixEmpty() {
        RyeConfig ryeConfig = buildEmptyConfig();

        Map<String, String> values = ryeConfig.getValuesByPrefix(PREFIX);

        assertThat(values).isEmpty();
    }

    @Test
    public void toProperties() {
        RyeConfig ryeConfig = buildConfig(DEFAULTS);

        Properties properties = ryeConfig.toProperties();

        assertThat(properties)
            .containsEntry("test.string", "str");
    }

    @Test
    public void toPropertiesEmpty() {
        RyeConfig ryeConfig = buildEmptyConfig();

        Properties properties = ryeConfig.toProperties();

        assertThat(properties).isEmpty();
    }

    @Test
    public void listKeys() {
        RyeConfig ryeConfig = buildConfig(DEFAULTS);

        Iterable<String> keys = ryeConfig.getKeys();

        assertThat(keys).contains(TestKey.STRING.key());
    }

    @Test
    public void listKeysEmpty() {
        RyeConfig ryeConfig = buildEmptyConfig();

        Properties properties = ryeConfig.toProperties();

        assertThat(properties).isEmpty();
    }

    @Test
    public void stringValueFound() {
        RyeConfig ryeConfig = buildConfig(DEFAULTS);

        String value = ryeConfig.getString(TestKey.STRING);

        assertThat(value)
            .isEqualTo("str");
    }

    @Test
    public void stringValueMissing() {
        RyeConfig ryeConfig = buildEmptyConfig();

        String value = ryeConfig.getString(TestKey.STRING);

        assertThat(value).isNull();
    }

    @Test
    public void stringValueTrimmed() {
        RyeConfig ryeConfig = buildConfig(DEFAULTS);

        String value = ryeConfig.getString(TestKey.STRING_TRIM);

        assertThat(value).isEqualTo("a");
    }

    @Test
    public void boolValueFound() {
        RyeConfig ryeConfig = buildConfig(DEFAULTS);

        boolean value = ryeConfig.getBoolean(TestKey.BOOL);

        assertThat(value).isTrue();
    }

    @Test
    public void boolValueMissing() {
        RyeConfig ryeConfig = buildEmptyConfig();

        assertThatThrownBy(() -> ryeConfig.getBoolean(TestKey.BOOL))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    public void intValueFound() {
        RyeConfig ryeConfig = buildConfig(DEFAULTS);

        int value = ryeConfig.getInt(TestKey.INT);

        assertThat(value).isEqualTo(123);
    }

    @Test
    public void intValueMissing() {
        RyeConfig ryeConfig = buildEmptyConfig();

        assertThatThrownBy(() -> ryeConfig.getBoolean(TestKey.INT))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    public void longValueFound() {
        RyeConfig ryeConfig = buildConfig(DEFAULTS);

        long value = ryeConfig.getLong(TestKey.LONG);

        assertThat(value).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    public void longValueMissing() {
        RyeConfig ryeConfig = buildEmptyConfig();

        assertThatThrownBy(() -> ryeConfig.getLong(TestKey.LONG))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    public void listFound() {
        RyeConfig ryeConfig = buildConfig(DEFAULTS);

        List<String> value = ryeConfig.getList(TestKey.LIST);

        assertThat(value)
            .containsExactly("a", "b", "c");
    }

    @Test
    public void listMissing() {
        RyeConfig ryeConfig = buildEmptyConfig();

        List<String> value = ryeConfig.getList(TestKey.LIST);

        assertThat(value).isEmpty();
    }

    @Test
    public void setFound() {
        RyeConfig ryeConfig = buildConfig(DEFAULTS);

        Set<String> value = ryeConfig.getSet(TestKey.SET);

        assertThat(value)
            .containsExactly("a", "b", "c");
    }

    @Test
    public void setMissing() {
        RyeConfig ryeConfig = buildEmptyConfig();

        Set<String> value = ryeConfig.getSet(TestKey.SET);

        assertThat(value).isEmpty();
    }

    private RyeConfig buildEmptyConfig() {
        return buildConfig(Map.of());
    }

    private RyeConfig buildConfig(Map<String, String> defaults) {
        SmallRyeConfig smallRyeConfig = new SmallRyeConfigBuilder()
            .withDefaultValues(defaults)
            .build();
        return new RyeConfig(smallRyeConfig);
    }
}
