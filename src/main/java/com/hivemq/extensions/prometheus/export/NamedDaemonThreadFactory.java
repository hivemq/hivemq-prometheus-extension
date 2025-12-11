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

package com.hivemq.extensions.prometheus.export;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link ThreadFactory} implementation that creates threads with custom names and daemon status,
 * specifically for use with the Prometheus HTTP server.
 * <p>
 * This factory wraps a delegate {@link ThreadFactory} and assigns each thread a unique name in the format
 * {@code prometheus-http-<poolNumber>-<threadNumber>}. It also allows threads to be created as daemon
 * threads.
 * <p>
 * <b>Why daemon threads?</b>
 * <p>
 * The Prometheus HTTP server is typically used for exposing metrics and should not prevent the JVM from shutting down
 * when the main application threads have terminated. By creating the HTTP server's threads as daemon threads,
 * we ensure that they do not block JVM shutdown, allowing for graceful application termination without requiring
 * explicit shutdown of the metrics server.
 *
 * @author David Sondermann
 */
class NamedDaemonThreadFactory implements ThreadFactory {

    private static final @NotNull AtomicInteger POOL_NUMBER = new AtomicInteger(1);

    private final int poolNumber = POOL_NUMBER.getAndIncrement();
    private final @NotNull AtomicInteger threadNumber = new AtomicInteger(1);

    private final @NotNull ThreadFactory delegate;
    private final boolean daemon;

    NamedDaemonThreadFactory(final @NotNull ThreadFactory delegate, final boolean daemon) {
        this.delegate = delegate;
        this.daemon = daemon;
    }

    @Override
    public Thread newThread(final @NotNull Runnable r) {
        final var thread = delegate.newThread(r);
        thread.setName(String.format("prometheus-http-%d-%d", poolNumber, threadNumber.getAndIncrement()));
        thread.setDaemon(daemon);
        return thread;
    }

    static ThreadFactory defaultThreadFactory() {
        return new NamedDaemonThreadFactory(Executors.defaultThreadFactory(), true);
    }
}
