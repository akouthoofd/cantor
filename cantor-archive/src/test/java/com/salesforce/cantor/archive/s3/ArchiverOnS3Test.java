package com.salesforce.cantor.archive.s3;

import com.adobe.testing.s3mock.testng.S3Mock;
import com.adobe.testing.s3mock.testng.S3MockListener;
import com.amazonaws.services.s3.AmazonS3;
import com.salesforce.cantor.Cantor;
import com.salesforce.cantor.Events;
import com.salesforce.cantor.archive.TestUtils;
import com.salesforce.cantor.h2.CantorOnH2;
import com.salesforce.cantor.misc.archivable.impl.ArchivableCantor;
import com.salesforce.cantor.s3.CantorOnS3;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Listeners(value = { S3MockListener.class })
public class ArchiverOnS3Test {
    private static final long TIMEFRAME_BOUND = System.currentTimeMillis();
    private static final long TIMEFRAME_ORIGIN = TIMEFRAME_BOUND - TimeUnit.DAYS.toMillis(2);
    private static final String H2_DIRECTORY = "/tmp/cantor-local-test";
    private static final long HOUR_MILLIS = TimeUnit.HOURS.toMillis(1);
    private static final String ARCHIVE_NAMESPACE = "events-archive";

    private Map<String, Long> cantorH2Namespaces;
    private Cantor cantorLocal;
    private CantorOnS3 cantorOnS3;
    private ArchiverOnS3 archiver;

    @BeforeMethod
    public void setUp() throws IOException {
        final AmazonS3 s3Client = createS3Client();
        this.cantorOnS3 = new CantorOnS3(s3Client, "cantor-archive-test");
        this.archiver = new ArchiverOnS3(cantorOnS3, HOUR_MILLIS);

        this.cantorLocal = new ArchivableCantor(new CantorOnH2(H2_DIRECTORY), this.archiver);
        this.cantorH2Namespaces = new HashMap<>();
        TestUtils.generateData(this.cantorLocal, TIMEFRAME_ORIGIN, TIMEFRAME_BOUND, cantorH2Namespaces);
    }

    @AfterMethod
    public void tearDown() throws IOException {
        // delete test cantor data
        for (final String cantorH2Namespace : this.cantorH2Namespaces.keySet()) {
            this.cantorLocal.events().drop(cantorH2Namespace);
        }

        for (final String namespace : this.cantorOnS3.objects().namespaces()) {
            this.cantorOnS3.objects().drop(namespace);
        }
        createS3Client().deleteBucket(String.format("%s-all-namespaces", "cantor-archive-test"));
    }

    /**
     * Recommended for testing with real s3 clients as up to 1 GB per namespace can be transferred
     */
    @Test
    public void testEventsArchiveSingleNamespace() throws IOException {
        final Map.Entry<String, Long> cantorH2Namespace = cantorH2Namespaces.entrySet().iterator().next();
        final List<Events.Event> totalEvents = this.cantorLocal.events()
                .get(cantorH2Namespace.getKey(), TIMEFRAME_ORIGIN, TIMEFRAME_BOUND);

        final List<Events.Event> events = this.cantorLocal.events()
                .get(cantorH2Namespace.getKey(), TIMEFRAME_ORIGIN, cantorH2Namespace.getValue());
        this.cantorLocal.events().expire(cantorH2Namespace.getKey(), cantorH2Namespace.getValue());

        // check that at least one archive was made (which should always be true since we generate events at timestamp 0)
        final Collection<String> archiveFilenames = this.cantorOnS3.objects().keys(ARCHIVE_NAMESPACE, 0, -1);
        final List<String> matchingArchives = ((EventsArchiverOnS3) this.archiver.events()).getMatchingArchives(cantorH2Namespace.getKey(), archiveFilenames, 0, cantorH2Namespace.getValue());
        Assert.assertNotEquals(matchingArchives.size(), 0);

        // restore the events
        final List<Events.Event> restoreEvents = this.cantorLocal.events()
                .get(cantorH2Namespace.getKey(), TIMEFRAME_ORIGIN, cantorH2Namespace.getValue());
        Assert.assertEquals(restoreEvents.size(), events.size(), "all events were not restored for namespace: " + cantorH2Namespace.getKey());

        // sanity check no events have been lost
        final List<Events.Event> totalEventsAgain = this.cantorLocal.events()
                .get(cantorH2Namespace.getKey(), TIMEFRAME_ORIGIN, TIMEFRAME_BOUND);
        Assert.assertEquals(totalEventsAgain.size(), totalEvents.size(), "more events were expired than were archived for namespace: " + cantorH2Namespace.getKey());
    }

    @Test
    public void testEventsArchive() throws IOException {
        for (final Map.Entry<String, Long> cantorH2Namespace : this.cantorH2Namespaces.entrySet()) {
            final List<Events.Event> totalEvents = this.cantorLocal.events()
                    .get(cantorH2Namespace.getKey(), TIMEFRAME_ORIGIN, TIMEFRAME_BOUND);

            final long endTimestamp = TestUtils.getFloorForWindow(cantorH2Namespace.getValue(), HOUR_MILLIS) - 1;
            final List<Events.Event> events = this.cantorLocal.events()
                    .get(cantorH2Namespace.getKey(), TIMEFRAME_ORIGIN, cantorH2Namespace.getValue());
            this.cantorLocal.events().expire(cantorH2Namespace.getKey(), cantorH2Namespace.getValue());

            // check that at least one archive was made (which should always be true since we generate events at timestamp 0)
            final Collection<String> archiveFilenames = this.cantorOnS3.objects().keys(ARCHIVE_NAMESPACE, 0, -1);
            final List<String> matchingArchives = ((EventsArchiverOnS3) this.archiver.events()).getMatchingArchives(cantorH2Namespace.getKey(), archiveFilenames, 0, cantorH2Namespace.getValue());
            Assert.assertNotEquals(matchingArchives.size(), 0);

            // restore the events
            final List<Events.Event> restoreEvents = this.cantorLocal.events()
                    .get(cantorH2Namespace.getKey(), TIMEFRAME_ORIGIN, cantorH2Namespace.getValue());
            Assert.assertEquals(restoreEvents.size(), events.size(), "all events were not restored for namespace: " + cantorH2Namespace.getKey());

            // sanity check no events have been lost
            final List<Events.Event> totalEventsAgain = this.cantorLocal.events()
                    .get(cantorH2Namespace.getKey(), TIMEFRAME_ORIGIN, TIMEFRAME_BOUND);
            Assert.assertEquals(totalEventsAgain.size(), totalEvents.size(), "more events were expired than were archived for namespace: " + cantorH2Namespace.getKey());
        }
    }

    // insert real S3 client here to run integration testing
    private AmazonS3 createS3Client() {
        return S3Mock.getInstance().createS3Client("us-west-1");
    }
}