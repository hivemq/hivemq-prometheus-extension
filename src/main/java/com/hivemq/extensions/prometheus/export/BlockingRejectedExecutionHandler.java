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

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * A {@link RejectedExecutionHandler} implementation that blocks the calling thread
 * when the executor's task queue is full, instead of rejecting the task.
 * <p>
 * Unlike standard {@code RejectedExecutionHandler} implementations (such as {@code AbortPolicy}),
 * this handler attempts to put the rejected task into the queue, blocking until space becomes available.
 * This can be useful in scenarios where task loss is unacceptable and backpressure is desired.
 *
 * @author David Sondermann
 */
class BlockingRejectedExecutionHandler implements RejectedExecutionHandler {

    @Override
    public void rejectedExecution(
            final @NotNull Runnable runnable,
            final @NotNull ThreadPoolExecutor threadPoolExecutor) {
        if (!threadPoolExecutor.isShutdown()) {
            try {
                threadPoolExecutor.getQueue().put(runnable);
            } catch (final InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
