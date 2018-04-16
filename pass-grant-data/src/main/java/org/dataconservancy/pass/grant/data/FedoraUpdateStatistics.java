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

import static java.lang.String.format;

public class FedoraUpdateStatistics {

    private int grantsUpdated = 0;
    private int fundersUpdated = 0;
    private int personsUpdated = 0;
    private int grantsCreated = 0;
    private int fundersCreated = 0;
    private int personsCreated = 0;
    private int pisAdded = 0;
    private int coPisAdded = 0;
    private String latestUpdateString = "";
    private String report ="";

    public String getReport() {
        return report;
    }

    public void setReport(int resultSetSize, int grantMapSize ) {
        StringBuilder sb = new StringBuilder();
        sb.append(format("%s grant records processed; the most recent update in this batch has timestamp %s",
                resultSetSize, latestUpdateString));
        sb.append("\n");
        sb.append(format("%s Pis and %s Co-Pis were processed on %s grants", pisAdded, coPisAdded, grantMapSize));
        sb.append("\n\n");
        sb.append("Fedora Activity");
        sb.append("\n\n");
        sb.append(format("%s Grants were created; %s Grants were updated", grantsCreated, grantsUpdated));
        sb.append("\n");
        sb.append(format("%s Persons were created; %s Persons were updated", personsCreated, personsUpdated));
        sb.append("\n");
        sb.append(format("%s Funders were created; %s Funders were updated", fundersCreated, fundersUpdated));

        sb.append("\n");
        this.report = sb.toString();
    }


    public int getGrantsUpdated() {
        return grantsUpdated;
    }

    public void setGrantsUpdated(int grantsUpdated) {
        this.grantsUpdated = grantsUpdated;
    }

    public int getFundersUpdated() {
        return fundersUpdated;
    }

    public void setFundersUpdated(int fundersUpdated) {
        this.fundersUpdated = fundersUpdated;
    }

    public int getPersonsUpdated() {
        return personsUpdated;
    }

    public void setPersonsUpdated(int personsUpdated) {
        this.personsUpdated = personsUpdated;
    }

    public int getGrantsCreated() {
        return grantsCreated;
    }

    public void setGrantsCreated(int grantsCreated) {
        this.grantsCreated = grantsCreated;
    }

    public int getFundersCreated() {
        return fundersCreated;
    }

    public void setFundersCreated(int fundersCreated) {
        this.fundersCreated = fundersCreated;
    }

    public int getPersonsCreated() {
        return personsCreated;
    }

    public void setPersonsCreated(int personsCreated) {
        this.personsCreated = personsCreated;
    }

    public int getPisAdded() {
        return pisAdded;
    }

    public void setPisAdded(int pisAdded) {
        this.pisAdded = pisAdded;
    }

    public int getCoPisAdded() {
        return coPisAdded;
    }

    public void setCoPisAdded(int coPisAdded) {
        this.coPisAdded = coPisAdded;
    }

    public String getLatestUpdateString() {
        return latestUpdateString;
    }

    public void setLatestUpdateString(String latestUpdateString) {
        this.latestUpdateString = latestUpdateString;
    }
}

