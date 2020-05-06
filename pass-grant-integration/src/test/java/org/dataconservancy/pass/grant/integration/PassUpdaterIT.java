package org.dataconservancy.pass.grant.integration;

import org.dataconservancy.pass.client.PassClient;
import org.dataconservancy.pass.client.PassClientFactory;
import org.dataconservancy.pass.model.Policy;
import org.junit.Before;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import static org.dataconservancy.pass.grant.data.CoeusFieldNames.*;

/**
 * This class is a base class mainly for setting up test data to be processed by descendants of the DefaultPassUpdater
 */
public class PassUpdaterIT {

    String grantAwardNumber1 = "A10000000";
    String grantAwardNumber2 = "A10000001";
    String grantAwardNumber3 = "A10000002";
    String grantLocalKey1 = "10000000";
    String grantLocalKey2 = "100000001";
    String grantLocalKey3 = "100000002";
    String grantProjectName1 = "Awesome Research Project I";
    String grantProjectName2 = "Awesome Research Project II";
    String grantProjectName3 = "Awesome Research Project III";
    String grantAwardDate1 = "01/01/1999";
    String grantAwardDate2 = "01/01/2001";
    String grantAwardDate3 = "01/01/2003";
    String grantStartDate1 = "07/01/2000";
    String grantStartDate2 = "07/01/2002";
    String grantStartDate3 = "07/01/2004";
    String grantEndDate1 = "06/30/2002";
    String grantEndDate2 = "06/30/2004";
    String grantEndDate3 = "06/30/2006";

    String grantUpdateTimestamp1 = "2006-03-11 00:00:00.0";
    String grantUpdateTimestamp2 = "2010-04-05 00:00:00.0";
    String grantUpdateTimestamp3 = "2015-11-11 00:00:00.0";

    String userEmployeeId1 = "30000000";
    String userEmployeeId2 = "30000001";
    String userEmployeeId3 = "30000002";

    String primaryFunderPolicyUriString;
    String directFunderPolicyUriString;

    String grantIdPrefix = "johnshopkins.edu:grant:";
    String funderIdPrefix = "johnshopkins.edu:funder:";
    String hopkinsidPrefix = "johnshopkins.edu:hopkinsid:";
    String employeeidPrefix = "johnshopkins.edu:employeeid:";
    String jhedidPrefis = "johnshopkins.edu:jhed:";

    PassClient passClient = PassClientFactory.getPassClient();

    @Before
    public void setup() {
        String prefix = System.getProperty("pass.fedora.baseurl");
        if ( !prefix.endsWith("/") ) {
            prefix = prefix + "/";
        }

        Policy policy = new Policy();
            policy.setTitle("Primary Policy");
            policy.setDescription("MOO");
            URI policyURI = passClient.createResource(policy);
            primaryFunderPolicyUriString = policyURI.toString().substring(prefix.length());
     //       funderPolicyUriMap.put("PrimaryFunderPolicy"+i,policyURI);

        policy =new Policy();
            policy.setTitle("Direct Policy");
            policy.setDescription("MOO");
            policyURI =passClient.createResource(policy);
            directFunderPolicyUriString = policyURI.toString().substring(prefix.length());
     //       funderPolicyUriMap.put("DirectFunderPolicy"+i,policyURI);

    }

    Map<String, String> makeBaseRowMap() {

        Map<String, String> rowMap = new HashMap<>();
        rowMap.put(C_GRANT_AWARD_NUMBER, grantAwardNumber1);
        rowMap.put(C_GRANT_AWARD_STATUS, "Active");
        rowMap.put(C_GRANT_LOCAL_KEY, grantLocalKey1);
        rowMap.put(C_GRANT_PROJECT_NAME, grantProjectName1);
        rowMap.put(C_GRANT_AWARD_DATE, grantAwardDate1);
        rowMap.put(C_GRANT_START_DATE, grantStartDate1);
        rowMap.put(C_GRANT_END_DATE, grantEndDate1);

        rowMap.put(C_DIRECT_FUNDER_LOCAL_KEY, "20000000");
        rowMap.put(C_DIRECT_FUNDER_NAME, "Enormous State University");
        rowMap.put(C_PRIMARY_FUNDER_LOCAL_KEY, "20000001");
        rowMap.put(C_PRIMARY_FUNDER_NAME, "J L Gotrocks Foundation");

        rowMap.put(C_USER_FIRST_NAME, "Andrew");
        rowMap.put(C_USER_MIDDLE_NAME, "Carnegie");
        rowMap.put(C_USER_LAST_NAME, "Melon");
        rowMap.put(C_USER_EMAIL, "amelon1@jhu.edu");
        rowMap.put(C_USER_INSTITUTIONAL_ID, "amelon1");
        rowMap.put(C_USER_EMPLOYEE_ID, userEmployeeId1);
        rowMap.put(C_USER_HOPKINS_ID, "OEJSNR");

        rowMap.put(C_UPDATE_TIMESTAMP, grantUpdateTimestamp1);
        rowMap.put(C_ABBREVIATED_ROLE, ("P"));

        rowMap.put(C_DIRECT_FUNDER_POLICY, directFunderPolicyUriString);
        rowMap.put(C_PRIMARY_FUNDER_POLICY, primaryFunderPolicyUriString);

        return  rowMap;
    }

    Map<String, String> makeAdjustedBaseRowMap( Map<String, String> replacements) {
        Map<String, String> adjustedMap = new HashMap<>();
        adjustedMap.putAll(makeBaseRowMap());
        for (String key : replacements.keySet()) {
            adjustedMap.replace(key, replacements.get(key));
        }
        return adjustedMap;
    }

    Map<String, String> makeAdjustedRowMap( Map<String, String> startMap, Map<String, String> replacements) {
        Map<String, String> adjustedMap = new HashMap<>();
        adjustedMap.putAll( startMap );
        for (String key : replacements.keySet()) {
            adjustedMap.replace(key, replacements.get(key));
        }
        return adjustedMap;
    }


    Map<String, String> changeUserInMap(Map<String, String> startMap, String firstName, String middleName, String lastName,
                                                String email, String instId, String employId,
                                                String hopkinsId, String abbrRole) {
        Map<String, String> adjustedMap = new  HashMap<>();
        adjustedMap.putAll( startMap );
        adjustedMap.replace(C_USER_FIRST_NAME, firstName);
        adjustedMap.replace(C_USER_MIDDLE_NAME, middleName);
        adjustedMap.replace(C_USER_LAST_NAME, lastName);
        adjustedMap.replace(C_USER_EMAIL, email);
        adjustedMap.replace(C_USER_INSTITUTIONAL_ID, instId);
        adjustedMap.replace(C_USER_EMPLOYEE_ID, employId);
        adjustedMap.replace(C_USER_HOPKINS_ID, hopkinsId);
        adjustedMap.replace(C_ABBREVIATED_ROLE, abbrRole);

        return adjustedMap;
    }


    
}