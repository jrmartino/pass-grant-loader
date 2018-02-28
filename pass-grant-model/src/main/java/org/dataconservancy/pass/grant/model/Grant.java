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
package org.dataconservancy.pass.grant.model;

import org.joda.time.DateTime;

import java.util.List;

public class Grant {

    private String awardNumber;
    private String localAwardId;
    private String projectName;
    private String primaryFunder;
    private String directFunder;
    private String awardDate;
    private String startDate;
    private String endDate;
    private Person creator;
    private DateTime created;
    private DateTime lastModified;
    private Person pi;
    private List<Person> copis;
    private String division; //could be enum?
    private String department;
    private String organizationalUnitName;


    private status awardStatus;

    public enum  status {
        ACTIVE,
        PRE_AWARD,
        TERMINATED
    }

    public enum compliance {
        YES,
        NO,
        IN_PROGRESS
    }


    public String getAwardNumber() {
        return awardNumber;
    }

    public void setAwardNumber(String awardNumber) {
        this.awardNumber = awardNumber;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getAwardDate() {
        return awardDate;
    }

    public void setAwardDate(String awardDate) {
        this.awardDate = awardDate;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }


    public Person getCreator() {
        return creator;
    }

    public void setCreator(Person creator) {
        this.creator = creator;
    }

    public Person getPi() {
        return pi;
    }

    public void setPi(Person pi) {
        this.pi = pi;
    }

    public List<Person> getCopis() {
        return copis;
    }

    public void setCopis(List<Person> copis) {
        this.copis = copis;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getOrganizationalUnitName() {
        return organizationalUnitName;
    }

    public void setOrganizationalUnitName(String organizationalUnit) { this.organizationalUnitName = organizationalUnit; }

    public status getAwardStatus() {
        return awardStatus;
    }

    public void setAwardStatus(status awardStatus) {
        this.awardStatus = awardStatus;
    }

    public String getLocalAwardId() {
        return localAwardId;
    }

    public void setLocalAwardId(String localAwardId) {
        this.localAwardId = localAwardId;
    }

    public String getPrimaryFunder() {
        return primaryFunder;
    }

    public void setPrimaryFunder(String primaryFunder) {
        this.primaryFunder = primaryFunder;
    }

    public String getDirectFunder() {
        return directFunder;
    }

    public void setDirectFunder(String directFunder) {
        this.directFunder = directFunder;
    }

    /**created is system generated, not modifiable */
    public DateTime getCreated() {
        return created;
    }

    /** lastModified is system generated, not modifiable */
    public DateTime getLastModified() {
        return lastModified;
    }



}
