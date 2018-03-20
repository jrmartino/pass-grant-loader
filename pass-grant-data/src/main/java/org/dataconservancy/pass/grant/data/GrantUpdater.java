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
import org.joda.time.DateTime;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.lang.String.format;
import static org.dataconservancy.pass.grant.data.DateTimeUtil.createJodaDateTime;

/**
 * This class is responsible for taking the ResultSet from the database query and constructing a corresponding
 * List of Grant objects.
 *
 * @author jrm@jhu.edu
 */
public class GrantUpdater {

    private ResultSet rs;
    private String latestUpdateString = "";
    private String report = "";
    FedoraPassClient fedoraClient = new FedoraPassClient();

    public GrantUpdater(ResultSet resultSet) {
        this.rs = resultSet;
    }

    /**
     * Build a List of Grants from a ResultSet, then update the grants in Fedora
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
        //by looking them up here.
        Map<String, URI> funderMap = new HashMap<>();
        Map<String, URI> personMap = new HashMap<>();

        int i=0;
        while (rs.next()) {
            Grant grant;
            String localAwardId = rs.getString("AWARD_NUMBER");

            URI grantURI = fedoraClient.findByAttribute(Grant.class, "AWARD_NUMBER", localAwardId);

            //is this the first record we have for this grant? If so, check to see if we already know
            //about it in Fedora. Retrieve it if we have it, build a new one if not
            //if we have already seen this grant, we have its URI and have set all single valued fields
            if (!grantMap.keySet().contains(localAwardId)) {
                if (grantURI == null) {
                    grant = new Grant();
                    grantURI = fedoraClient.createResource(grant);
                }

                grant = (Grant) fedoraClient.readResource(grantURI, Grant.class);
                grant.setCoPis(new ArrayList<>());//we will build this from scratch in either case

                //process all single valued fields - everyone by co-pis at the moment
                grant.setAwardNumber(rs.getString("GRANT_NUMBER"));
                switch (rs.getString("AWARD_STATUS")) {
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
                grant.setProjectName(rs.getString("TITLE"));
                grant.setAwardDate(createJodaDateTime(rs.getString("AWARD_DATE")));
                grant.setStartDate(createJodaDateTime(rs.getString("AWARD_START")));
                grant.setEndDate(createJodaDateTime(rs.getString("AWARD_END")));


                //funder semantics here and sponsor semantics in COEUS are different.
                //in COEUS, we always have a Sponsor. This is the direct source of the $. (like another university)
                //if we have a sub-award, we also have a Primary Sponsor (like NSF etc.); else this field is null.

                //our semantics are that we always track two things - the directFunder, which is who gives us the $,
                //and the primary funder, which is where the $ originated. if it is not a subcontract, these values are the same.

                Funder directFunder;
                String directFunderId = rs.getString(("SPOSNOR_CODE"));//A.SPOSNOR_CODE
                URI directFunderURI;

                if(!funderMap.containsKey(directFunderId)) {
                    directFunderURI = fedoraClient.findByAttribute(Funder.class, "localId", directFunderId);
                    if (directFunderId == null) {
                        directFunder = new Funder();
                        directFunderURI = fedoraClient.createResource(directFunder);
                    }
                    directFunder = (Funder) fedoraClient.readResource(directFunderURI, Funder.class);

                    directFunder.setLocalId(directFunderId);
                    directFunder.setName(rs.getString("SPONSOR"));//A.SPONSOR
                    fedoraClient.updateResource(directFunder);
                    funderMap.put(directFunderId, directFunderURI);
                } else {//save the overhead of a redundant update
                    directFunderURI = funderMap.get(directFunderId);
                }
                grant.setDirectFunder(directFunderURI);

                Funder primaryFunder;
                String primaryFunderId = rs.getString(("SPONSOR_CODE"));//D.SPONSOR_CODE
                URI primaryFunderURI;

                if (primaryFunderId != null) {
                    if(!funderMap.containsKey(primaryFunderId)) {
                        primaryFunderURI = fedoraClient.findByAttribute(Funder.class, "localId", primaryFunderId);
                        if (primaryFunderId == null) {
                            primaryFunder = new Funder();
                            primaryFunderURI = fedoraClient.createResource(primaryFunder);
                        }
                        primaryFunder = (Funder) fedoraClient.readResource(primaryFunderURI, Funder.class);

                        primaryFunder.setLocalId(primaryFunderId);
                        primaryFunder.setName(rs.getString("SPONSOR_NAME"));//D.SPONSOR_NAME
                        fedoraClient.updateResource(primaryFunder);
                        funderMap.put(primaryFunderId, primaryFunderURI);
                    } else {
                        primaryFunderURI = funderMap.get(primaryFunderId);
                    }
                    grant.setPrimaryFunder(primaryFunderURI);
                } else {//save the overhead of a redundant update
                    grant.setPrimaryFunder(directFunderURI);
                }

            } else {//we have started working on this grant already
                grant=grantMap.get(localAwardId);//let's continue
            }
            //now process any person fields on passes subsequent to initial pass (or initial pass)
            Person investigator;
            String jhedId = rs.getString("JHED_ID");
            URI investigatorURI;

            if(!personMap.containsKey(jhedId)) {
                investigatorURI = fedoraClient.findByAttribute(Person.class, "institutionalId", jhedId);

                if (investigatorURI == null) {
                    investigator = new Person();
                    investigatorURI = fedoraClient.createResource(investigator);
                }

                investigator = (Person) fedoraClient.readResource(investigatorURI, Person.class);
                investigator.setFirstName(rs.getString("FIRST_NAME"));
                investigator.setMiddleName(rs.getString("MIDDLE_NAME"));
                investigator.setLastName(rs.getString("LAST_NAME"));
                investigator.setDisplayName(rs.getString("PRINCIPAL_INV"));
                investigator.setEmail(rs.getString("EMAIL_ADDRESS"));
                investigator.setInstitutionalId(rs.getString("JHED_ID"));

                fedoraClient.updateResource(investigator);
                personMap.put(jhedId, investigatorURI);
            } else {//save the overhead of a redundant update
                investigatorURI = personMap.get(jhedId);
            }
            if (Objects.equals(rs.getString("ABBREVIATED_ROLE"), "P")) {//it's the PI
                grant.setPi(investigatorURI);
            } else {//it's a co PI
                grant.getCoPis().add(investigatorURI);
            }

            grantMap.put(localAwardId, grant);

            //see if this is the latest grant updated
            String grantUpdateString = rs.getString("UPDATE_TIMESTAMP");
            String baseString = "1980-01-01 00:00:00.0";//just some random long ago timestamp to handle the first record
            latestUpdateString = latestUpdateString.length()==0 ? baseString : returnLaterUpdate(grantUpdateString, latestUpdateString);

            //increment the number of records processed
            i++;
        }

        //now put updated grant objects in fedora
        for(Grant grant : grantMap.values()){
            fedoraClient.updateResource(grant);
        }

        //success - we capture some information to report
        report = format("%s grant records processed; most recent update in this batch has timestamp %s", String.valueOf(i), getLatestUpdate());

    }

    /**
     * Compare two timestamps and return the later of them
     * @param currentUpdateString the current latest timestamp string
     * @param latestUpdateString the new timestamp to be compared against the current latest timestamp
     * @return the later of the two parameters
     */
    protected static String returnLaterUpdate(String currentUpdateString, String latestUpdateString) {
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
