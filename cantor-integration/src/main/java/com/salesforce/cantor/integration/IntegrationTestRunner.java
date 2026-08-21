package com.salesforce.cantor.integration;

import com.salesforce.cantor.Cantor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.TestNG;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class IntegrationTestRunner {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationTestRunner.class);

    public static void main(String[] args) {

        String version = "";
        for (int i = 0; i < args.length; i++) {
            if ("--type".equals(args[i]) && i + 1 < args.length) {
                version = args[i + 1];
            }
        }

        int exitCode = 0;

        final String hostEnv = System.getenv("CANTOR_SERVER_HOST");
        final String host = (hostEnv != null) ? hostEnv : "localhost";

        final String portEnv = System.getenv("CANTOR_SERVER_PORT");
        final int port = (portEnv != null) ? Integer.parseInt(portEnv) : 7443;

        ServerManager serverManager = new ServerManager(host, port);
        try {
            serverManager.connect();
            Cantor client = serverManager.getClient();

            final Class<?> testClass = Class.forName("com.salesforce.cantor.integration.IntegrationEventsTest");
            testClass.getMethod("setCantor", Cantor.class).invoke(null, client);

            final int[] eventCounts = {30, 300, 3000};

            List<BenchmarkStats> stats = new ArrayList<>();

            for (final int eventCount : eventCounts) {
                testClass.getMethod("setEventCount", int.class).invoke(null, eventCount);

                logger.info("Running tests for eventCount={}", eventCount);

                TimingListener timingListener = new TimingListener();
                TestNG testng = new TestNG();
                testng.setTestClasses(new Class<?>[]{testClass});
                testng.addListener(timingListener);
                testng.run();

                if (testng.getStatus() != 0) {
                    exitCode = 1;
                }

                Map<String, List<Long>> methods = new LinkedHashMap<>();
                for (TimingListener.TestTiming t : timingListener.getResults()) {
                    String key = t.getMethodName();
                    List<Long> values = methods.get(key);
                    if (values == null) {
                        values = new ArrayList<>();
                        methods.put(key, values);
                    }
                    values.add(t.getDurationMs());
                }

                for (Map.Entry<String, List<Long>> e : methods.entrySet()) {
                    stats.add(new BenchmarkStats(eventCount, e.getKey(), e.getValue()));
                }
            }

            final String reportsDir = "cantor-integration/reports";
            final String reportPath = reportsDir + "/" + version + ".csv";
            CSVReporter.generate(reportPath, stats);
            logger.info("Performance report generated at {}", reportPath);

            final String htmlPath = reportsDir + "/cantor-performance-metric.html";
            HTMLReporter.generate(reportsDir, htmlPath);
            logger.info("HTML report generated at {}", htmlPath);

            logger.info("All tests completed.");

        } catch (Exception e) {
            logger.error("ERROR: {}", e.getMessage(), e);
            exitCode = 1;
        }

        logger.info("Done.");

        System.exit(exitCode);
    }
}