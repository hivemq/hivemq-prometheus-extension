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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.aeonbits.owner.Config;
import org.jetbrains.annotations.Nullable;

import static org.aeonbits.owner.Config.DisableableFeature.PARAMETER_FORMATTING;
import static org.aeonbits.owner.Config.DisableableFeature.VARIABLE_EXPANSION;

@Config.DisableFeature({VARIABLE_EXPANSION, PARAMETER_FORMATTING})
public interface PrometheusExtensionConfiguration extends Config {

    @NotNull String METRIC_PATH_KEY = "metric_path";
    @NotNull String IP_KEY = "ip";
    @NotNull String PORT_KEY = "port";

    @Key(PORT_KEY)
    int port();

    @Key(IP_KEY)
    @Nullable String hostIp();

    @Key(METRIC_PATH_KEY)
    @Nullable String metricPath();
}
