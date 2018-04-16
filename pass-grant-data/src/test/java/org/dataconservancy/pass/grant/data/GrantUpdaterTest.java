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

import org.dataconservancy.pass.client.fedora.FedoraPassClient;
import org.dataconservancy.pass.model.Funder;
import org.dataconservancy.pass.model.Grant;
import org.dataconservancy.pass.model.Person;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.dataconservancy.pass.grant.data.CoeusFieldNames.*;
import static org.dataconservancy.pass.grant.data.GrantUpdater.returnLaterUpdate;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test class for building the {@code List} of {@code Grant}s
 *
 * @author jrm@jhu.edu
 */
@RunWith(MockitoJUnitRunner.class)
public class GrantUpdaterTest {

    @Mock
    private FedoraPassClient fedoraClientMock;

    private URI grantUri;
    private URI piUri;
    private URI coPiUri;
    private URI directFunderUri;
    private URI primaryFunderUri;

        
    @Before
    public void setup() {
        grantUri = URI.create("grantUri");
        piUri = URI.create("piUri");
        coPiUri = URI.create("coPiUri");
        directFunderUri = URI.create("directFunderUri");
        primaryFunderUri = URI.create("primaryFunderUri");

        when(fedoraClientMock.createResource(any(Grant.class))).thenReturn(grantUri);
        when(fedoraClientMock.createResource(any(Funder.class))).thenReturn(directFunderUri, primaryFunderUri);
        when(fedoraClientMock.createResource(any(Person.class))).thenReturn(piUri, coPiUri);
    }
    /**
     * Test static timestamp utility method to verify it returns the later of two supplied timestamps
     */
    @Test
    public void testReturnLatestUpdate() {
        String baseString = "1980-01-01 00:00:00.0";
        String earlyDate  = "2018-01-02 03:04:05.0";
        String laterDate  = "2018-01-02 04:08:09.0";

        String latestDate = returnLaterUpdate(baseString, earlyDate);
        Assert.assertEquals(earlyDate, latestDate);
        latestDate = returnLaterUpdate(latestDate, laterDate);
        Assert.assertEquals(laterDate, latestDate);

        Assert.assertEquals(earlyDate, returnLaterUpdate(earlyDate, earlyDate));
    }

    @Test
    public void testGrantBuilding() {

        Set<Map<String, String>> resultSet = new HashSet<>();

        String awardNumber = "12345678";
        String awardStatus = "Active";
        String localAwardId = "Active";
        String projectName =  "Moo Project";
        String awardDate = "01/01/2000";
        String startDate = "01/01/2001";
        String endDate =  "01/01/2002";
        String directFunderId = "000029282";
        String directFunderName =  "JHU Department of Synergy";
        String primaryFunderId = "8675309";
        String primaryFunderName = "J. L. Gotrocks Foundation";

        Map<String, String> rowMap = new HashMap<>();
        rowMap.put(C_GRANT_AWARD_NUMBER, awardNumber);
        rowMap.put(C_GRANT_AWARD_STATUS, awardStatus);
        rowMap.put(C_GRANT_LOCAL_AWARD_ID, localAwardId);
        rowMap.put(C_GRANT_PROJECT_NAME, projectName);
        rowMap.put(C_GRANT_AWARD_DATE, awardDate);
        rowMap.put(C_GRANT_START_DATE, startDate);
        rowMap.put(C_GRANT_END_DATE,endDate);

        rowMap.put(C_DIRECT_FUNDER_LOCAL_ID, directFunderId);
        rowMap.put(C_DIRECT_FUNDER_NAME, directFunderName);
        rowMap.put(C_PRIMARY_FUNDER_LOCAL_ID, primaryFunderId);
        rowMap.put(C_PRIMARY_FUNDER_NAME, primaryFunderName);

        rowMap.put(C_PERSON_FIRST_NAME, "Amanda");
        rowMap.put(C_PERSON_MIDDLE_NAME, "Beatrice");
        rowMap.put(C_PERSON_LAST_NAME, "Reckondwith");
        rowMap.put(C_PERSON_EMAIL, "areckon3@jhu.edu");
        rowMap.put(C_PERSON_INSTITUTIONAL_ID, "ARECKON3");

        rowMap.put(C_UPDATE_TIMESTAMP, "2018-01-01 0:00:00.0");
        rowMap.put(C_ABBREVIATED_ROLE, "P");

        resultSet.add(rowMap);

        rowMap = new HashMap<>();
        rowMap.put(C_GRANT_AWARD_NUMBER, awardNumber);
        rowMap.put(C_GRANT_AWARD_STATUS, awardStatus);
        rowMap.put(C_GRANT_LOCAL_AWARD_ID, localAwardId);
        rowMap.put(C_GRANT_PROJECT_NAME, projectName);
        rowMap.put(C_GRANT_AWARD_DATE, awardDate);
        rowMap.put(C_GRANT_START_DATE, startDate);
        rowMap.put(C_GRANT_END_DATE, endDate);

        rowMap.put(C_DIRECT_FUNDER_LOCAL_ID, directFunderId);
        rowMap.put(C_DIRECT_FUNDER_NAME, directFunderName);
        rowMap.put(C_PRIMARY_FUNDER_LOCAL_ID, primaryFunderId);
        rowMap.put(C_PRIMARY_FUNDER_NAME, primaryFunderName);

        rowMap.put(C_PERSON_FIRST_NAME, "Marsha");
        rowMap.put(C_PERSON_MIDDLE_NAME, null);
        rowMap.put(C_PERSON_LAST_NAME, "Lartz");
        rowMap.put(C_PERSON_EMAIL, "alartz3@jhu.edu");
        rowMap.put(C_PERSON_INSTITUTIONAL_ID, "MLARTZ5");

        rowMap.put(C_UPDATE_TIMESTAMP, "2018-01-01 0:00:00.0");
        rowMap.put(C_ABBREVIATED_ROLE, "C");

        resultSet.add(rowMap);

        GrantUpdater grantUpdater = new GrantUpdater(fedoraClientMock);
        grantUpdater.updateGrants(resultSet);

        Map<URI, Grant> grantMap = grantUpdater.getGrantUriMap();
        Assert.assertEquals(1, grantMap.size());
        Grant grant = grantMap.get(grantUri);
        Assert.assertEquals(piUri, grant.getPi());
        Assert.assertEquals(1, grant.getCoPis().size());
        Assert.assertEquals(coPiUri, grant.getCoPis().get(0));
        Assert.assertEquals(2, grantUpdater.getFunderMap().size());
        Assert.assertEquals(directFunderUri, grantUpdater.getFunderMap().get(directFunderId));
        Assert.assertEquals(primaryFunderUri, grantUpdater.getFunderMap().get(primaryFunderId));
        Assert.assertEquals(grant.getDirectFunder(),directFunderUri);
        Assert.assertEquals(grant.getPrimaryFunder(),primaryFunderUri);
        Assert.assertEquals(piUri, grantUpdater.getPersonMap().get("areckon3"));//store as lower case
        Assert.assertEquals(coPiUri, grantUpdater.getPersonMap().get("mlartz5"));//store as lower case

        Assert.assertEquals(awardNumber, grant.getAwardNumber());
        Assert.assertEquals(Grant.AwardStatus.ACTIVE, grant.getAwardStatus());
        Assert.assertEquals(localAwardId, grant.getLocalAwardId());
        Assert.assertEquals(DateTimeUtil.createJodaDateTime(awardDate), grant.getAwardDate());
        Assert.assertEquals(DateTimeUtil.createJodaDateTime(startDate), grant.getStartDate());
        Assert.assertEquals(DateTimeUtil.createJodaDateTime(endDate), grant.getEndDate());
        Assert.assertEquals(projectName, grant.getProjectName());
    }
}
