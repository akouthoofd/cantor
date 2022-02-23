/*
 * Copyright (c) 2020, Salesforce.com, Inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cantor.misc.loggable;

import com.salesforce.cantor.Namespaceable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Callable;

import static com.salesforce.cantor.common.CommonPreconditions.checkCreate;
import static com.salesforce.cantor.common.CommonPreconditions.checkDrop;

abstract class AbstractBaseLoggableNamespaceable<T extends Namespaceable> implements Namespaceable {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final T delegate;

    public AbstractBaseLoggableNamespaceable(final T delegate) {
        this.delegate = delegate;
    }

    @Override
    public final void create(final String namespace) throws IOException {
        checkCreate(namespace);
        logCall(() -> { getDelegate().create(namespace); return null; },
                "create", namespace
        );
    }

    @Override
    public final void drop(final String namespace) throws IOException {
        checkDrop(namespace);
        logCall(() -> { getDelegate().drop(namespace); return null; },
                "drop", namespace
        );
    }

    <R> R logCall(final Callable<R> callable,
                  final String methodName,
                  final String namespace,
                  final Object... parameters) throws IOException {
        final long startNanos = System.nanoTime();
        try {
            return callable.call();
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            logger.info("'{}.{}('{}', {}); time spent: {}ms",
                    getClass().getSimpleName(),
                    methodName,
                    namespace,
                    parameters,
                    ((System.nanoTime() - startNanos) / 1_000_000)  // nanos to millis
            );
        }
    }

    protected T getDelegate() {
        return this.delegate;
    }
}
