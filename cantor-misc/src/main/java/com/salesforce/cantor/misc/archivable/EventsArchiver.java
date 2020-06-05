package com.salesforce.cantor.misc.archivable;

import com.salesforce.cantor.Events;
import com.salesforce.cantor.misc.archivable.impl.ArchivableEvents;

import java.io.IOException;
import java.util.Map;

/**
 * EventsArchiver is the contract used by {@link ArchivableEvents} when handling the archiving of events
 */
public interface EventsArchiver {

    /**
     *  Checks for the existence of any archives that hold events in the provided timeframe.
     *  <br><br>
     *  {@code hasArchives()} is expected to return {@literal true} if even one archived chunk overlaps with the timeframe,
     *  and {@literal false} otherwise.
     *  <br><br>
     *  Depending on the implementation {@code restore()} could be called even if the exact query doesn't match any
     *  events in the archive.
     */
    boolean hasArchives(String namespace,
                        long startTimestampMillis,
                        long endTimestampMillis);

    /**
     * Will retrieve and archive all events before the provided timestamp.
     * <br><br>
     * {@code archive()} may store events past {@code endTimestampMillis} as chunking of the data into buckets may be used.
     * <br><br>
     * It will depend on the implementation.
     */
    default void archive(Events events,
                         String namespace,
                         long endTimestampMillis) throws IOException {
        archive(events, namespace, 0, endTimestampMillis, null, null);
    }

    /**
     * Will retrieve and archive all events using these given parameters and load this into the destination.
     * <br><br>
     * {@code archive()} may store events before {@code startTimestampMillis} and past {@code endTimestampMillis}
     * as chunking of the data into buckets may be used.
     * <br><br>
     * It will depend on the implementation.
     */
    void archive(Events events,
                 String namespace,
                 long startTimestampMillis,
                 long endTimestampMillis,
                 Map<String, String> metadataQuery,
                 Map<String, String> dimensionsQuery) throws IOException;

    /**
     * Will retrieve all archived chunks between the provided timestamps and load them back into Cantor.
     * <br><br>
     * {@code restore()} may pull more events down than what are between the {@code startTimestampMillis} and
     * {@code endTimestampMillis} as it may restore surrounding data if the implementation uses chunking.
     * <br><br>
     * It will depend on the implementation
     */
    void restore(Events events,
                 String namespace,
                 long startTimestampMillis,
                 long endTimestampMillis) throws IOException;
}
