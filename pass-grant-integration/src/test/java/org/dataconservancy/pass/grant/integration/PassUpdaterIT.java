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
package org.dataconservancy.pass.grant.integration;

import org.dataconservancy.pass.client.PassClient;
import org.dataconservancy.pass.client.PassClientFactory;
import org.dataconservancy.pass.grant.data.DateTimeUtil;
import org.dataconservancy.pass.grant.data.PassUpdateStatistics;
import org.dataconservancy.pass.grant.data.PassUpdater;
import org.dataconservancy.pass.grant.data.CoeusPassEntityUtil;
import org.dataconservancy.pass.model.Funder;
import org.dataconservancy.pass.model.Grant;

import org.dataconservancy.pass.model.User;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static java.lang.Thread.sleep;
import static org.dataconservancy.pass.grant.data.CoeusFieldNames.*;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * An integration test class for the PassUpdater.
 */
@RunWith(MockitoJUnitRunner.class)
public class PassUpdaterIT {

    private List<Map<String, String>> resultSet = new ArrayList<>();
    private static String funderIdPrefix = "johnshopkins.edu:funder:";
    private static String grantIdPrefix = "johnshopkins.edu:grant:";
    private static String employeeidPrefix = "johnshopkins.edu:employeeid:";
    private static String hopkinsidPrefix = "johnshopkins.edu:hopkinsid:";
    private static String jhedPrefix = "johnshopkins.edu:jhed:";
    private CoeusPassEntityUtil coeusPassEntityUtil = new CoeusPassEntityUtil();

    @Rule
    public TemporaryFolder folder= new TemporaryFolder();

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
            rowMap.put(C_USER_EMPLOYEE_ID, C_USER_EMPLOYEE_ID + Integer.toString(i));
            rowMap.put(C_USER_HOPKINS_ID, C_USER_HOPKINS_ID + Integer.toString(i));

            rowMap.put(C_UPDATE_TIMESTAMP, "2018-01-01 0" + Integer.toString(i) + ":00:00.0");
            rowMap.put(C_ABBREVIATED_ROLE, (i % 2 == 0 ? "P" : "C"));

            resultSet.add(rowMap);
        }

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
    public void updateGrantsIT() throws InterruptedException {

        PassClient passClient = PassClientFactory.getPassClient();
        PassUpdater passUpdater = new PassUpdater(passClient);
        passUpdater.updatePass(resultSet, "grant");
        PassUpdateStatistics statistics = passUpdater.getStatistics();

        assertEquals(5, statistics.getPisAdded());
        assertEquals(5, statistics.getCoPisAdded());
        assertEquals(20, statistics.getFundersCreated());
        assertEquals(0, statistics.getFundersUpdated());
        assertEquals(10, statistics.getGrantsCreated());
        assertEquals(0, statistics.getGrantsUpdated());
        assertEquals("2018-01-01 09:00:00.0", statistics.getLatestUpdateString());
        assertEquals(10, statistics.getUsersCreated());
        assertEquals(0, statistics.getUsersUpdated());

        assertEquals(10, passUpdater.getGrantUriMap().size());

        for (URI grantUri : passUpdater.getGrantUriMap().keySet()) {
            Grant grant = passUpdater.getGrantUriMap().get(grantUri);
            Grant passGrant = passUpdater.getPassClient().readResource(grantUri, Grant.class);
            assertNull(coeusPassEntityUtil.update(grant, passGrant)); //this means grants are "coeus-equal"

        }

        sleep(20000);
        //try depositing the exact same resultSet. nothing should happen in Pass
        passUpdater.updatePass(resultSet, "grant");

        assertEquals(0, statistics.getFundersCreated());
        assertEquals(0, statistics.getFundersUpdated());
        assertEquals(0, statistics.getGrantsCreated());
        assertEquals(0, statistics.getGrantsUpdated());
        assertEquals(0, statistics.getUsersCreated());
        assertEquals(0, statistics.getUsersUpdated());

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
        rowMap.put(C_USER_HOPKINS_ID, C_USER_HOPKINS_ID + Integer.toString(1));

        rowMap.put(C_UPDATE_TIMESTAMP, "2018-01-01 0" + Integer.toString(1) + ":00:00.0");
        rowMap.put(C_ABBREVIATED_ROLE, ("C"));

        resultSet.clear();
        resultSet.add(rowMap);

        passUpdater.updatePass(resultSet, "grant");
        assertEquals(0, statistics.getFundersCreated());
        assertEquals(1, statistics.getFundersUpdated());
        assertEquals(0, statistics.getGrantsCreated());
        assertEquals(1, statistics.getGrantsUpdated());
        assertEquals(0, statistics.getUsersCreated());
        assertEquals(1, statistics.getUsersUpdated());

        sleep(20000);

        for (int i = 0; i < 10; i++) {
            Grant grant = new Grant();
            grant.setAwardNumber(C_GRANT_AWARD_NUMBER + Integer.toString(i));
            grant.setAwardStatus(Grant.AwardStatus.ACTIVE);
            grant.setLocalKey(grantIdPrefix + C_GRANT_LOCAL_KEY + Integer.toString(i));
            grant.setProjectName(C_GRANT_PROJECT_NAME + Integer.toString(i));
            grant.setAwardDate(DateTimeUtil.createJodaDateTime("01/01/2000"));
            grant.setStartDate(DateTimeUtil.createJodaDateTime("01/01/2001"));
            grant.setEndDate(DateTimeUtil.createJodaDateTime("01/01/2002"));

            URI passGrantUri = passClient.findByAttribute(Grant.class, "localKey", grant.getLocalKey());
            Grant passGrant = passClient.readResource(passGrantUri, Grant.class);

            assertEquals(grant.getAwardNumber(), passGrant.getAwardNumber());
            assertEquals(grant.getAwardStatus(), passGrant.getAwardStatus());
            assertEquals(grant.getLocalKey(), passGrant.getLocalKey());
            if (i == 1) {
                assertEquals(grant.getProjectName() + "MOOO", passGrant.getProjectName());
            } else {
                assertEquals(grant.getProjectName(), passGrant.getProjectName());
            }
            assertEquals(grant.getAwardDate(), passGrant.getAwardDate());
            assertEquals(grant.getStartDate(), passGrant.getStartDate());
            assertEquals(grant.getEndDate(), passGrant.getEndDate());

            //let's check funder stuff
            Funder directFunder = new Funder();
            directFunder.setLocalKey(funderIdPrefix + C_DIRECT_FUNDER_LOCAL_KEY + Integer.toString(i));
            directFunder.setName(C_DIRECT_FUNDER_NAME + Integer.toString(i));

            URI directFunderUri = passClient.findByAttribute(Funder.class, "localKey", directFunder.getLocalKey());
            Funder passDirectFunder = passClient.readResource(directFunderUri, Funder.class);
            if (i == 1) {
                assertEquals(directFunder.getName() + "MOOOOO", passDirectFunder.getName());
                assertEquals(directFunder.getLocalKey(), passDirectFunder.getLocalKey());
                assertEquals(passDirectFunder.getId(), passGrant.getDirectFunder());
            } else {
                assertEquals(directFunder.getName(), passDirectFunder.getName());
            }

            Funder primaryFunder = new Funder();
            primaryFunder.setLocalKey(funderIdPrefix + C_PRIMARY_FUNDER_LOCAL_KEY + Integer.toString(i));
            primaryFunder.setName(C_PRIMARY_FUNDER_NAME + Integer.toString(i));

            URI primaryFunderUri = passClient.findByAttribute(Funder.class, "localKey", primaryFunder.getLocalKey());
            Funder passPrimaryFunder = passClient.readResource(primaryFunderUri, Funder.class);
            assertEquals(primaryFunder.getName(), passPrimaryFunder.getName());
            assertEquals(passPrimaryFunder.getId(), passGrant.getPrimaryFunder());
            assertEquals(primaryFunder.getLocalKey(), passPrimaryFunder.getLocalKey());

            User user = new User();

            //institutionalId and localKey were localized by the grant loader
            user.getLocatorIds().add(employeeidPrefix + C_USER_EMPLOYEE_ID + Integer.toString(i));
            user.getLocatorIds().add(hopkinsidPrefix + C_USER_HOPKINS_ID + Integer.toString(i));
            user.getLocatorIds().add(jhedPrefix + C_USER_INSTITUTIONAL_ID.toLowerCase() + Integer.toString(i));
            user.setFirstName(C_USER_FIRST_NAME + Integer.toString(i));
            user.setMiddleName(C_USER_MIDDLE_NAME + Integer.toString(i));
            user.setLastName(C_USER_LAST_NAME + Integer.toString(i));
            user.setEmail(C_USER_EMAIL + Integer.toString(i));

            URI userUri = null;
            ListIterator idIterator = user.getLocatorIds().listIterator();

            while (userUri == null && idIterator.hasNext()) {
                String id = String.valueOf(idIterator.next());
                if (id != null) {
                    userUri = passClient.findByAttribute(User.class, "locatorIds", id);
                }
            }

            User passUser = passClient.readResource(userUri, User.class);
            assertEquals(user.getFirstName(), passUser.getFirstName());
            if (i == 1) {
                assertEquals(user.getMiddleName() + "MOOOO", passUser.getMiddleName());
            } else {
                assertEquals(user.getMiddleName(), passUser.getMiddleName());
            }
            assertEquals(user.getLastName(), passUser.getLastName());
            assertEquals(user.getEmail(), passUser.getEmail());
            assertTrue(user.getLocatorIds().containsAll(passUser.getLocatorIds()));
            assertTrue(passUser.getLocatorIds().containsAll(user.getLocatorIds()));
            assertEquals(passUser.getLocatorIds().size(), user.getLocatorIds().size());

            if (i % 2 == 0) {
                assertNotNull(passGrant.getPi());
                assertEquals(0, passGrant.getCoPis().size());
            } else {
                assertNull(passGrant.getPi());
                assertEquals(1, passGrant.getCoPis().size());
            }

        }
    }


    @Test
    public void updateUsersIT() throws InterruptedException {

        User user10 = new User();
        user10.getLocatorIds().add(employeeidPrefix + C_USER_EMPLOYEE_ID + 10);
        user10.setFirstName(C_USER_FIRST_NAME + 10);
        user10.setMiddleName(C_USER_MIDDLE_NAME + 10);
        user10.setLastName(C_USER_LAST_NAME + 10);

        PassClient passClient = PassClientFactory.getPassClient();
        PassUpdater passUpdater = new PassUpdater(passClient);

        URI passUserURI = passUpdater.getPassClient().createResource(user10);

        User passUser = passClient.readResource(passUserURI, User.class);
        assertNull(passUser.getDisplayName());
        assertEquals(1, passUser.getLocatorIds().size());
        assertNull(passUser.getEmail());

        sleep(20000);

        List<Map<String, String>> userResultSet = new ArrayList<>();

        for (int i = 10; i < 12; i++) {
            Map<String, String> rowMap = new HashMap<>();

            rowMap.put(C_USER_FIRST_NAME, C_USER_FIRST_NAME + Integer.toString(i));
            rowMap.put(C_USER_MIDDLE_NAME, C_USER_MIDDLE_NAME + Integer.toString(i));
            rowMap.put(C_USER_LAST_NAME, C_USER_LAST_NAME + Integer.toString(i));
            rowMap.put(C_USER_EMAIL, C_USER_EMAIL + Integer.toString(i));
            rowMap.put(C_USER_INSTITUTIONAL_ID, C_USER_INSTITUTIONAL_ID + Integer.toString(i));
            rowMap.put(C_USER_EMPLOYEE_ID, C_USER_EMPLOYEE_ID + Integer.toString(i));
            rowMap.put(C_USER_HOPKINS_ID, C_USER_HOPKINS_ID + Integer.toString(i));
            rowMap.put(C_UPDATE_TIMESTAMP, "2018-01-01 0" + Integer.toString(1) + ":00:00.0");
            userResultSet.add(rowMap);
        }


        passUpdater.updatePass(userResultSet, "user");
        PassUpdateStatistics statistics = passUpdater.getStatistics();

        //now update from the set of two users - the second one is not in PASS, but is not created
        //the first (user10) should be updated, with new fields added
        assertEquals(0, statistics.getUsersCreated());
        assertEquals(1, statistics.getUsersUpdated());

        assertNotNull(passUserURI);
        User updatedUser = passUpdater.getPassClient().readResource(passUserURI, User.class);


        assertNotNull(updatedUser.getEmail());
        assertNotNull(updatedUser.getDisplayName());
        assertNotNull(updatedUser.getLocatorIds());
        assertTrue(updatedUser.getLocatorIds().contains(employeeidPrefix + C_USER_EMPLOYEE_ID + Integer.toString(10)));
        assertTrue(updatedUser.getLocatorIds().contains(jhedPrefix + C_USER_INSTITUTIONAL_ID.toLowerCase() + Integer.toString(10)));

        assertEquals(C_USER_EMAIL + 10, updatedUser.getEmail());
    }

    @Test
    public void testSerializeAndDeserialize() throws IOException {
        File serialized= folder.newFile("serializedData");

        try (FileOutputStream fos = new FileOutputStream(serialized);
              ObjectOutputStream out  = new ObjectOutputStream(fos)
            ){
            out.writeObject(resultSet);
        } catch (IOException e) {
            e.printStackTrace();
        }

        List input = null;
        try (FileInputStream fis = new FileInputStream(serialized);
            ObjectInputStream in = new ObjectInputStream(fis)
            ){
            input = (List<Map<String,String>>)in.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }

        assertEquals(resultSet, input);
    }

}