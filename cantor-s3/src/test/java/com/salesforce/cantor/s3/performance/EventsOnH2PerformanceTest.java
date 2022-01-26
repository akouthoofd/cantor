/*
 * Copyright (c) 2020, Salesforce.com, Inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cantor.s3.performance;

import com.amazonaws.auth.*;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.salesforce.cantor.Cantor;
import com.salesforce.cantor.common.performance.AbstractBaseEventsPerformanceTest;
import com.salesforce.cantor.s3.CantorOnS3;

import java.io.*;
import java.util.Scanner;

public class EventsOnH2PerformanceTest extends AbstractBaseEventsPerformanceTest {
    private static final String credentialsLocation = "/Users/akouthoofd/.aws/credentials";

    @Override
    protected Cantor getCantor() throws IOException {
        final AmazonS3 s3Client = createS3Client();
        return new CantorOnS3(s3Client, "warden-cantor--monitoring--dev1--us-west-2--dev");
    }

    // insert real S3 client here to run integration testing
    private AmazonS3 createS3Client() throws IOException {

            return AmazonS3ClientBuilder.standard().withRegion(Regions.US_WEST_2)
                    .build();
    }
}
