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
import org.dataconservancy.pass.grant.data.DateTimeUtil;
import org.dataconservancy.pass.grant.data.DirectoryServiceUtil;
import org.dataconservancy.pass.grant.data.PassUpdateStatistics;
import org.dataconservancy.pass.grant.data.PassUpdater;
import org.dataconservancy.pass.grant.data.PassEntityUtil;
import org.dataconservancy.pass.model.Funder;
import org.dataconservancy.pass.model.Grant;

import org.dataconservancy.pass.model.User;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static java.lang.Thread.sleep;
import static org.dataconservancy.pass.grant.data.CoeusFieldNames.*;
import static org.dataconservancy.pass.grant.data.PassUpdater.institutionalSuffix;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * An integration test class for the PassUpdater.
 */
@RunWith(MockitoJUnitRunner.class)
public class PassUpdaterIT {

    private Set<Map<String,String>> resultSet = new HashSet<>();

    @Mock
    private DirectoryServiceUtil directoryServiceUtilMock;


    @Before
    public void setup() throws IOException {

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
            rowMap.put(C_USER_EMPLOYEE_ID, C_USER_EMPLOYEE_ID + Integer.toString(i));

            rowMap.put(C_UPDATE_TIMESTAMP, "2018-01-01 0" + Integer.toString(i) + ":00:00.0");
            rowMap.put(C_ABBREVIATED_ROLE, (i%2==0?"P":"C"));

            resultSet.add(rowMap);
        }

        when(directoryServiceUtilMock.getHopkinsIdForEmployeeId(anyString())).thenAnswer(i -> i.getArguments()[0].toString().substring(6)); //needs to be unique
    }

    /**
     * The behavior of PassUpdate's updatePass() method is to compare the data coming in on the ResultSet with
     * the existing data in Pass, and create objects if Pass does not yet have them, and update them if they exist in Pass but
     * there are differences in the fields for which COEUS is the authoritative source, or COEUS has a clue about other fields which are null
     * on the PASS object.
     *
     * @throws InterruptedException - the exception
     */
    @Test
    public void updateGrantsIT() throws InterruptedException, IOException {

        PassClient passClient = PassClientFactory.getPassClient();
        PassUpdater passUpdater = new PassUpdater(passClient, directoryServiceUtilMock);
        passUpdater.updatePass(resultSet, "grant");
        PassUpdateStatistics statistics = passUpdater.getStatistics();

        Assert.assertEquals(5, statistics.getPisAdded());
        Assert.assertEquals(5, statistics.getCoPisAdded());
        Assert.assertEquals(20, statistics.getFundersCreated());
        Assert.assertEquals(0, statistics.getFundersUpdated());
        Assert.assertEquals(10, statistics.getGrantsCreated());
        Assert.assertEquals(0, statistics.getGrantsUpdated());
        Assert.assertEquals("2018-01-01 09:00:00.0", statistics.getLatestUpdateString());
        Assert.assertEquals(10, statistics.getUsersCreated());
        Assert.assertEquals(0, statistics.getUsersUpdated());

        Assert.assertEquals(10, passUpdater.getGrantUriMap().size());

        for (URI grantUri : passUpdater.getGrantUriMap().keySet()) {
            Grant grant = passUpdater.getGrantUriMap().get(grantUri);
            Grant passGrant = passUpdater.getPassClient().readResource(grantUri, Grant.class);
            Assert.assertTrue(PassEntityUtil.coeusGrantsEqual(grant, passGrant));
        }

        sleep(20000);
        //try depositing the exact same resultSet. nothing should happen in Pass
        passUpdater.updatePass(resultSet, "grant");

        Assert.assertEquals(0, statistics.getFundersCreated());
        Assert.assertEquals(0, statistics.getFundersUpdated());
        Assert.assertEquals(0, statistics.getGrantsCreated());
        Assert.assertEquals(0, statistics.getGrantsUpdated());
        Assert.assertEquals(0, statistics.getUsersCreated());
        Assert.assertEquals(0, statistics.getUsersUpdated());

        //now let's monkey with a few things; we expect to update the changed objects
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
        rowMap.put(C_USER_EMPLOYEE_ID, C_USER_EMPLOYEE_ID + Integer.toString(1));

        rowMap.put(C_UPDATE_TIMESTAMP, "2018-01-01 0" + Integer.toString(1) + ":00:00.0");
        rowMap.put(C_ABBREVIATED_ROLE, ("C"));

        resultSet.clear();
        resultSet.add(rowMap);

        passUpdater.updatePass(resultSet, "grant");
        Assert.assertEquals(0, statistics.getFundersCreated());
        Assert.assertEquals(1, statistics.getFundersUpdated());
        Assert.assertEquals(0, statistics.getGrantsCreated());
        Assert.assertEquals(1, statistics.getGrantsUpdated());
        Assert.assertEquals(0, statistics.getUsersCreated());
        Assert.assertEquals(1, statistics.getUsersUpdated());

        sleep(20000);

        for(int i = 0; i<10; i++) {
            Grant grant = new Grant();
            grant.setAwardNumber(C_GRANT_AWARD_NUMBER + Integer.toString(i));
            grant.setAwardStatus(Grant.AwardStatus.ACTIVE);
            grant.setLocalKey(C_GRANT_LOCAL_KEY + Integer.toString(i) + institutionalSuffix);
            grant.setProjectName(C_GRANT_PROJECT_NAME + Integer.toString(i));
            grant.setAwardDate(DateTimeUtil.createJodaDateTime("01/01/2000"));
            grant.setStartDate(DateTimeUtil.createJodaDateTime("01/01/2001"));
            grant.setEndDate(DateTimeUtil.createJodaDateTime("01/01/2002"));

            URI passGrantUri = passClient.findByAttribute(Grant.class, "localKey", grant.getLocalKey());
            Grant passGrant = passClient.readResource(passGrantUri, Grant.class);

            Assert.assertEquals(grant.getAwardNumber(), passGrant.getAwardNumber());
            Assert.assertEquals(grant.getAwardStatus(), passGrant.getAwardStatus());
            Assert.assertEquals(grant.getLocalKey(), passGrant.getLocalKey());
            if(i==1) {
                Assert.assertEquals(grant.getProjectName()+"MOOO", passGrant.getProjectName());
            } else {
                Assert.assertEquals(grant.getProjectName(), passGrant.getProjectName());
            }
            Assert.assertEquals(grant.getAwardDate(),passGrant.getAwardDate());
            Assert.assertEquals(grant.getStartDate(), passGrant.getStartDate());
            Assert.assertEquals(grant.getEndDate(),passGrant.getEndDate());

            //let's check funder stuff
            Funder directFunder = new Funder();
            directFunder.setLocalKey(C_DIRECT_FUNDER_LOCAL_KEY + Integer.toString(i)+institutionalSuffix);
            directFunder.setName(C_DIRECT_FUNDER_NAME + Integer.toString(i));

            URI directFunderUri = passClient.findByAttribute(Funder.class, "localKey", directFunder.getLocalKey());
            Funder passDirectFunder = passClient.readResource(directFunderUri, Funder.class);
            if(i==1) {
                Assert.assertEquals(directFunder.getName()+ "MOOOOO", passDirectFunder.getName());
                Assert.assertEquals(directFunder.getLocalKey(), passDirectFunder.getLocalKey());
                Assert.assertEquals(passDirectFunder.getId(), passGrant.getDirectFunder());
            } else {
                Assert.assertEquals(directFunder.getName(),passDirectFunder.getName());
            }

            Funder primaryFunder = new Funder();
            primaryFunder.setLocalKey( C_PRIMARY_FUNDER_LOCAL_KEY + Integer.toString(i)+institutionalSuffix);
            primaryFunder.setName( C_PRIMARY_FUNDER_NAME + Integer.toString(i));

            URI primaryFunderUri = passClient.findByAttribute(Funder.class, "localKey", primaryFunder.getLocalKey());
            Funder passPrimaryFunder = passClient.readResource(primaryFunderUri, Funder.class);
            Assert.assertEquals(primaryFunder.getName(), passPrimaryFunder.getName());
            Assert.assertEquals(passPrimaryFunder.getId(), passGrant.getPrimaryFunder());
            Assert.assertEquals(primaryFunder.getLocalKey(), passPrimaryFunder.getLocalKey());

            User user = new User();

            //institutionalId and localKey were set to different values by the grant loader
            String newInstitutionalId = directoryServiceUtilMock.getHopkinsIdForEmployeeId(C_USER_EMPLOYEE_ID + Integer.toString(i)) + institutionalSuffix;
            user.setLocalKey(C_USER_EMPLOYEE_ID + Integer.toString(i) + institutionalSuffix);
            user.setFirstName(C_USER_FIRST_NAME + Integer.toString(i));
            user.setMiddleName(C_USER_MIDDLE_NAME + Integer.toString(i));
            user.setLastName(C_USER_LAST_NAME + Integer.toString(i));
            user.setEmail(C_USER_EMAIL + Integer.toString(i));
            user.setInstitutionalId(newInstitutionalId);

            URI userUri = passClient.findByAttribute(User.class, "localKey", user.getLocalKey());
            User passUser = passClient.readResource(userUri, User.class);
            Assert.assertEquals(user.getFirstName(), passUser.getFirstName());
            if (i==1) {
                Assert.assertEquals(user.getMiddleName() + "MOOOO", passUser.getMiddleName());
            } else {
                Assert.assertEquals(user.getMiddleName(), passUser.getMiddleName());
            }
            Assert.assertEquals(user.getLastName(), passUser.getLastName());
            Assert.assertEquals(user.getEmail(), passUser.getEmail());
            Assert.assertEquals(user.getInstitutionalId(), passUser.getInstitutionalId());

            if (i%2 == 0) {
                assertNotNull(passGrant.getPi());
                Assert.assertEquals(0, passGrant.getCoPis().size());
            } else {
                assertNull(passGrant.getPi());
                Assert.assertEquals(1, passGrant.getCoPis().size());
            }

        }
    }

    @Test
    public void updateExistingGrantsIT() throws IOException, InterruptedException {
        Grant grant10 = new Grant();
        grant10.setLocalKey(C_GRANT_LOCAL_KEY + 10);
        grant10.setAwardNumber(C_GRANT_AWARD_NUMBER + 10);
        grant10.setAwardStatus(Grant.AwardStatus.ACTIVE);
        grant10.setLocalKey(C_GRANT_LOCAL_KEY + 10);
        grant10.setProjectName(C_GRANT_PROJECT_NAME + 10);
        grant10.setAwardDate(DateTimeUtil.createJodaDateTime("01/01/2000"));
        grant10.setStartDate(DateTimeUtil.createJodaDateTime("01/01/2001"));
        grant10.setEndDate(DateTimeUtil.createJodaDateTime("01/01/2002"));

        PassClient passClient = PassClientFactory.getPassClient();
        PassUpdater passUpdater = new PassUpdater(passClient, directoryServiceUtilMock);

        URI passGrantURI = passUpdater.getPassClient().createResource(grant10);

        Grant passGrant = passClient.readResource(passGrantURI, Grant.class);
        assertEquals(grant10.getLocalKey(), passGrant.getLocalKey());

        sleep(20000);

        Set<Map<String,String>> grantResultSet = new HashSet<>();

        for(int i = 10; i<12; i++) {
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
            rowMap.put(C_USER_EMPLOYEE_ID, C_USER_EMPLOYEE_ID + Integer.toString(i));

            rowMap.put(C_UPDATE_TIMESTAMP, "2018-01-01 0" + Integer.toString(i) + ":00:00.0");
            rowMap.put(C_ABBREVIATED_ROLE, (i % 2 == 0 ? "P" : "C"));

            grantResultSet.add(rowMap);
        }

        passUpdater.updatePass(grantResultSet, "existing-grant");
        PassUpdateStatistics statistics = passUpdater.getStatistics();

        //now update from the set of two grants - the second one is not in PASS, and should not be created
        //the first (user10) should  be updated
        assertEquals(0, statistics.getGrantsCreated());
        assertEquals(1, statistics.getGrantsUpdated());

        assertNotNull(passGrantURI);
        Grant updatedGrant = passUpdater.getPassClient().readResource(passGrantURI, Grant.class);
        assertEquals(passGrant.getLocalKey() + institutionalSuffix, updatedGrant.getLocalKey());

    }

    @Test
    public void updateUsersIT() throws InterruptedException, IOException {

        User user10 = new User();
        user10.setLocalKey(C_USER_EMPLOYEE_ID + 10 + institutionalSuffix);
        user10.setFirstName(C_USER_FIRST_NAME + 10);
        user10.setMiddleName(C_USER_MIDDLE_NAME + 10);
        user10.setLastName(C_USER_LAST_NAME + 10);

        PassClient passClient = PassClientFactory.getPassClient();
        PassUpdater passUpdater = new PassUpdater(passClient, directoryServiceUtilMock);

        URI passUserURI = passUpdater.getPassClient().createResource(user10);

        User passUser = passClient.readResource(passUserURI, User.class);
        assertNull(passUser.getDisplayName());
        assertNull(passUser.getInstitutionalId());
        assertNull(passUser.getEmail());

        sleep(20000);

        Set<Map<String,String>> userResultSet = new HashSet<>();

        for(int i=10; i<12; i++) {
            Map<String, String> rowMap = new HashMap<>();

            rowMap.put(C_USER_FIRST_NAME, C_USER_FIRST_NAME + Integer.toString(i));
            rowMap.put(C_USER_MIDDLE_NAME, C_USER_MIDDLE_NAME + Integer.toString(i));
            rowMap.put(C_USER_LAST_NAME, C_USER_LAST_NAME + Integer.toString(i));
            rowMap.put(C_USER_EMAIL, C_USER_EMAIL + Integer.toString(i));
            rowMap.put(C_USER_INSTITUTIONAL_ID, C_USER_INSTITUTIONAL_ID + Integer.toString(i));
            rowMap.put(C_USER_EMPLOYEE_ID, C_USER_EMPLOYEE_ID + Integer.toString(i));
            rowMap.put(C_UPDATE_TIMESTAMP, "2018-01-01 0" + Integer.toString(1) + ":00:00.0");
            userResultSet.add(rowMap);
        }


        passUpdater.updatePass(userResultSet, "user");
        PassUpdateStatistics statistics = passUpdater.getStatistics();

        //now update from the set of two users - the second one is not in PASS, and should be created
        //the first (user10) should be updated, with new fields added
        assertEquals(1, statistics.getUsersCreated());
        assertEquals(1, statistics.getUsersUpdated());

        assertNotNull(passUserURI);
        User updatedUser = passUpdater.getPassClient().readResource(passUserURI, User.class);

        assertNotNull(updatedUser.getInstitutionalId());
        assertNotNull(updatedUser.getEmail());
        assertNotNull(updatedUser.getDisplayName());
        assertEquals(directoryServiceUtilMock.getHopkinsIdForEmployeeId(C_USER_EMPLOYEE_ID + 10) + institutionalSuffix, updatedUser.getInstitutionalId());
        assertEquals(user10.getLocalKey(), updatedUser.getLocalKey());
        assertEquals(C_USER_EMAIL + 10, updatedUser.getEmail());
    }


    @Test
    public void updateExistingUsersIT() throws InterruptedException, IOException {

        User user12 = new User();
        user12.setLocalKey(C_USER_EMPLOYEE_ID + 12);
        user12.setFirstName(C_USER_FIRST_NAME + 12);
        user12.setMiddleName(C_USER_MIDDLE_NAME + 12);
        user12.setLastName(C_USER_LAST_NAME + 12);

        PassClient passClient = PassClientFactory.getPassClient();
        PassUpdater passUpdater = new PassUpdater(passClient, directoryServiceUtilMock);

        URI passUserURI = passUpdater.getPassClient().createResource(user12);

        User passUser = passClient.readResource(passUserURI, User.class);
        assertNull(passUser.getDisplayName());
        assertNull(passUser.getInstitutionalId());
        assertNull(passUser.getEmail());

        sleep(20000);

        Set<Map<String,String>> userResultSet = new HashSet<>();

        for(int i=12; i<14; i++) {
            Map<String, String> rowMap = new HashMap<>();

            rowMap.put(C_USER_FIRST_NAME, C_USER_FIRST_NAME + Integer.toString(i));
            rowMap.put(C_USER_MIDDLE_NAME, C_USER_MIDDLE_NAME + Integer.toString(i));
            rowMap.put(C_USER_LAST_NAME, C_USER_LAST_NAME + Integer.toString(i));
            rowMap.put(C_USER_EMAIL, C_USER_EMAIL + Integer.toString(i));
            rowMap.put(C_USER_INSTITUTIONAL_ID, C_USER_INSTITUTIONAL_ID + Integer.toString(i));
            rowMap.put(C_USER_EMPLOYEE_ID, C_USER_EMPLOYEE_ID + Integer.toString(i));
            rowMap.put(C_UPDATE_TIMESTAMP, "2018-01-01 0" + Integer.toString(1) + ":00:00.0");
            userResultSet.add(rowMap);
        }


        passUpdater.updatePass(userResultSet, "existing-user");
        PassUpdateStatistics statistics = passUpdater.getStatistics();

        //now update from the set of two users - the second one is not in PASS, and not be created
        //the first (user10) should be updated, with new fields added
        Assert.assertEquals(0, statistics.getUsersCreated());
        Assert.assertEquals(1, statistics.getUsersUpdated());

        assertNotNull(passUserURI);
        User updatedUser = passUpdater.getPassClient().readResource(passUserURI, User.class);

        assertNotNull(updatedUser.getInstitutionalId());
        assertNotNull(updatedUser.getEmail());
        assertNotNull(updatedUser.getDisplayName());

        assertEquals(directoryServiceUtilMock.getHopkinsIdForEmployeeId(user12.getLocalKey()) + institutionalSuffix, updatedUser.getInstitutionalId());
        assertEquals(user12.getLocalKey()+ institutionalSuffix, updatedUser.getLocalKey());
        assertEquals(C_USER_EMAIL + 12, updatedUser.getEmail());
    }
}
