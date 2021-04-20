/**
 * Copyright (c) 2009 - 2012 Red Hat, Inc.
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
package org.candlepin.sync;

import org.candlepin.common.config.Configuration;
import org.candlepin.config.ConfigProperties;
import org.candlepin.dto.ModelTranslator;
import org.candlepin.dto.manifest.v1.ConsumerDTO;
import org.candlepin.model.Consumer;

import com.google.inject.Inject;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Consumer - maps to the consumer.json file
 */
public class ConsumerExporter {

    private final Configuration config;
    private final FileExporter fileExporter;
    private final ModelTranslator translator;

    @Inject
    public ConsumerExporter(Configuration config, FileExporter fileExporter, ModelTranslator translator) {
        this.config = config;
        this.fileExporter = fileExporter;
        this.translator = translator;
    }

    public void exportTo(Path exportDir, Consumer consumer, String webAppPrefix, String apiUrl)
        throws IOException {

        Path export = exportDir.resolve("consumer.json");
        ConsumerDTO consumerDTO = createDto(consumer, webAppPrefix, apiUrl);
        this.fileExporter.exportTo(export, consumerDTO);
    }

    private ConsumerDTO createDto(Consumer consumer, String webAppPrefix, String apiUrl) {
        ConsumerDTO consumerDTO = this.translator.translate(consumer, ConsumerDTO.class);
        consumerDTO.setUrlApi(getPrefixApiUrl(apiUrl));
        consumerDTO.setUrlWeb(getPrefixWebUrl(webAppPrefix));
        return consumerDTO;
    }

    private String getPrefixWebUrl(String override) {
        return getWithOverride(override, ConfigProperties.PREFIX_WEBURL);
    }

    private String getPrefixApiUrl(String override) {
        return getWithOverride(override, ConfigProperties.PREFIX_APIURL);
    }

    private String getWithOverride(String override, String key) {
        String prefixApiUrl = config.getString(key);
        if (!StringUtils.isBlank(override)) {
            return override;
        }

        if (StringUtils.isBlank(prefixApiUrl)) {
            return null;
        }

        return prefixApiUrl;
    }

}
