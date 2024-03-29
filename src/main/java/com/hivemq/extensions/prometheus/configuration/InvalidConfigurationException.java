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

/**
 * An exception which indicates a wrong configuration, e.g. not putting an mandatory field, wrong values, ...
 *
 * @author Daniel Krüger
 */
public class InvalidConfigurationException extends Exception {

    private static final long serialVersionUID = -6216153002463951736L;

    public InvalidConfigurationException(final @NotNull String message) {
        super(message);
    }
}
