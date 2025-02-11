/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dromara.dynamictp.starter.apollo.refresher;

import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.ConfigFileChangeListener;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.model.ConfigFileChangeEvent;
import com.ctrip.framework.apollo.spring.config.PropertySourcesConstants;
import com.google.common.base.Splitter;
import lombok.extern.slf4j.Slf4j;
import org.dromara.dynamictp.common.em.ConfigFileTypeEnum;
import org.dromara.dynamictp.core.refresher.AbstractRefresher;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;

/**
 * ApolloRefresher related
 * <p>Listen for configuration file changes</p>
 *
 * @author yanhom
 * @since 1.0.0
 **/
@Slf4j
public class ApolloRefresher extends AbstractRefresher implements ConfigFileChangeListener, InitializingBean {

    private static final Splitter NAMESPACE_SPLITTER = Splitter.on(",").omitEmptyStrings().trimResults();

    @Override
    public void onChange(ConfigFileChangeEvent changeEvent) {
        String namespace = changeEvent.getNamespace();
        String newValue = changeEvent.getNewValue();
        ConfigFileFormat configFileFormat = determineFileFormat(namespace);
        ConfigFileTypeEnum configFileType = ConfigFileTypeEnum.of(configFileFormat.getValue());
        refresh(newValue, configFileType);
    }

    @Override
    public void afterPropertiesSet() {
        String namespaces = environment.getProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_NAMESPACES, ConfigConsts.NAMESPACE_APPLICATION);
        log.debug("Apollo bootstrap namespaces: {}", namespaces);
        List<String> namespaceList = NAMESPACE_SPLITTER.splitToList(namespaces);

        for (String namespace : namespaceList) {
            ConfigFileFormat format = determineFileFormat(namespace);
            String actualNamespaceName = trimNamespaceFormat(namespace, format);
            ConfigFile configFile = ConfigService
                    .getConfigFile(actualNamespaceName, format);
            try {
                configFile.addChangeListener(this);
                log.info("DynamicTp refresher, add listener success, namespace: {}", actualNamespaceName);
            } catch (Exception e) {
                log.error("DynamicTp refresher, add listener error, namespace: {}", actualNamespaceName, e);
            }
        }
    }

    private ConfigFileFormat determineFileFormat(String namespaceName) {
        String lowerCase = namespaceName.toLowerCase();
        for (ConfigFileFormat format : ConfigFileFormat.values()) {
            if (lowerCase.endsWith("." + format.getValue())) {
                return format;
            }
        }

        return ConfigFileFormat.Properties;
    }

    private String trimNamespaceFormat(String namespaceName, ConfigFileFormat format) {
        String extension = "." + format.getValue();
        if (!namespaceName.toLowerCase().endsWith(extension)) {
            return namespaceName;
        }

        return namespaceName.substring(0, namespaceName.length() - extension.length());
    }

}