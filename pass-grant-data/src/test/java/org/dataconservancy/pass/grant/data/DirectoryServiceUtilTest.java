/*
 * Copyright 2018 Johns Hopkins University
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

package org.dataconservancy.pass.grant.data;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Properties;

import static org.dataconservancy.pass.grant.data.DirectoryServiceUtil.DIRECTORY_SERVICE_BASE_URL;
import static org.dataconservancy.pass.grant.data.DirectoryServiceUtil.DIRECTORY_SERVICE_CLIENT_ID;
import static org.dataconservancy.pass.grant.data.DirectoryServiceUtil.DIRECTORY_SERVICE_CLIENT_SECRET;


/**
 * This is a test class for a simple directory lookup service running at the endpoint specified by "serviceUrl" below
 * the service type completes the URL, and the client id and client secret are supplied as headers.
 *
 * values for serviceUrl, clientId and slientSecret, must be supplied below.
 *
 * This test has been run against the running service with valid parameters and arguments supplied to the methods - this class has been
 * cleaned up after successful testing. because of the simplicity and isolation of this class, it does not need to be tested
 * every build - just when something about the service changes. so we ignore it for now.
 */
@Ignore
public class DirectoryServiceUtilTest {
    private DirectoryServiceUtil underTest;

    @Before
    public void setup() {
        String serviceUrl = "https://the.service/url";
        final String clientId = "the-client-id";
        final String clientSecret = "the-client-secret";
        Properties connectionProperties = new Properties();
        connectionProperties.setProperty(DIRECTORY_SERVICE_BASE_URL, serviceUrl);
        connectionProperties.setProperty(DIRECTORY_SERVICE_CLIENT_ID, clientId);
        connectionProperties.setProperty(DIRECTORY_SERVICE_CLIENT_SECRET, clientSecret);
        underTest = new DirectoryServiceUtil(connectionProperties);
    }

    @Test
    public void testGetHopkinsId() throws java.io.IOException {
        String result = underTest.getHopkinsIdForEmployeeId("supply valid argument here");
        Assert.assertEquals("expected value", result);
        System.out.println(result);

    }

    @Test
    public void testGetEmployeeId() throws java.io.IOException {
        String result = underTest.getEmployeeIdForHopkinsId("A58756");
        Assert.assertEquals("expected value", result);
    }

    @Test
    public void testGetEmployeeIdIsNull() throws IOException {
        String result = underTest.getEmployeeIdForHopkinsId("SomeBadValue");
        Assert.assertNull(result);
    }
}
