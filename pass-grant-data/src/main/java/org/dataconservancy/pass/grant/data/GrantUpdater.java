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
import org.dataconservancy.pass.model.Person;
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
 * constructing a corresponding Collection of Grant objects, which it then sends to Fedora to update.
 *
 * @author jrm@jhu.edu
 */

public class GrantUpdater {

    private static Logger LOG = LoggerFactory.getLogger(GrantUpdater.class);
    private String latestUpdateString = "";

    private FedoraPassClient fedoraClient;
    private FedoraUpdateStatistics statistics = new FedoraUpdateStatistics();
    private int grantsUpdated=0;
    private int fundersUpdated=0;
    private int personsUpdated=0;
    private int grantsCreated=0;
    private int fundersCreated=0;
    private int personsCreated=0;
    private int pisAdded=0;
    private int coPisAdded=0;

    //used in test classes
    private Map<URI, Grant> grantUriMap = new HashMap<>();

    //used in unit test
    //some entities may be referenced many times during an update, but just need to be updated the first time
    //they are encountered. these include Persons and Funders. we save the overhead of redundant updates
    //of these by looking them up here; if they are on the Map, they have already been processed
    //
    private Map<String, URI> funderMap = new HashMap<>();
    private Map<String, URI> personMap = new HashMap<>();

    public GrantUpdater(FedoraPassClient fedoraPassClient) {
        this.fedoraClient = fedoraPassClient;
    }

    /**
     * Build a Collection of Grants from a ResultSet, then update the grants in Fedora
     * Because we need to make sure we catch any updates to fields referenced by URIs, we construct
     * these and update these as well
     */
    public void updateGrants(Set<Map<String,String>> results) {

        //a grant will have several rows in the ResultSet if there are co-pis. so we put the grant on this
        //Map and add to it as additional rows add information.
        Map<String, Grant> grantMap = new HashMap<>();

        LOG.info("Processing result set with " + results.size() + " rows");

        for(Map<String,String> rowMap : results){
            String localAwardId = rowMap.get(C_GRANT_LOCAL_AWARD_ID);
            String directFunderLocalId = rowMap.get(C_DIRECT_FUNDER_LOCAL_ID);
            String primaryFunderLocalId = rowMap.get(C_PRIMARY_FUNDER_LOCAL_ID);
            Grant grant;

            //if this is the first record for this Grant, it will not be on the Map
            //we process all data which is common to every record for this grant
            //i.e., everything except the investigator(s)
            if(!grantMap.containsKey(localAwardId)) {
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
                grant.setLocalAwardId(localAwardId);
                grant.setProjectName(rowMap.get(C_GRANT_PROJECT_NAME));
                grant.setAwardDate(createJodaDateTime(rowMap.get(C_GRANT_AWARD_DATE)));
                grant.setStartDate(createJodaDateTime(rowMap.get(C_GRANT_START_DATE)));
                grant.setEndDate(createJodaDateTime(rowMap.get(C_GRANT_END_DATE)));

                //process direct funder, and primary funder if we have one
                //update funder(s) in fedora as needed
                if (funderMap.containsKey(directFunderLocalId)) {
                    grant.setDirectFunder(funderMap.get(directFunderLocalId));
                } else {
                    Funder updatedFunder = new Funder();
                    updatedFunder.setLocalId(rowMap.get(C_DIRECT_FUNDER_LOCAL_ID));
                    updatedFunder.setName(rowMap.get(C_DIRECT_FUNDER_NAME));
                    URI fedoraFunderURI =  updateFunderInFedora(updatedFunder);
                    funderMap.put(directFunderLocalId, fedoraFunderURI);
                    grant.setDirectFunder(fedoraFunderURI);
                }

                if(primaryFunderLocalId != null) {
                    if(funderMap.containsKey(primaryFunderLocalId)) {
                        grant.setPrimaryFunder(funderMap.get(primaryFunderLocalId));
                    } else {
                        Funder updatedFunder = new Funder();
                        updatedFunder.setLocalId(rowMap.get(C_PRIMARY_FUNDER_LOCAL_ID));
                        updatedFunder.setName(rowMap.get(C_PRIMARY_FUNDER_LOCAL_ID));
                        URI fedoraFunderURI =  updateFunderInFedora(updatedFunder);
                        funderMap.put(primaryFunderLocalId, fedoraFunderURI);
                        grant.setPrimaryFunder(fedoraFunderURI);
                    }
                }

                grant.setCoPis(new ArrayList<>());//we will build this from scratch in either case
                grantMap.put(localAwardId, grant);//save the state of this Grant
            }
            //now we process the Person (investigator)
            grant = grantMap.get(localAwardId);
            String investigatorId = rowMap.get(C_PERSON_INSTITUTIONAL_ID);
            String abbreviatedRole = rowMap.get(C_ABBREVIATED_ROLE);

            if(abbreviatedRole.equals("C") || grant.getPi() == null) {
                if (!personMap.containsKey(investigatorId)) {
                    String firstName = rowMap.get(C_PERSON_FIRST_NAME);
                    String middleName = rowMap.get(C_PERSON_MIDDLE_NAME);
                    String lastName = rowMap.get(C_PERSON_LAST_NAME);

                    //set display name - we construct it here.
                    StringBuilder sb = new StringBuilder();
                    sb.append(lastName);
                    sb.append(", ");
                    sb.append(firstName);
                    if (middleName != null && middleName.length() > 0) {
                        sb.append(" ");
                        sb.append(middleName.charAt(0));
                    }
                    String displayName = sb.toString();

                    Person updatedPerson = new Person();
                    updatedPerson.setFirstName(firstName);
                    updatedPerson.setMiddleName(middleName);
                    updatedPerson.setLastName(lastName);
                    updatedPerson.setDisplayName(displayName);
                    updatedPerson.setEmail(rowMap.get(C_PERSON_EMAIL));
                    updatedPerson.setInstitutionalId(investigatorId);

                    URI fedoraPersonURI = updatePersonInFedora(updatedPerson);
                    personMap.put(investigatorId, fedoraPersonURI);
                }

                //now our Person URI is on the map - let's process:
                if (abbreviatedRole.equals("P")) {
                    grant.setPi(personMap.get(investigatorId));
                    pisAdded++;
                } else if (abbreviatedRole.equals("C") && !grant.getCoPis().contains(personMap.get(investigatorId))) {
                    grant.getCoPis().add(personMap.get(investigatorId));
                    coPisAdded++;
                }
            }
            //we are done with this record, let's save the state of this Grant
            grantMap.put(localAwardId, grant);
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
            statistics.setPersonsCreated(personsCreated);
            statistics.setPersonsUpdated(personsUpdated);
            statistics.setLatestUpdateString(latestUpdateString);
            statistics.setReport(results.size(), grantMap.size());

        } else {
            System.out.println("No records were processed in this update");
        }
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
        URI fedoraFunderURI = fedoraClient.findByAttribute(Funder.class, "localId", updatedFunder.getLocalId());
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
     * Take a new Person object populated as fully as possible from the COEUS pull, and use this
     * new information to update an object for the same Person in Fedora (if it exists)
     *
     * @param updatedPerson the new Person object populated from COEUS
     * @return the URI for the resource representing the updated Person in Fedora
     */
    private URI updatePersonInFedora(Person updatedPerson) {
        Person storedPerson;
        URI fedoraPersonURI = fedoraClient.findByAttribute(Person.class, "institutionalId", updatedPerson.getInstitutionalId());
        if (fedoraPersonURI != null ) {
            storedPerson = fedoraClient.readResource(fedoraPersonURI, Person.class);
            if (!PassEntityUtil.coeusPersonsEqual(updatedPerson, storedPerson)) {
                storedPerson = PassEntityUtil.updatePerson(updatedPerson, storedPerson);
                fedoraClient.updateResource(storedPerson);
                personsUpdated++;
            }//if the Fedora version is COEUS-equal to our version from the update, we don't have to do anything
             //this can happen if the Grant was updated in COEUS only with information we don't consume here
        } else {//don't have a stored Person for this URI - this one is new to Fedora
            fedoraPersonURI = fedoraClient.createResource(updatedPerson);
            personsCreated++;
        }
        return fedoraPersonURI;
    }

    /**
     * Take a new Grant object populated as fully as possible from the COEUS pull, and use this
     * new information to update an object for the same Grant in Fedora (if it exists)
     *
     * @param updatedGrant the new Grant object populated from COEUS
     */
    private URI updateGrantInFedora(Grant updatedGrant) {
        Grant storedGrant;
        URI fedoraGrantURI = fedoraClient.findByAttribute(Grant.class, "localAwardId", updatedGrant.getLocalAwardId());
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
     * This returns the final statistics of the processing of the Grant List
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
    public Map<String, URI> getPersonMap() {
        return personMap;
    }

}
