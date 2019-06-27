package org.dataconservancy.pass.grant.integration;

import org.dataconservancy.pass.client.PassClient;
import org.dataconservancy.pass.client.PassClientFactory;
import org.dataconservancy.pass.grant.data.DateTimeUtil;
import org.dataconservancy.pass.grant.data.HarvardPilotPassEntityUtil;
import org.dataconservancy.pass.grant.data.HarvardPilotPassUpdater;
import org.dataconservancy.pass.grant.data.PassEntityUtil;
import org.dataconservancy.pass.grant.data.PassUpdateStatistics;
import org.dataconservancy.pass.grant.data.PassUpdater;
import org.dataconservancy.pass.model.Funder;
import org.dataconservancy.pass.model.Grant;
import org.dataconservancy.pass.model.Policy;
import org.dataconservancy.pass.model.User;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.HashMap;

import static java.lang.Thread.sleep;
import static org.dataconservancy.pass.grant.data.CoeusFieldNames.*;
import static org.junit.Assert.*;

public class HarvardPilotPassUpdaterIT {

    private static final String DOMAIN = "harvard.edu";

    private List<Map<String, String>> resultSet = new ArrayList<>();
    private static String employeeidPrefix = DOMAIN + ":employeeid:";
    private PassEntityUtil passEntityUtil = new HarvardPilotPassEntityUtil();
    private Map<String, URI> funderPolicyUriMap = new HashMap<>();

    private String directFunderPolicyUriString1;
    private String primaryFunderPolicyUriString1;


    private PassClient passClient = PassClientFactory.getPassClient();
 

    @Rule
    public TemporaryFolder folder= new TemporaryFolder();

    @Before
    public void setup() {

        for (int i = 0; i < 10; i++) {

            String prefix = System.getProperty("pass.fedora.baseurl");
            if (!prefix.endsWith("/")) {
                prefix = prefix + "/";
            }

            Policy policy = new Policy();
            policy.setTitle("Primary Policy" + i);
            policy.setDescription("MOO");
            URI policyURI = passClient.createResource(policy);
            String primaryPolicyUriString = policyURI.toString().substring(prefix.length());
            funderPolicyUriMap.put("PrimaryFunderPolicy" + i, policyURI);

            policy = new Policy();
            policy.setTitle("Direct Policy" + i);
            policy.setDescription("MOO");
            policyURI = passClient.createResource(policy);
            String directPolicyUriString = policyURI.toString().substring(prefix.length());
            funderPolicyUriMap.put("DirectFunderPolicy" + i, policyURI);

            if (i == 1) {
                directFunderPolicyUriString1 = directPolicyUriString;
                primaryFunderPolicyUriString1 = primaryPolicyUriString;
            }

            
            
            
            Map<String, String> rowMap = new HashMap<>();

            rowMap.put(C_GRANT_AWARD_NUMBER, C_GRANT_AWARD_NUMBER + i);
            //rowMap.put(C_GRANT_AWARD_STATUS, "Active");
            rowMap.put(C_GRANT_LOCAL_KEY, C_GRANT_LOCAL_KEY + i);
            rowMap.put(C_GRANT_PROJECT_NAME, C_GRANT_PROJECT_NAME + i);
            //rowMap.put(C_GRANT_AWARD_DATE, "01/01/2000");
            rowMap.put(C_GRANT_START_DATE, "01/01/2001");
            rowMap.put(C_GRANT_END_DATE, "01/01/2002");

            rowMap.put(C_DIRECT_FUNDER_LOCAL_KEY, C_DIRECT_FUNDER_LOCAL_KEY + i);
            rowMap.put(C_DIRECT_FUNDER_NAME, C_DIRECT_FUNDER_NAME + i);
            rowMap.put(C_PRIMARY_FUNDER_LOCAL_KEY, C_PRIMARY_FUNDER_LOCAL_KEY + i);
            rowMap.put(C_PRIMARY_FUNDER_NAME, C_PRIMARY_FUNDER_NAME + i);

            rowMap.put(C_USER_FIRST_NAME, C_USER_FIRST_NAME + i);
            //rowMap.put(C_USER_MIDDLE_NAME, C_USER_MIDDLE_NAME + i);
            rowMap.put(C_USER_LAST_NAME, C_USER_LAST_NAME + i);
            rowMap.put(C_USER_EMAIL, C_USER_EMAIL + i);
            //rowMap.put(C_USER_INSTITUTIONAL_ID, C_USER_INSTITUTIONAL_ID + i);
            rowMap.put(C_USER_EMPLOYEE_ID, C_USER_EMPLOYEE_ID + i);
           // rowMap.put(C_USER_HOPKINS_ID, C_USER_HOPKINS_ID + i);

            //rowMap.put(C_UPDATE_TIMESTAMP, "2018-01-01 0" + i + ":00:00.0");
            rowMap.put(C_ABBREVIATED_ROLE, (i % 2 == 0 ? "P" : "C"));
            rowMap.put(C_DIRECT_FUNDER_POLICY, directPolicyUriString);
            rowMap.put(C_PRIMARY_FUNDER_POLICY, primaryPolicyUriString);

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

        PassUpdater passUpdater = new HarvardPilotPassUpdater();
        passUpdater.updatePass(resultSet, "grant");
        PassUpdateStatistics statistics = passUpdater.getStatistics();

        assertEquals(5, statistics.getPisAdded());
        assertEquals(5, statistics.getCoPisAdded());
        assertEquals(20, statistics.getFundersCreated());
        assertEquals(0, statistics.getFundersUpdated());
        assertEquals(10, statistics.getGrantsCreated());
        assertEquals(0, statistics.getGrantsUpdated());
        //assertEquals("2018-01-01 09:00:00.0", statistics.getLatestUpdateString());
        assertEquals(10, statistics.getUsersCreated());
        assertEquals(0, statistics.getUsersUpdated());

        assertEquals(10, passUpdater.getGrantUriMap().size());

        for (URI grantUri : passUpdater.getGrantUriMap().keySet()) {
            Grant grant = passUpdater.getGrantUriMap().get(grantUri);
            Grant passGrant = passUpdater.getPassClient().readResource(grantUri, Grant.class);
            assertNull(passEntityUtil.update(grant, passGrant)); //this means grants are "harvard-equal"

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
        rowMap.put(C_GRANT_AWARD_NUMBER, C_GRANT_AWARD_NUMBER + 1);
        //rowMap.put(C_GRANT_AWARD_STATUS, "Active");
        rowMap.put(C_GRANT_LOCAL_KEY, C_GRANT_LOCAL_KEY + 1);
        rowMap.put(C_GRANT_PROJECT_NAME, C_GRANT_PROJECT_NAME + 1 + "MOOO");
        //rowMap.put(C_GRANT_AWARD_DATE, "01/01/2000");
        rowMap.put(C_GRANT_START_DATE, "01/01/2001");
        rowMap.put(C_GRANT_END_DATE, "01/01/2002");

        rowMap.put(C_DIRECT_FUNDER_LOCAL_KEY, C_DIRECT_FUNDER_LOCAL_KEY + 1);
        rowMap.put(C_DIRECT_FUNDER_NAME, C_DIRECT_FUNDER_NAME + 1 + "MOOOO");
        rowMap.put(C_PRIMARY_FUNDER_LOCAL_KEY, C_PRIMARY_FUNDER_LOCAL_KEY + 1);
        rowMap.put(C_PRIMARY_FUNDER_NAME, C_PRIMARY_FUNDER_NAME + 1);

        rowMap.put(C_USER_FIRST_NAME, C_USER_FIRST_NAME + 1 + "MOOOOO");
        rowMap.put(C_USER_LAST_NAME, C_USER_LAST_NAME + 1);
        rowMap.put(C_USER_EMAIL, C_USER_EMAIL + 1);
        //rowMap.put(C_USER_INSTITUTIONAL_ID, C_USER_INSTITUTIONAL_ID + 1);
        rowMap.put(C_USER_EMPLOYEE_ID, C_USER_EMPLOYEE_ID + 1);
        //rowMap.put(C_USER_HOPKINS_ID, C_USER_HOPKINS_ID + 1);

        //rowMap.put(C_UPDATE_TIMESTAMP, "2018-01-01 0" + 1 + ":00:00.0");
        rowMap.put(C_ABBREVIATED_ROLE, ("C"));

        rowMap.put(C_DIRECT_FUNDER_POLICY, directFunderPolicyUriString1);
        rowMap.put(C_PRIMARY_FUNDER_POLICY, primaryFunderPolicyUriString1);


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
            grant.setAwardNumber(C_GRANT_AWARD_NUMBER + i);
            //grant.setAwardStatus(Grant.AwardStatus.ACTIVE);
            String grantIdPrefix = DOMAIN + ":grant:";
            grant.setLocalKey(grantIdPrefix + C_GRANT_LOCAL_KEY + i);
            grant.setProjectName(C_GRANT_PROJECT_NAME + i);
            //grant.setAwardDate(DateTimeUtil.createJodaDateTime("01/01/2000"));
            grant.setStartDate(DateTimeUtil.createJodaDateTime("01/01/2001"));
            grant.setEndDate(DateTimeUtil.createJodaDateTime("01/01/2002"));

            URI passGrantUri = passClient.findByAttribute(Grant.class, "localKey", grant.getLocalKey());
            Grant passGrant = passClient.readResource(passGrantUri, Grant.class);

            assertEquals(grant.getAwardNumber(), passGrant.getAwardNumber());
            //assertEquals(grant.getAwardStatus(), passGrant.getAwardStatus());
            assertEquals(grant.getLocalKey(), passGrant.getLocalKey());
            if (i == 1) {
                assertEquals(grant.getProjectName() + "MOOO", passGrant.getProjectName());
            } else {
                assertEquals(grant.getProjectName(), passGrant.getProjectName());
            }
            //assertEquals(grant.getAwardDate(), passGrant.getAwardDate());
            assertEquals(grant.getStartDate(), passGrant.getStartDate());
            assertEquals(grant.getEndDate(), passGrant.getEndDate());

            //let's check funder stuff
            Funder directFunder = new Funder();
            String funderIdPrefix = DOMAIN + ":funder:";
            directFunder.setLocalKey(funderIdPrefix + C_DIRECT_FUNDER_LOCAL_KEY + i);
            directFunder.setName(C_DIRECT_FUNDER_NAME + i);
            directFunder.setPolicy(funderPolicyUriMap.get("DirectFunderPolicy" + i));

            URI directFunderUri = passClient.findByAttribute(Funder.class, "localKey", directFunder.getLocalKey());
            Funder passDirectFunder = passClient.readResource(directFunderUri, Funder.class);
            if (i == 1) {
                assertEquals(directFunder.getName() + "MOOOO", passDirectFunder.getName());
                assertEquals(directFunder.getLocalKey(), passDirectFunder.getLocalKey());
                assertEquals(passDirectFunder.getId(), passGrant.getDirectFunder());
            } else {
                assertEquals(directFunder.getName(), passDirectFunder.getName());
            }

            Funder primaryFunder = new Funder();
            primaryFunder.setLocalKey(funderIdPrefix + C_PRIMARY_FUNDER_LOCAL_KEY + i);
            primaryFunder.setName(C_PRIMARY_FUNDER_NAME + i);
            primaryFunder.setPolicy(funderPolicyUriMap.get("PrimaryFunderPolicy" + i));

            URI primaryFunderUri = passClient.findByAttribute(Funder.class, "localKey", primaryFunder.getLocalKey());
            Funder passPrimaryFunder = passClient.readResource(primaryFunderUri, Funder.class);
            assertEquals(primaryFunder.getName(), passPrimaryFunder.getName());
            assertEquals(passPrimaryFunder.getId(), passGrant.getPrimaryFunder());
            assertEquals(primaryFunder.getLocalKey(), passPrimaryFunder.getLocalKey());
            assertEquals(primaryFunder.getPolicy(), passPrimaryFunder.getPolicy());

            User user = new User();

            //institutionalId and localKey were localized by the grant loader
            user.getLocatorIds().add(employeeidPrefix + C_USER_EMPLOYEE_ID + i);
            //String idPrefix = "johnshopkins.edu:hopkinsid:";
            //user.getLocatorIds().add(hopkinsidPrefix + C_USER_HOPKINS_ID + i);
            //user.getLocatorIds().add(jhedPrefix + C_USER_INSTITUTIONAL_ID.toLowerCase() + i);
            user.setFirstName(C_USER_FIRST_NAME + i);
            //user.setMiddleName(C_USER_MIDDLE_NAME + i);
            user.setLastName(C_USER_LAST_NAME + i);
            user.setEmail(C_USER_EMAIL + i);

            URI userUri = null;
            ListIterator idIterator = user.getLocatorIds().listIterator();

            while (userUri == null && idIterator.hasNext()) {
                String id = String.valueOf(idIterator.next());
                if (id != null) {
                    userUri = passClient.findByAttribute(User.class, "locatorIds", id);
                }
            }

            User passUser = passClient.readResource(userUri, User.class);
            if (i == 1) {
               assertEquals(user.getFirstName() + "MOOOOO", passUser.getFirstName());
            } else {
                assertEquals(user.getFirstName(), passUser.getFirstName());
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


}
