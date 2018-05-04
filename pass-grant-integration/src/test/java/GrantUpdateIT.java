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

import org.dataconservancy.pass.client.PassClient;
import org.dataconservancy.pass.client.PassClientFactory;
import org.dataconservancy.pass.grant.data.FedoraUpdateStatistics;
import org.dataconservancy.pass.grant.data.FedoraUpdater;
import org.dataconservancy.pass.grant.data.PassEntityUtil;
import org.dataconservancy.pass.model.Grant;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static java.lang.Thread.sleep;
import static org.dataconservancy.pass.grant.data.CoeusFieldNames.*;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GrantUpdateIT {

    private Set<Map<String,String>> resultSet = new HashSet<>();

    @Before
    public void setup() {

        for (int i = 0; i < 10; i++) {

            Map<String, String> rowMap = new HashMap<>();
            rowMap.put(C_GRANT_AWARD_NUMBER, C_GRANT_AWARD_NUMBER + Integer.toString(i));
            rowMap.put(C_GRANT_AWARD_STATUS, "Active");
            rowMap.put(C_GRANT_LOCAL_KEY, C_GRANT_LOCAL_KEY + Integer.toString(i));
            rowMap.put(C_GRANT_PROJECT_NAME, C_GRANT_PROJECT_NAME + Integer.toString(i));
            rowMap.put(C_GRANT_AWARD_DATE, "01/01/2000");
            rowMap.put(C_GRANT_START_DATE, "01/01/2001");
            rowMap.put(C_GRANT_END_DATE, "01/01/2002");

            rowMap.put(C_DIRECT_FUNDER_LOCAL_KEY, C_DIRECT_FUNDER_LOCAL_KEY + Integer.toString(i));
            rowMap.put(C_DIRECT_FUNDER_NAME, C_DIRECT_FUNDER_NAME + Integer.toString(i));
            rowMap.put(C_PRIMARY_FUNDER_LOCAL_KEY, C_PRIMARY_FUNDER_LOCAL_KEY + Integer.toString(i));
            rowMap.put(C_PRIMARY_FUNDER_NAME, C_PRIMARY_FUNDER_NAME + Integer.toString(i));

            rowMap.put(C_USER_FIRST_NAME, C_USER_FIRST_NAME + Integer.toString(i));
            rowMap.put(C_USER_MIDDLE_NAME, C_USER_MIDDLE_NAME + Integer.toString(i));
            rowMap.put(C_USER_LAST_NAME, C_USER_LAST_NAME + Integer.toString(i));
            rowMap.put(C_USER_EMAIL, C_USER_EMAIL + Integer.toString(i));
            rowMap.put(C_USER_INSTITUTIONAL_ID, C_USER_INSTITUTIONAL_ID + Integer.toString(i));
            rowMap.put(C_USER_LOCAL_KEY, C_USER_LOCAL_KEY + Integer.toString(i));

            rowMap.put(C_UPDATE_TIMESTAMP, "2018-01-01 0" + Integer.toString(i) + ":00:00.0");
            rowMap.put(C_ABBREVIATED_ROLE, (i%2==0?"P":"C"));

            resultSet.add(rowMap);
        }
    }

    @Test
    public void depositGrantsIT() throws InterruptedException {

        PassClient passClient = PassClientFactory.getPassClient();
        FedoraUpdater fedoraUpdater = new FedoraUpdater(passClient);
        fedoraUpdater.updateFedora(resultSet, "grant");
        FedoraUpdateStatistics statistics = fedoraUpdater.getStatistics();

        Assert.assertEquals(5, statistics.getPisAdded());
        Assert.assertEquals(5, statistics.getCoPisAdded());
        Assert.assertEquals(20, statistics.getFundersCreated());
        Assert.assertEquals(0, statistics.getFundersUpdated());
        Assert.assertEquals(10, statistics.getGrantsCreated());
        Assert.assertEquals(0, statistics.getGrantsUpdated());
        Assert.assertEquals("2018-01-01 09:00:00.0", statistics.getLatestUpdateString());
        Assert.assertEquals(10, statistics.getUsersCreated());
        Assert.assertEquals(0, statistics.getUsersUpdated());

        Assert.assertEquals(10, fedoraUpdater.getGrantUriMap().size());

        for (URI grantUri : fedoraUpdater.getGrantUriMap().keySet()) {
            Grant grant = fedoraUpdater.getGrantUriMap().get(grantUri);
            Grant fedoraGrant = fedoraUpdater.getPassClient().readResource(grantUri, Grant.class);
            Assert.assertTrue(PassEntityUtil.coeusGrantsEqual(grant, fedoraGrant));
        }


        sleep(12000);
        //try depositing the exact same resultSet. nothing should happen in Fedora
        fedoraUpdater.updateFedora(resultSet, "grant");

        Assert.assertEquals(0, statistics.getFundersCreated());
        Assert.assertEquals(0, statistics.getFundersUpdated());
        Assert.assertEquals(0, statistics.getGrantsCreated());
        Assert.assertEquals(0, statistics.getGrantsUpdated());
        Assert.assertEquals(0, statistics.getUsersCreated());
        Assert.assertEquals(0, statistics.getUsersUpdated());

        //now let's monkey with a few things
        Map<String, String> rowMap = new HashMap<>();
        rowMap.put(C_GRANT_AWARD_NUMBER, C_GRANT_AWARD_NUMBER + Integer.toString(1));
        rowMap.put(C_GRANT_AWARD_STATUS, "Active");
        rowMap.put(C_GRANT_LOCAL_KEY, C_GRANT_LOCAL_KEY + Integer.toString(1));
        rowMap.put(C_GRANT_PROJECT_NAME, C_GRANT_PROJECT_NAME + Integer.toString(1) + "MOOO");
        rowMap.put(C_GRANT_AWARD_DATE, "01/01/2000");
        rowMap.put(C_GRANT_START_DATE, "01/01/2001");
        rowMap.put(C_GRANT_END_DATE, "01/01/2002");

        rowMap.put(C_DIRECT_FUNDER_LOCAL_KEY, C_DIRECT_FUNDER_LOCAL_KEY + Integer.toString(1));
        rowMap.put(C_DIRECT_FUNDER_NAME, C_DIRECT_FUNDER_NAME + Integer.toString(1) + "MOOOOO");
        rowMap.put(C_PRIMARY_FUNDER_LOCAL_KEY, C_PRIMARY_FUNDER_LOCAL_KEY + Integer.toString(1));
        rowMap.put(C_PRIMARY_FUNDER_NAME, C_PRIMARY_FUNDER_NAME + Integer.toString(1));

        rowMap.put(C_USER_FIRST_NAME, C_USER_FIRST_NAME + Integer.toString(1));
        rowMap.put(C_USER_MIDDLE_NAME, C_USER_MIDDLE_NAME + Integer.toString(1) + "MOOOO");
        rowMap.put(C_USER_LAST_NAME, C_USER_LAST_NAME + Integer.toString(1));
        rowMap.put(C_USER_EMAIL, C_USER_EMAIL + Integer.toString(1));
        rowMap.put(C_USER_INSTITUTIONAL_ID, C_USER_INSTITUTIONAL_ID + Integer.toString(1));
        rowMap.put(C_USER_LOCAL_KEY, C_USER_LOCAL_KEY + Integer.toString(1));

        rowMap.put(C_UPDATE_TIMESTAMP, "2018-01-01 0" + Integer.toString(1) + ":00:00.0");
        rowMap.put(C_ABBREVIATED_ROLE, ("C"));

        resultSet.clear();
        resultSet.add(rowMap);

        fedoraUpdater.updateFedora(resultSet, "grant");
        Assert.assertEquals(0, statistics.getFundersCreated());
        Assert.assertEquals(1, statistics.getFundersUpdated());
        Assert.assertEquals(0, statistics.getGrantsCreated());
        Assert.assertEquals(1, statistics.getGrantsUpdated());
        Assert.assertEquals(0, statistics.getUsersCreated());
        Assert.assertEquals(1, statistics.getUsersUpdated());


    }
}
