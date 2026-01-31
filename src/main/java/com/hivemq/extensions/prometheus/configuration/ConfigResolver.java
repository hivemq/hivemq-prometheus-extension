/*
 * Copyright 2018-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.extensions.prometheus.configuration;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class ConfigResolver implements Supplier<Path> {

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(ConfigResolver.class);

    private final @NotNull AtomicBoolean legacyWarningAlreadyLogged = new AtomicBoolean();
    private final @NotNull Path extensionHome;
    private final @NotNull String extensionName;
    private final @NotNull String configLocation;
    private final @NotNull String legacyConfigLocation;

    public ConfigResolver(
            final @NotNull Path extensionHome,
            final @NotNull String extensionName,
            final @NotNull String configLocation,
            final @NotNull String legacyConfigLocation) {
        this.extensionHome = extensionHome;
        this.extensionName = extensionName;
        this.configLocation = configLocation;
        this.legacyConfigLocation = legacyConfigLocation;
    }

    @Override
    public @NotNull Path get() {
        final Path configPath = extensionHome.resolve(configLocation);
        final Path legacyPath = extensionHome.resolve(legacyConfigLocation);

        // if config is present at the legacy location, use it (with warning)
        // the only way it could be there is when deliberately placed
        if (legacyPath.toFile().exists()) {
            if (!legacyWarningAlreadyLogged.getAndSet(true)) {
                LOG.warn("{}: The configuration file '{}' is placed at the legacy location. " +
                                "Please move the configuration file to '{}'. " +
                                "Support for the legacy location will be removed in a future release.",
                        extensionName,
                        legacyPath,
                        configPath);
            }
            return legacyPath;
        }
        return configPath;
    }
}
