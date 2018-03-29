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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.dataconservancy.pass.grant.data.CoeusFieldNames.*;
import static org.dataconservancy.pass.grant.data.DateTimeUtil.createJodaDateTime;

/**
 * This class is responsible for taking the ResultSet from the database query and constructing a corresponding
 * Collection of Grant objects, which it then sends to fedora to update.
 *
 * @author jrm@jhu.edu
 */
public class GrantUpdater {
    private static Logger LOG = LoggerFactory.getLogger(GrantUpdater.class);

    private List<Map<String,String>> results;
    private String latestUpdateString = "";
    private String report = "";
    private FedoraPassClient fedoraClient = new FedoraPassClient();

    public GrantUpdater(List<Map<String,String>> results) {
        this.results = results;
    }

    /**
     * Build a Collection of Grants from a ResultSet, then update the grants in Fedora
     * Because we need to make sure we catch any updates to fields referenced by URIs, we construct
     * these and update these as well
     * @throws SQLException if the database had an issue with our SQL query
     */
    public void updateGrants() throws SQLException {


        //a grant will have several rows in the ResultSet if there are co-pis. so we put the grant on this
        //Map and add to it as additional rows add information.
        Map<String, Grant> grantMap = new HashMap<>();

        //some entities may be referenced many times during an update, but just need to be updated the first time
        //they are encountered. these include Persons and Funders. we save the overhead of redundant updates
        //of these by looking them up here; if they are on the Map, they have already been processed
        Map<String, URI> funderMap = new HashMap<>();
        Map<String, URI> personMap = new HashMap<>();
        LOG.info("Processing result set with " + results.size() + " rows");

        for(Map<String,String> rowMap : results){
            Grant grant;
            String localAwardId = rowMap.get(C_GRANT_LOCAL_AWARD_ID);
            LOG.info("Processing result row for grant number " + localAwardId);

            URI grantURI = fedoraClient.findByAttribute(Grant.class, "localAwardId", localAwardId);

            //is this the first record we have for this grant? If so, check to see if we already know
            //about it in Fedora. Retrieve it if we have it, build a new one if not
            if (!grantMap.keySet().contains(localAwardId)) {
                if (grantURI == null) {
                    grant = new Grant();
                    grantURI = fedoraClient.createResource(grant);
                }

                grant = fedoraClient.readResource(grantURI, Grant.class);
                grant.setCoPis(new ArrayList<>());//we will build this from scratch in either case

                //process all fields which are the same for every record for this grant
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

                //funder semantics here and sponsor semantics in COEUS are different.
                //in COEUS, we always have a Sponsor. This is the direct source of the $. this could be another university
                //or another department or a funding agency.
                //if we are a sub-awardee, we will also have a Primary Sponsor (like NSF etc.); else this field is null.

                //our semantics are that we always track two things - the directFunder, which is who gives us the $,
                //and the primaryFunder, which is where the $ originated. if it is not a subcontract, these values are the same.

                //process the direct funder. we go to the trouble to see if there are any COEUS fields
                //which differ from the existing fields (when we have the object in fedora already).
                //if not, we don't need to send an update.
                Funder directFunder;
                String directFunderId = rowMap.get((C_DIRECT_FUNDER_LOCAL_ID));
                URI directFunderURI;

                if(!funderMap.containsKey(directFunderId)) {//we haven't processed this funder in this session
                    directFunderURI = fedoraClient.findByAttribute(Funder.class, "localId", directFunderId);
                    if (directFunderURI == null) {
                        directFunder = new Funder();
                        directFunderURI = fedoraClient.createResource(directFunder);
                    }

                    directFunder = fedoraClient.readResource(directFunderURI, Funder.class);
                    directFunder.setLocalId(directFunderId);
                    directFunder.setName(rowMap.get(C_DIRECT_FUNDER_NAME));

                    funderMap.put(directFunderId, directFunderURI);
                    fedoraClient.updateResource(directFunder);

                } else {//save the overhead of checking a redundant update
                    directFunderURI = funderMap.get(directFunderId);
                }
                grant.setDirectFunder(directFunderURI);

                //process the primary funder. we go to the trouble to see if there are any COEUS fields
                //which differ from the existing fields (when we have the object in fedora already).
                //if not, we don't need to send an update.
                Funder primaryFunder;
                String primaryFunderId = rowMap.get((C_PRIMARY_FUNDER_LOCAL_ID));//D.SPONSOR_CODE
                URI primaryFunderURI;

                if (primaryFunderId != null) {
                    if(!funderMap.containsKey(primaryFunderId)) {
                        primaryFunderURI = fedoraClient.findByAttribute(Funder.class, "localId", primaryFunderId);
                        if (primaryFunderURI == null) {
                            primaryFunder = new Funder();
                            primaryFunderURI = fedoraClient.createResource(primaryFunder);
                        }
                        primaryFunder = fedoraClient.readResource(primaryFunderURI, Funder.class);

                        primaryFunder.setLocalId(primaryFunderId);
                        primaryFunder.setName(rowMap.get(C_PRIMARY_FUNDER_NAME));

                        funderMap.put(primaryFunderId, primaryFunderURI);
                        fedoraClient.updateResource(primaryFunder);
                    } else {//save the overhead of checking a redundant update
                        primaryFunderURI = funderMap.get(primaryFunderId);
                    }
                    grant.setPrimaryFunder(primaryFunderURI);
                } else {//primaryFunder is the same as directFunder - not a subaward
                    grant.setPrimaryFunder(directFunderURI);
                }

            } else {//we have started working on this grant already
                grant=grantMap.get(localAwardId);//let's continue
            }

            //now process any person fields on any record which corresponds to this Grant
            //these are different for each record for this grant, as each record
            //refers to a different investigator
            Person investigator;
            String jhedId = rowMap.get(C_PERSON_INSTITUTIONAL_ID);
            URI investigatorURI;
            boolean mustUpdate = false;
            //we take this windy approach to comparing what we have in Fedora with what we have in the ResultSet
            //because the ResultSet fields do not fully cover the model fields
            //if they ever do, we can use PassObject.equals()

            if(!personMap.containsKey(jhedId)) {
                investigatorURI = fedoraClient.findByAttribute(Person.class, "institutionalId", jhedId);

                if (investigatorURI == null) {
                    investigator = new Person();
                    investigatorURI = fedoraClient.createResource(investigator);
                }

                investigator = fedoraClient.readResource(investigatorURI, Person.class);

                String firstName=rowMap.get(C_PERSON_FIRST_NAME);
                String middleName=rowMap.get(C_PERSON_MIDDLE_NAME);
                String lastName = rowMap.get(C_PERSON_LAST_NAME);

                //set display name - we construct it here.
                StringBuilder sb = new StringBuilder();
                sb.append(lastName);
                sb.append(", ");
                sb.append(firstName);
                if (middleName != null && middleName.length()>0){
                    sb.append(" ");
                    sb.append(middleName.charAt(0) );
                }
                String displayName = sb.toString();

                investigator.setFirstName(rowMap.get(firstName));
                investigator.setMiddleName(rowMap.get(middleName));
                investigator.setLastName(rowMap.get(lastName));
                investigator.setDisplayName(displayName);
                investigator.setEmail(rowMap.get(C_PERSON_EMAIL));
                investigator.setInstitutionalId(C_PERSON_INSTITUTIONAL_ID);

                personMap.put(jhedId, investigatorURI);
                fedoraClient.updateResource(investigator);
            } else {//save the overhead of checking a redundant update
                investigatorURI = personMap.get(jhedId);
            }
            if (rowMap.get(C_ABBREVIATED_ROLE).equals("P")) {//it's the PI
                grant.setPi(investigatorURI);
            } else if (rowMap.get(C_ABBREVIATED_ROLE).equals("C")){//it's a co PI
                grant.getCoPis().add(investigatorURI);
            }

            grantMap.put(localAwardId, grant);

            //see if this is the latest grant updated
            String grantUpdateString = rowMap.get(C_UPDATE_TIMESTAMP);
            latestUpdateString = latestUpdateString.length()==0 ? grantUpdateString : returnLaterUpdate(grantUpdateString, latestUpdateString);

        }

        //now put updated grant objects in fedora
        for(Grant grant : grantMap.values()){
            fedoraClient.updateResource(grant);
        }

        //success - we capture some information to report
        report = format("%s grant records processed; most recent update in this batch has timestamp %s", grantMap.size(), getLatestUpdate());

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
        return report;
    }

}
