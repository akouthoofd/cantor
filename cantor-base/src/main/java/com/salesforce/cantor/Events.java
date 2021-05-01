/*
 * Copyright (c) 2020, Salesforce.com, Inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cantor;

import java.io.IOException;
import java.util.*;

/**
 * Events are multi-dimensional time series data.
 *
 * {
 *   timestamp = 1521860720690,
 *   dimensions = ['cpu'=0.5, 'mem'=0.13],
 *   metadata: ['ip'='10.0.137.21', 'data-center'='dc-1'],
 *   payload: "arbitrary byte array"
 * }
 *
 * Implementations of this interface provide functionalities to store and retrieve
 */
public interface Events extends Namespaceable {

    /**
     * An event consists of a timestamp in milli-seconds, a map of string to string as metadata, and a map of string
     * to double value as dimensions.
     */
    class Event {
        private final long timestampMillis;
        private final Map<String, String> metadata;
        private final Map<String, Double> dimensions;
        private final byte[] payload;

        public Event(final long timestampMillis,
                     final Map<String, String> metadata,
                     final Map<String, Double> dimensions) {
            this(timestampMillis, metadata, dimensions, null);
        }

        public Event(final long timestampMillis,
                     final Map<String, String> metadata,
                     final Map<String, Double> dimensions,
                     final byte[] payload) {
            this.timestampMillis = timestampMillis;
            this.metadata = metadata != null ? metadata : Collections.emptyMap();
            this.dimensions = dimensions != null ? dimensions : Collections.emptyMap();
            this.payload = payload;
        }

        /**
         * Get the UTC timestamp for this event.
         * @return timestamp of the event in milli-seconds
         */
        public long getTimestampMillis() {
            return this.timestampMillis;
        }

        /**
         * Get the metadata for this event.
         * @return map of string to string, representing all metadata for this event
         */
        public Map<String, String> getMetadata() {
            return this.metadata;
        }

        /**
         * Get the dimensions for this event (note that events are multi-dimensional).
         * @return map of string to double, representing all dimensions for this event
         */
        public Map<String, Double> getDimensions() {
            return this.dimensions;
        }

        /**
         * Get the payload attached to this event.
         * @return byte array representing payload for this event
         */
        public byte[] getPayload() {
            return this.payload;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Event)) {
                return false;
            }
            final Event other = (Event) obj;
            return this.getTimestampMillis() == other.getTimestampMillis()
                    && this.getMetadata().equals(other.getMetadata())
                    && this.getDimensions().equals(other.getDimensions())
                    && Arrays.equals(this.getPayload(), other.getPayload());
        }

        @Override
        public String toString() {
            return "timestampMillis=" + getTimestampMillis() +
                    ",dimensions=" + getDimensions() +
                    ",metadata=" + getMetadata() +
                    ",payload=" + Arrays.toString(getPayload());
        }
    }

    /**
     * Store an event in the given namespace, with timestamp and optionally metadata and dimensions.
     *
     * @param namespace the namespace identifier
     * @param timestampMillis event's UTC timestamp in milli-seconds
     * @param metadata metadata to be attached to the event
     * @param dimensions dimensions to be attached to the event
     * @throws IOException exception thrown from the underlying storage implementation
     */
    default void store(String namespace,
                       long timestampMillis,
                       Map<String, String> metadata,
                       Map<String, Double> dimensions) throws IOException {
        store(namespace, timestampMillis, metadata, dimensions, null);
    }

    /**
     * Store an event object.
     *
     * @param namespace the namespace identifier
     * @param event the event object
     * @throws IOException exception thrown from the underlying storage implementation
     */
    default void store(String namespace, Event event) throws IOException {
        store(namespace, event.getTimestampMillis(), event.getMetadata(), event.getDimensions(), event.getPayload());
    }

    /**
     * Store an event in the given namespace, with timestamp and optionally metadata and dimensions, along with an
     * arbitrary byte array as payload.
     *
     * @param namespace       the namespace identifier
     * @param timestampMillis event's UTC timestamp in milli-seconds
     * @param metadata        metadata to be attached to the event
     * @param dimensions      dimensions to be attached to the event
     * @param payload         payload to be attached to the event
     * @throws IOException exception thrown from the underlying storage implementation
     */
    default void store(String namespace,
                       long timestampMillis,
                       Map<String, String> metadata,
                       Map<String, Double> dimensions,
                       byte[] payload) throws IOException {
        store(namespace, Collections.singleton(new Event(timestampMillis, metadata, dimensions, payload)));
    }

    /**
     * Store batch of events in the given namespace.
     *
     * @param namespace the namespace identifier
     * @param batch batch of events
     * @throws IOException exception thrown from the underlying storage implementation
     */
    void store(String namespace, Collection<Event> batch) throws IOException;

    /**
     * Get all events in the given namespace, with timestamp between the start and end.
     *
     * @param namespace the namespace identifier
     * @param startTimestampMillis start UTC timestamp in milli-seconds
     * @param endTimestampMillis end UTC timestamp in milli-seconds
     * @return list of all events in the namespace with timestamp between start/end
     * @throws IOException exception thrown from the underlying storage implementation
     */
    default List<Event> get(String namespace,
                            long startTimestampMillis,
                            long endTimestampMillis) throws IOException {
        return get(namespace, startTimestampMillis, endTimestampMillis, false);
    }

    /**
     * Get all events in the given namespace, with timestamp between the start and end.
     *
     * @param namespace the namespace identifier
     * @param startTimestampMillis start UTC timestamp in milli-seconds
     * @param endTimestampMillis end UTC timestamp in milli-seconds
     * @param includePayloads flag to include payloads in the response or not; if false, event.getPayload() returns null
     * @return list of all events in the namespace with timestamp between start/end
     * @throws IOException exception thrown from the underlying storage implementation
     */
    default List<Event> get(String namespace,
                            long startTimestampMillis,
                            long endTimestampMillis,
                            boolean includePayloads) throws IOException {
        return get(namespace, startTimestampMillis, endTimestampMillis, null, null, includePayloads, true, 0);
    }

    /**
     * Get all events in the given namespace, with timestamp between the start and end, and metadata and dimensions
     * matching the given queries.
     * <p>
     * A metadata query can match against an exact value for a key, or use wild-card
     * character (i.e., '*'). For example: "host" => "localhost" matches with all events where the value of metadata
     * with key "host" is exactly "localhost"; and the query "host" => "~prod-*-example" matches all events where the
     * value is "like 'prod-*-example'". Note that wild-card queries have to start with "~", exact queries can
     * optionally start with "=".
     * <p>
     * A dimensions query can match against exact values or less-than, less-than-or-equal, more-than, more-than-or-equal
     * values for a set of dimensions. For example: "cpu" => ">=0.3" matches all events where the dimension with key
     * "cpu" has a value higher than or equal to 0.3.
     *
     * @param namespace            the namespace identifier
     * @param startTimestampMillis start UTC timestamp in milli-seconds
     * @param endTimestampMillis   end UTC timestamp in milli-seconds
     * @param metadataQuery        map of string to string representing a query to run against events metadata
     * @param dimensionsQuery      map of string to string representing a query to run against events dimensions
     * @return list of all events in the namespace with timestamp between start/end
     * and metadata/dimensions matching the query
     * @throws IOException exception thrown from the underlying storage implementation
     */
    default List<Event> get(String namespace,
                            long startTimestampMillis,
                            long endTimestampMillis,
                            Map<String, String> metadataQuery,
                            Map<String, String> dimensionsQuery) throws IOException {
        return get(namespace, startTimestampMillis, endTimestampMillis, metadataQuery, dimensionsQuery, false, true, 0);
    }

    /**
     * Get all events in the given namespace, with timestamp between the start and end, and metadata and dimensions
     * matching the given queries.
     *
     * A metadata query can match against an exact value for a key, or use wild-card
     * character (i.e., '*'). For example: "host" => "localhost" matches with all events where the value of metadata
     * with key "host" is exactly "localhost"; and the query "host" => "~prod-*-example" matches all events where the
     * value is "like 'prod-*-example'". Note that wild-card queries have to start with "~", exact queries can
     * optionally start with "=".
     *
     * A dimensions query can match against exact values or less-than, less-than-or-equal, more-than, more-than-or-equal
     * values for a set of dimensions. For example: "cpu" => ">=0.3" matches all events where the dimension with key
     * "cpu" has a value higher than or equal to 0.3.
     *
     * @param namespace the namespace identifier
     * @param startTimestampMillis start UTC timestamp in milli-seconds
     * @param endTimestampMillis end UTC timestamp in milli-seconds
     * @param metadataQuery map of string to string representing a query to run against events metadata
     * @param dimensionsQuery map of string to string representing a query to run against events dimensions
     * @param includePayloads flag to include payloads in the response or not; if false, event.getPayload() returns null
     * @return list of all events in the namespace with timestamp between start/end
     * and metadata/dimensions matching the query
     * @throws IOException exception thrown from the underlying storage implementation
     */
    default List<Event> get(String namespace,
                            long startTimestampMillis,
                            long endTimestampMillis,
                            Map<String, String> metadataQuery,
                            Map<String, String> dimensionsQuery,
                            boolean includePayloads) throws IOException {
        return get(namespace, startTimestampMillis, endTimestampMillis, metadataQuery, dimensionsQuery, includePayloads, true, 0);
    }

    /**
     * Get all events in the given namespace, with timestamp between the start and end, and metadata and dimensions
     * matching the given queries.
     *
     * A metadata query can match against an exact value for a key, or use wild-card
     * character (i.e., '*'). For example: "host" => "localhost" matches with all events where the value of metadata
     * with key "host" is exactly "localhost"; and the query "host" => "~prod-*-example" matches all events where the
     * value is "like 'prod-*-example'". Note that wild-card queries have to start with "~", exact queries can
     * optionally start with "=".
     *
     * A dimensions query can match against exact values or less-than, less-than-or-equal, more-than, more-than-or-equal
     * values for a set of dimensions. For example: "cpu" => ">=0.3" matches all events where the dimension with key
     * "cpu" has a value higher than or equal to 0.3.
     *
     * @param namespace the namespace identifier
     * @param startTimestampMillis start UTC timestamp in milli-seconds
     * @param endTimestampMillis end UTC timestamp in milli-seconds
     * @param metadataQuery map of string to string representing a query to run against events metadata
     * @param dimensionsQuery map of string to string representing a query to run against events dimensions
     * @param includePayloads flag to include payloads in the response or not; if false, event.getPayload() returns null
     * @param ascending order results ascending if true; descending if false
     * @param limit maximum number of events to return
     * @return list of all events in the namespace with timestamp between start/end
     * and metadata/dimensions matching the query
     * @throws IOException exception thrown from the underlying storage implementation
     */
    List<Event> get(String namespace,
                    long startTimestampMillis,
                    long endTimestampMillis,
                    Map<String, String> metadataQuery,
                    Map<String, String> dimensionsQuery,
                    boolean includePayloads,
                    boolean ascending,
                    int limit) throws IOException;

    /**
     * Get distinct metadata values for the given metadata key for events in the given namespace, with timestamp between
     * the start and end, metadata and dimensions matching the given queries.
     *
     * @param namespace the namespace identifier
     * @param metadataKey the given metadata key
     * @param startTimestampMillis start timestamp in milli-seconds
     * @param endTimestampMillis end timestamp in milli-seconds
     * @param metadataQuery map of string to string representing a query to run against events metadata
     * @param dimensionsQuery map of string to string representing a query to run against events dimensions
     * @return set of metadata values for all events in the namespace with timestamp
     * between start/end and metadata/dimensions matching the query
     * @throws IOException exception thrown from the underlying storage implementation
     */
    Set<String> metadata(String namespace,
                         String metadataKey,
                         long startTimestampMillis,
                         long endTimestampMillis,
                         Map<String, String> metadataQuery,
                         Map<String, String> dimensionsQuery) throws IOException;

    /**
     *
     * @param namespace the namespace identifier
     * @param dimensionKey the given dimension key
     * @param startTimestampMillis start timestamp in milli-seconds
     * @param endTimestampMillis end timestamp in milli-seconds
     * @param metadataQuery map of string to string representing a query to run against events metadata
     * @param dimensionsQuery map of string to string representing a query to run against events dimensions
     * @return list of events where each event only contains the specified dimension - no payload or metadata is
     * included, in the namespace with timestamp between start/end and metadata/dimensions matching the query
     * @throws IOException exception thrown from the underlying storage implementation
     */
    List<Event> dimension(String namespace,
                          String dimensionKey,
                          long startTimestampMillis,
                          long endTimestampMillis,
                          Map<String, String> metadataQuery,
                          Map<String, String> dimensionsQuery) throws IOException;

    /**
     * Expire all events with timestamp before the given end timestamp.
     *
     * @param endTimestampMillis end timestamp in milli-seconds
     * @throws IOException exception thrown from the underlying storage implementation
     */
    void expire(String namespace, long endTimestampMillis) throws IOException;
}

