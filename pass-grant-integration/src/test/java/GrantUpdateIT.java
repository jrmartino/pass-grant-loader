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

import org.dataconservancy.pass.client.fedora.FedoraPassClient;
import org.dataconservancy.pass.grant.data.FedoraUpdateStatistics;
import org.dataconservancy.pass.grant.data.GrantUpdater;
import org.dataconservancy.pass.grant.data.PassEntityUtil;
import org.dataconservancy.pass.model.Grant;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
            rowMap.put(C_GRANT_LOCAL_AWARD_ID, C_GRANT_LOCAL_AWARD_ID + Integer.toString(i));
            rowMap.put(C_GRANT_PROJECT_NAME, C_GRANT_PROJECT_NAME + Integer.toString(i));
            rowMap.put(C_GRANT_AWARD_DATE, "01/01/2000");
            rowMap.put(C_GRANT_START_DATE, "01/01/2001");
            rowMap.put(C_GRANT_END_DATE, "01/01/2002");

            rowMap.put(C_DIRECT_FUNDER_LOCAL_ID, C_DIRECT_FUNDER_LOCAL_ID + Integer.toString(i));
            rowMap.put(C_DIRECT_FUNDER_NAME, C_DIRECT_FUNDER_NAME + Integer.toString(i));
            rowMap.put(C_PRIMARY_FUNDER_LOCAL_ID, C_PRIMARY_FUNDER_LOCAL_ID + Integer.toString(i));
            rowMap.put(C_PRIMARY_FUNDER_NAME, C_PRIMARY_FUNDER_NAME + Integer.toString(i));

            rowMap.put(C_PERSON_FIRST_NAME, C_PERSON_FIRST_NAME + Integer.toString(i));
            rowMap.put(C_PERSON_MIDDLE_NAME, C_PERSON_MIDDLE_NAME + Integer.toString(i));
            rowMap.put(C_PERSON_LAST_NAME, C_PERSON_LAST_NAME + Integer.toString(i));
            rowMap.put(C_PERSON_EMAIL, C_PERSON_EMAIL + Integer.toString(i));
            rowMap.put(C_PERSON_INSTITUTIONAL_ID, C_PERSON_INSTITUTIONAL_ID + Integer.toString(i));

            rowMap.put(C_UPDATE_TIMESTAMP, "2018-01-01 0" + Integer.toString(i) + ":00:00.0");
            rowMap.put(C_ABBREVIATED_ROLE, (i%2==0?"P":"C"));

            resultSet.add(rowMap);
        }
    }

    @Test
    public void depositGrantsIT() {

        GrantUpdater grantUpdater = new GrantUpdater(new FedoraPassClient());
        grantUpdater.updateGrants(resultSet);
        FedoraUpdateStatistics statistics = grantUpdater.getStatistics();

        Assert.assertEquals(statistics.getPisAdded(),5);
        Assert.assertEquals(statistics.getCoPisAdded(), 5);
        Assert.assertEquals(statistics.getFundersCreated(), 20);
        Assert.assertEquals(statistics.getFundersUpdated(),0);
        Assert.assertEquals(statistics.getGrantsCreated(),10 );
        Assert.assertEquals(statistics.getGrantsUpdated(), 0);
        Assert.assertEquals(statistics.getLatestUpdateString(),"2018-01-01 09:00:00.0");
        Assert.assertEquals(statistics.getPersonsCreated(),10);
        Assert.assertEquals(statistics.getPersonsUpdated(),0);

        for (URI grantUri : grantUpdater.getGrantUriMap().keySet()) {
            Grant grant = grantUpdater.getGrantUriMap().get(grantUri);
            Grant fedoraGrant = grantUpdater.getFedoraClient().readResource(grantUri, Grant.class);
            Assert.assertTrue(PassEntityUtil.coeusGrantsEqual(grant, fedoraGrant));
        }


    }
}
