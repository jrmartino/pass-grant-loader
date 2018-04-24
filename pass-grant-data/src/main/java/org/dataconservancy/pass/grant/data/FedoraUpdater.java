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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.dataconservancy.pass.client.fedora.FedoraPassClient;
import org.dataconservancy.pass.model.Funder;
import org.dataconservancy.pass.model.Grant;
import org.dataconservancy.pass.model.User;
import org.joda.time.DateTime;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.dataconservancy.pass.grant.data.CoeusFieldNames.*;
import static org.dataconservancy.pass.grant.data.DateTimeUtil.createJodaDateTime;

/**
 * This class is responsible for taking the Set of Maps derived from the ResultSet from the database query and
 * constructing a corresponding Collection of Grant or User objects, which it then sends to Fedora to update.
 *
 * @author jrm@jhu.edu
 */

public class FedoraUpdater {

    private static Logger LOG = LoggerFactory.getLogger(FedoraUpdater.class);
    private String latestUpdateString = "";

    private FedoraPassClient fedoraClient;
    private FedoraUpdateStatistics statistics = new FedoraUpdateStatistics();
    private int grantsUpdated=0;
    private int fundersUpdated=0;
    private int usersUpdated =0;
    private int grantsCreated=0;
    private int fundersCreated=0;
    private int usersCreated=0;
    private int pisAdded=0;
    private int coPisAdded=0;

    //used in test classes
    private Map<URI, Grant> grantUriMap = new HashMap<>();

    //used in unit test
    //some entities may be referenced many times during an update, but just need to be updated the first time
    //they are encountered. these include Users and Funders. we save the overhead of redundant updates
    //of these by looking them up here; if they are on the Map, they have already been processed
    //
    private Map<String, URI> funderMap = new HashMap<>();
    private Map<String, URI> userMap = new HashMap<>();

    public FedoraUpdater(FedoraPassClient fedoraPassClient) {
        this.fedoraClient = fedoraPassClient;
    }

    public void updateFedora(Set<Map<String, String>> results, String mode) {
        statistics.setType(mode);
        if (mode.equals("grant")) {
            updateGrants(results);
        }
        if (mode.equals("user")) {
            updateUsers(results);
        }
    }

    /**
     * Build a Collection of Grants from a ResultSet, then update the grants in Fedora
     * Because we need to make sure we catch any updates to fields referenced by URIs, we construct
     * these and update these as well
     */
    void updateGrants(Set<Map<String, String>> results) {

        //a grant will have several rows in the ResultSet if there are co-pis. so we put the grant on this
        //Map and add to it as additional rows add information.
        Map<String, Grant> grantMap = new HashMap<>();

        LOG.info("Processing result set with " + results.size() + " rows");

        for(Map<String,String> rowMap : results){
            String grantLocalKey = rowMap.get(C_GRANT_LOCAL_KEY);
            String directFunderLocalKey = rowMap.get(C_DIRECT_FUNDER_LOCAL_KEY);
            String primaryFunderLocalKey = rowMap.get(C_PRIMARY_FUNDER_LOCAL_KEY);
            Grant grant;

            //if this is the first record for this Grant, it will not be on the Map
            //we process all data which is common to every record for this grant
            //i.e., everything except the investigator(s)
            if(!grantMap.containsKey(grantLocalKey)) {
                grant = new Grant();
                grant.setAwardNumber(rowMap.get(C_GRANT_AWARD_NUMBER));
                switch (rowMap.get(C_GRANT_AWARD_STATUS)) {
                    case "Active":
                        grant.setAwardStatus(Grant.AwardStatus.ACTIVE);
                        break;
                    case "Pre-Award":
                        grant.setAwardStatus(Grant.AwardStatus.PRE_AWARD);
                        break;
                    case "Terminated":
                        grant.setAwardStatus(Grant.AwardStatus.TERMINATED);
                }

                grant.setLocalKey(grantLocalKey);
                grant.setProjectName(rowMap.get(C_GRANT_PROJECT_NAME));
                grant.setAwardDate(createJodaDateTime(rowMap.get(C_GRANT_AWARD_DATE)));
                grant.setStartDate(createJodaDateTime(rowMap.get(C_GRANT_START_DATE)));
                grant.setEndDate(createJodaDateTime(rowMap.get(C_GRANT_END_DATE)));

                //process direct funder, and primary funder if we have one
                //update funder(s) in fedora as needed
                if (funderMap.containsKey(directFunderLocalKey)) {
                    grant.setDirectFunder(funderMap.get(directFunderLocalKey));
                } else {
                    Funder updatedFunder = new Funder();
                    updatedFunder.setLocalKey(rowMap.get(C_DIRECT_FUNDER_LOCAL_KEY));
                    updatedFunder.setName(rowMap.get(C_DIRECT_FUNDER_NAME));
                    URI fedoraFunderURI =  updateFunderInFedora(updatedFunder);
                    funderMap.put(directFunderLocalKey, fedoraFunderURI);
                    grant.setDirectFunder(fedoraFunderURI);
                }

                if(primaryFunderLocalKey != null) {
                    if(funderMap.containsKey(primaryFunderLocalKey)) {
                        grant.setPrimaryFunder(funderMap.get(primaryFunderLocalKey));
                    } else {
                        Funder updatedFunder = new Funder();
                        updatedFunder.setLocalKey(rowMap.get(C_PRIMARY_FUNDER_LOCAL_KEY));
                        updatedFunder.setName(rowMap.get(C_PRIMARY_FUNDER_NAME));
                        URI fedoraFunderURI =  updateFunderInFedora(updatedFunder);
                        funderMap.put(primaryFunderLocalKey, fedoraFunderURI);
                        grant.setPrimaryFunder(fedoraFunderURI);
                    }
                }

                grant.setCoPis(new ArrayList<>());//we will build this from scratch in either case
                grantMap.put(grantLocalKey, grant);//save the state of this Grant
            }

            //now we process the User (investigator)
            grant = grantMap.get(grantLocalKey);
            String employeeId = rowMap.get(C_USER_LOCAL_KEY);
            String abbreviatedRole = rowMap.get(C_ABBREVIATED_ROLE);

            if(abbreviatedRole.equals("C") || grant.getPi() == null) {
                if (!userMap.containsKey(employeeId)) {
                    User updatedUser = buildUser(rowMap);
                    URI fedoraUserURI = updateUserInFedora(updatedUser);
                    userMap.put(employeeId, fedoraUserURI);
                }

                //now our User URI is on the map - let's process:
                if (abbreviatedRole.equals("P")) {
                    grant.setPi(userMap.get(employeeId));
                    pisAdded++;
                } else if (abbreviatedRole.equals("C") && !grant.getCoPis().contains(userMap.get(employeeId))) {
                    grant.getCoPis().add(userMap.get(employeeId));
                    coPisAdded++;
                }
            }
            //we are done with this record, let's save the state of this Grant
            grantMap.put(grantLocalKey, grant);
            //see if this is the latest grant updated
            String grantUpdateString = rowMap.get(C_UPDATE_TIMESTAMP);
            latestUpdateString = latestUpdateString.length()==0 ? grantUpdateString : returnLaterUpdate(grantUpdateString, latestUpdateString);

        }

        //now put updated grant objects in fedora
        for(Grant grant : grantMap.values()){
            grantUriMap.put(updateGrantInFedora(grant), grant);
        }

        //success - we capture some information to report
        if (grantMap.size() > 0) {
            statistics.setPisAdded(pisAdded);
            statistics.setCoPisAdded(coPisAdded);
            statistics.setFundersCreated(fundersCreated);
            statistics.setFundersUpdated(fundersUpdated);
            statistics.setGrantsCreated(grantsCreated);
            statistics.setGrantsUpdated(grantsUpdated);
            statistics.setUsersCreated(usersCreated);
            statistics.setUsersUpdated(usersUpdated);
            statistics.setLatestUpdateString(latestUpdateString);
            statistics.setReport(results.size(), grantMap.size());
        } else {
            System.out.println("No records were processed in this update");
        }
    }

    private void updateUsers(Set<Map<String, String>> results) {
        for(Map<String,String> rowMap : results) {
            User updatedUser = buildUser(rowMap);
            updateUserInFedora(updatedUser);
        }

        if (results.size() > 0) {
            statistics.setUsersCreated(usersCreated);
            statistics.setUsersUpdated(usersUpdated);
            statistics.setLatestUpdateString(latestUpdateString);
            statistics.setReport(results.size(), results.size());
        } else {
            System.out.println("No records were processed in this update");
        }

    }

    private User buildUser(Map<String, String> rowMap) {
        String firstName = rowMap.get(C_USER_FIRST_NAME);
        String middleName = rowMap.get(C_USER_MIDDLE_NAME);
        String lastName = rowMap.get(C_USER_LAST_NAME);

        User user = new User();
        user.setFirstName(firstName);
        user.setMiddleName(middleName);
        user.setLastName(lastName);
        user.setDisplayName(String.join(" ", firstName, lastName));
        user.setEmail(rowMap.get(C_USER_EMAIL));
        user.setInstitutionalId(rowMap.get(C_USER_INSTITUTIONAL_ID).toLowerCase());
        user.setLocalKey(rowMap.get(C_USER_LOCAL_KEY));
        user.getRoles().add(User.Role.SUBMITTER);
        return user;
    }

    /**
     * Take a new Funder object populated as fully as possible from the COEUS pull, and use this
     * new information to update an object for the same Funder in Fedora (if it exists)
     *
     * @param updatedFunder the new Funder object populated from COEUS
     * @return the URI for the resource representing the updated Funder in Fedora
     */
    private URI updateFunderInFedora(Funder updatedFunder) {
        Funder storedFunder;
        URI fedoraFunderURI = fedoraClient.findByAttribute(Funder.class, "localKey", updatedFunder.getLocalKey());
        if (fedoraFunderURI != null ) {
            storedFunder = fedoraClient.readResource(fedoraFunderURI, Funder.class);
            if (!PassEntityUtil.coeusFundersEqual(updatedFunder, storedFunder)) {
                storedFunder = PassEntityUtil.updateFunder(updatedFunder, storedFunder);
                fedoraClient.updateResource(storedFunder);
                fundersUpdated++;
            }//if the Fedora version is COEUS-equal to our version from the update, we don't have to do anything
             //this can happen if the Grant was updated in COEUS only with information we don't consume here
        } else {//don't have a stored Funder for this URI - this one is new to Fedora
            fedoraFunderURI = fedoraClient.createResource(updatedFunder);
            fundersCreated++;
        }
        return fedoraFunderURI;
    }

    /**
     * Take a new User object populated as fully as possible from the COEUS pull, and use this
     * new information to update an object for the same User in Fedora (if it exists)
     *
     * @param updatedUser the new User object populated from COEUS
     * @return the URI for the resource representing the updated User in Fedora
     */
    private URI updateUserInFedora(User updatedUser) {
        User storedUser;
        //we may have Users in Fedora who are not in the COEUS system yet, and so we haven't had access to their employee id.
        // we fall back to jhed id for finding these users - update them here when they appear in COEUS
        URI fedoraUserURI = fedoraClient.findByAttribute(User.class, "localKey", updatedUser.getLocalKey()) != null?
                fedoraClient.findByAttribute(User.class, "localKey", updatedUser.getLocalKey()):
                fedoraClient.findByAttribute(User.class, "institutionalId", updatedUser.getInstitutionalId());
        if (fedoraUserURI != null ) {
            storedUser = fedoraClient.readResource(fedoraUserURI, User.class);
            if (!PassEntityUtil.coeusUsersEqual(updatedUser, storedUser)) {
                storedUser = PassEntityUtil.updateUser(updatedUser, storedUser);
                //post COEUS processing goes here
                if(!storedUser.getRoles().contains(User.Role.SUBMITTER)) {
                    storedUser.getRoles().add(User.Role.SUBMITTER);
                }
                fedoraClient.updateResource(storedUser);
                usersUpdated++;
            }//if the Fedora version is COEUS-equal to our version from the update, we don't have to do anything
             //this can happen if the User was updated in COEUS only with information we don't consume here
        } else {//don't have a stored User for this URI - this one is new to Fedora
            fedoraUserURI = fedoraClient.createResource(updatedUser);
            usersCreated++;
        }
        return fedoraUserURI;
    }

    /**
     * Take a new Grant object populated as fully as possible from the COEUS pull, and use this
     * new information to update an object for the same Grant in Fedora (if it exists)
     *
     * @param updatedGrant the new Grant object populated from COEUS
     */
    private URI updateGrantInFedora(Grant updatedGrant) {
        Grant storedGrant;
        URI fedoraGrantURI = fedoraClient.findByAttribute(Grant.class, "localKey", updatedGrant.getLocalKey());
        if (fedoraGrantURI != null ) {
            storedGrant = fedoraClient.readResource(fedoraGrantURI, Grant.class);
            if (!PassEntityUtil.coeusGrantsEqual(updatedGrant, storedGrant)) {
                storedGrant = PassEntityUtil.updateGrant(updatedGrant, storedGrant);
                fedoraClient.updateResource(storedGrant);
                grantsUpdated++;
            }//if the Fedora version is COEUS-equal to our version from the update, we don't have to do anything
             //this can happen if the Grant was updated in COEUS only with information we don't consume here
        } else {//don't have a stored Grant for this URI - this one is new to Fedora
            fedoraGrantURI = fedoraClient.createResource(updatedGrant);
            grantsCreated++;
        }
        return fedoraGrantURI;
    }

    /**
     * Compare two timestamps and return the later of them
     * @param currentUpdateString the current latest timestamp string
     * @param latestUpdateString the new timestamp to be compared against the current latest timestamp
     * @return the later of the two parameters
     */
    static String returnLaterUpdate(String currentUpdateString, String latestUpdateString) {
        DateTime grantUpdateTime = createJodaDateTime(currentUpdateString);
        DateTime previousLatestUpdateTime = createJodaDateTime(latestUpdateString);
        return grantUpdateTime.isAfter(previousLatestUpdateTime)? currentUpdateString : latestUpdateString;
    }

    /**
     * This method provides the latest timestamp of all records processed. After processing, this timestamp
     * will be used to be tha base timestamp for the next run of the app
     * @return the latest update timestamp string
     */
    public String getLatestUpdate(){
        return this.latestUpdateString;
    }

    /**
     * This returns the final statistics of the processing of the Grant or User Set
     * @return the report
     */
    public String getReport(){
        return statistics.getReport();
    }

    /**
     * This returns the final statistics Object - useful in testing
     * @return the statistics object
     */
    public FedoraUpdateStatistics getStatistics() {
        return statistics;
    }


    public Map<URI, Grant> getGrantUriMap() {
        return grantUriMap;
    }

    //this is used by an integration test
    public FedoraPassClient getFedoraClient() {
        return fedoraClient;
    }


    public Map<String, URI> getFunderMap() {
        return funderMap;
    }

    //used in unit test
    public Map<String, URI> getUserMap() {
        return userMap;
    }

}
