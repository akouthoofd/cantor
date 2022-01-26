/*
 * Copyright (c) 2020, Salesforce.com, Inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cantor.common.performance;

import com.salesforce.cantor.Events;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertFalse;

/**
 * Performance test for Cantor Events. At the end of each test statistics for store and retrieval will be output.
 */
public abstract class AbstractBaseEventsPerformanceTest extends AbstractBaseCantorPerformanceTest {
    private final String namespace = UUID.randomUUID().toString();

    private Map<String, Percentile> percentiles;

    @BeforeMethod
    public void before() throws Exception {
        this.percentiles = new TreeMap<>();
        getEvents().create(this.namespace);
    }

    @AfterMethod
    public void after(final ITestResult result) throws Exception {
        getEvents().drop(this.namespace);
        super.printStatsTable(result.getName(), this.percentiles);
    }

    @Test(enabled = false)
    public void testManySmallEvents() throws IOException {
        final Events events = getEvents();
        final List<Events.Event> storedEvents = generateEvents(getManyCount(), 5, 10, 1024);
        for (int iteration = 0; iteration < getIterations(); iteration++) {
            getEvents().create(this.namespace);
            // batch is a single
            final Percentile storageBatchStats = doTestBatchStorageOfEvents(events, storedEvents);
            this.percentiles.put(String.format("storeBatch%d-%d", getManyCount(), iteration), storageBatchStats);

            final Percentile storageStats = doTestStorageOfEvents(events, storedEvents);
            this.percentiles.put(String.format("store%d-%d", getManyCount(), iteration), storageStats);

            final Percentile retrievalStats = doTestRetrievalOfEvents(events, storedEvents);
            this.percentiles.put(String.format("get%d-%d", getManyCount(), iteration), retrievalStats);

            final Percentile dropStats = doTestDropOfEvents(events);
            this.percentiles.put(String.format("drop%d-%d", getManyCount(), iteration), dropStats);
        }
    }

    @Test(enabled = false)
    public void testFewLargeEvents() throws IOException {
        final Events events = getEvents();
        final List<Events.Event> storedEvents = generateEvents(getFewCount(), 25, 50, 1024 * 1024);
        for (int iteration = 0; iteration < getIterations(); iteration++) {
            getEvents().create(this.namespace);
            // batch is a single
            final Percentile storageBatchStats = doTestBatchStorageOfEvents(events, storedEvents);
            this.percentiles.put(String.format("storeBatch%d-%d", getManyCount(), iteration), storageBatchStats);

            final Percentile storageStats = doTestStorageOfEvents(events, storedEvents);
            this.percentiles.put(String.format("store%d-%d", getManyCount(), iteration), storageStats);

            final Percentile retrievalStats = doTestRetrievalOfEvents(events, storedEvents);
            this.percentiles.put(String.format("get%d-%d", getManyCount(), iteration), retrievalStats);

            final Percentile dropStats = doTestDropOfEvents(events);
            this.percentiles.put(String.format("drop%d-%d", getManyCount(), iteration), dropStats);
        }
    }

    @Test(enabled = true)
    public void testFewSmallEvents() throws IOException {
        final Events events = getEvents();
        for (int iteration = 0; iteration < getIterations(); iteration++) {
            final List<Events.Event> storedEvents = generateEvents(getFewCount(), 5, 10, 0);
            getEvents().create(this.namespace);
            // batch is a single
            final Percentile storageBatchStats = doTestBatchStorageOfEvents(events, storedEvents);
            this.percentiles.put(String.format("storeBatch%d-%d", getFewCount(), iteration), storageBatchStats);

//            final Percentile storageStats = doTestStorageOfEvents(events, storedEvents);
//            this.percentiles.put(String.format("store%d-%d", getManyCount(), iteration), storageStats);

            final Percentile retrievalStats = doTestRetrievalOfEvents(events, storedEvents, getFewCount());
            this.percentiles.put(String.format("get%d-%d", getFewCount(), iteration), retrievalStats);

            final Percentile dropStats = doTestDropOfEvents(events);
            this.percentiles.put(String.format("drop%d-%d", getFewCount(), iteration), dropStats);
            rotate();
        }
    }

    @Test(enabled = false)
    public void testManyLargeEvents() throws IOException {
        final Events events = getEvents();
        final List<Events.Event> storedEvents = generateEvents(getManyCount(), 25, 50, 1024 * 1024);
        for (int iteration = 0; iteration < getIterations(); iteration++) {
            getEvents().create(this.namespace);
            // batch is a single
            final Percentile storageBatchStats = doTestBatchStorageOfEvents(events, storedEvents);
            this.percentiles.put(String.format("storeBatch%d-%d", getManyCount(), iteration), storageBatchStats);

            final Percentile storageStats = doTestStorageOfEvents(events, storedEvents);
            this.percentiles.put(String.format("store%d-%d", getManyCount(), iteration), storageStats);

            final Percentile retrievalStats = doTestRetrievalOfEvents(events, storedEvents);
            this.percentiles.put(String.format("get%d-%d", getManyCount(), iteration), retrievalStats);

            final Percentile dropStats = doTestDropOfEvents(events);
            this.percentiles.put(String.format("drop%d-%d", getManyCount(), iteration), dropStats);
        }
    }

    // override the number of times the performance test runs
    protected int getIterations() {
        return 9;
    }

    // override change the count stored by a "many" performance test
    protected int getManyCount() {
        return 3_000;
    }

    // override change the count stored by a "few" performance test
    int index = 0;
    final int[] counts = new int[]{12, 24, 7 * 24};
    private void rotate() {
        index = (index == counts.length - 1) ? 0 : index + 1;
    }

    protected int getFewCount() {
        return counts[index];
    }

    private Percentile doTestDropOfEvents(final Events events) throws IOException {
        logger.info("calling events.drop() for namespace {}", this.namespace);
        final long initialTimestamp = System.nanoTime();
        events.drop(this.namespace);
        final long finalTimestamp = System.nanoTime();

        logger.info("drop total duration: {}ms", TimeUnit.NANOSECONDS.toMillis(finalTimestamp - initialTimestamp));
        final Percentile percentile = new Percentile();
        percentile.setData(new double[] {TimeUnit.NANOSECONDS.toMillis(finalTimestamp - initialTimestamp)});
        return percentile;
    }

    private Percentile doTestBatchStorageOfEvents(final Events events, final List<Events.Event> storedEvents) throws IOException {
        logger.info("calling events.store(batch) for {} events in batches of 100", storedEvents.size());
        final double[] storageStats = new double[(storedEvents.size() / 100) + 1];
        final long initialTimestamp = System.nanoTime();
        for (int eventIndex = 0, statIndex = 0; eventIndex < storedEvents.size(); eventIndex += 100, statIndex++) {
            final List<Events.Event> batch = storedEvents.subList(eventIndex, Math.min(eventIndex + 100, storedEvents.size()));
            logger.debug("calling events.store(batch) for {} events {}-{}", batch.size(), eventIndex, Math.min(eventIndex + 100, storedEvents.size() - 1));

            final long startTimestamp = System.nanoTime();
            events.store(this.namespace, batch);
            final long afterStoreTimestamp = System.nanoTime();

            final long duration = TimeUnit.NANOSECONDS.toMillis(afterStoreTimestamp - startTimestamp);
            logger.debug("took {}ms to store 100 events", duration);
            storageStats[statIndex] = duration;
        }
        final long finalTimestamp = System.nanoTime();

        logger.info("batch store total duration: {}ms", TimeUnit.NANOSECONDS.toMillis(finalTimestamp - initialTimestamp));
        logger.debug("raw event store durations: {}", Arrays.toString(storageStats));
        final Percentile percentile = new Percentile();
        percentile.setData(storageStats);
        return percentile;
    }

    private Percentile doTestStorageOfEvents(final Events events, final List<Events.Event> storedEvents) throws IOException {
        logger.info("calling events.store() for {} events", storedEvents.size());
        final double[] storageStats = new double[storedEvents.size()];
        final long initialTimestamp = System.nanoTime();
        for (int eventIndex = 0; eventIndex < storedEvents.size(); eventIndex++) {

            final long startTimestamp = System.nanoTime();
            events.store(this.namespace, storedEvents.get(eventIndex));
            final long afterStoreTimestamp = System.nanoTime();

            final long duration = TimeUnit.NANOSECONDS.toMillis(afterStoreTimestamp - startTimestamp);
            storageStats[eventIndex] = duration;
        }
        final long finalTimestamp = System.nanoTime();

        logger.info("store total duration: {}ms", TimeUnit.NANOSECONDS.toMillis(finalTimestamp - initialTimestamp));
        logger.debug("raw event store durations: {}", Arrays.toString(storageStats));
        final Percentile percentile = new Percentile();
        percentile.setData(storageStats);
        return percentile;
    }

    private Percentile doTestRetrievalOfEvents(final Events events, final List<Events.Event> storedEvents) throws IOException {
        return doTestRetrievalOfEvents(events, storedEvents, 100);
    }

    private Percentile doTestRetrievalOfEvents(final Events events, final List<Events.Event> storedEvents, final int batchCount) throws IOException {
        logger.info("calling events.get() to retrieve {} events in {} event batches", storedEvents.size(), batchCount);
        final double[] storageStats = new double[(storedEvents.size() / batchCount) + 1];
        final long initialTimestamp = System.nanoTime();
        for (int eventIndex = batchCount - 1, statIndex = 0; eventIndex < storedEvents.size(); eventIndex += batchCount, statIndex++) {
            final Events.Event batchStart = storedEvents.get(eventIndex - (batchCount - 1));
            final Events.Event batchEnd = storedEvents.get(Math.min(eventIndex, storedEvents.size() - 1));
            final long startTimestamp = System.nanoTime();
            events.get(this.namespace, batchStart.getTimestampMillis(), batchEnd.getTimestampMillis(), true);
            final long afterStoreTimestamp = System.nanoTime();

            final long duration = TimeUnit.NANOSECONDS.toMillis(afterStoreTimestamp - startTimestamp);
            storageStats[statIndex] = duration;
        }
        final long finalTimestamp = System.nanoTime();

        logger.info("retrieval total duration: {}ms", TimeUnit.NANOSECONDS.toMillis(finalTimestamp - initialTimestamp));
        logger.debug("raw event retrieval durations: {}", Arrays.toString(storageStats));
        final Percentile percentile = new Percentile();
        percentile.setData(storageStats);
        return percentile;
    }

    private static final long time = 1642982400000L;
    private List<Events.Event> generateEvents(final int eventCount, final int originRandom, final int boundRandom, final int payloadSize) {
        final List<Events.Event> events = new ArrayList<>();
        final int metadataCount = ThreadLocalRandom.current().nextInt(originRandom, boundRandom);
        final int dimensionCount = ThreadLocalRandom.current().nextInt(originRandom, boundRandom);
        long timestamp = time;
        for (int i = 0; i < eventCount; ++i) {
            final Map<String, String> metadata = getRandomMetadata(metadataCount);
            final Map<String, Double> dimensions = getRandomDimensions(dimensionCount);
            final byte[] payload = getRandomPayload(0);
            events.add(new Events.Event(timestamp, metadata, dimensions, payload));
            timestamp += TimeUnit.HOURS.toMillis(1);
        }

        return events;
    }

    private Map<String, Double> getRandomDimensions(final int count) {
        final Map<String, Double> dimensions = new HashMap<>();
        for (int i = 0; i < count; ++i) {
            dimensions.put("random-keys-like-!@#$%^&*()_+=-/?,>,<`-are-all-accepted; " +
                    "even really really unnecessarily long keys like this is acceptable!..." + i, ThreadLocalRandom.current().nextDouble());
        }
        return dimensions;
    }

    private Map<String, String> getRandomMetadata(final int count) {
        final Map<String, String> metadata = new HashMap<>();
        for (int i = 0; i < count; ++i) {
            metadata.put("random-keys-like-!@#$%^&*()_+=-/?,>,<`-are-all-accepted; " +
                    "even really really unnecessarily long keys like this is acceptable!..." + i, UUID.randomUUID().toString());
        }
        return metadata;
    }

    private byte[] getRandomPayload(final int size) {
        final byte[] buffer = new byte[size];
        new Random().nextBytes(buffer);
        return buffer;
    }

    private Events getEvents() throws IOException {
        return getCantor().events();
    }
}
