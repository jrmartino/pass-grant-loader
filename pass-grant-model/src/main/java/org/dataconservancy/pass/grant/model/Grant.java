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

import java.util.List;

public class Grant {

    private Identifier id;
    private Identifier institutionalId;
    private String awardNumber;
    private String projectName;
    private String sponsor;
    private String sponsorCode;
    private String primeSponsor;
    private String awardDate;
    private String startDate;
    private String endDate;
    private List<Identifier> alternateIds;
    private Person creator;
    private String creationDate;
    private Person pi;
    private List<Person> copis;
    private String division; //could be enum?
    private String department;
    private String organizationalUnitName;
    private String organizationalunitNumber;


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

    public Identifier getId() {
        return id;
    }

    public void setId(Identifier id) {
        this.id = id;
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

    public String getSponsor() {
        return sponsor;
    }

    public void setSponsor(String sponsor) {
        this.sponsor = sponsor;
    }

    public String getSponsorCode() {
        return sponsorCode;
    }
    public void setSponsorCode(String sponsorCode) {
        this.sponsorCode = sponsorCode;
    }

    public String getPrimeSponsor() {
        return primeSponsor;
    }

    public void setPrimeSponsor(String primeSponsor) {
        this.primeSponsor = primeSponsor;
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

    public List<Identifier> getAlternateIds() {
        return alternateIds;
    }

    public void setAlternateIds(List<Identifier> alternateIds) {
        this.alternateIds = alternateIds;
    }

    public Person getCreator() {
        return creator;
    }

    public void setCreator(Person creator) {
        this.creator = creator;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
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

    public String getOrganizationalunitNumber() { return organizationalunitNumber; }

    public void setOrganizationalunitNumber(String organizationalunitNumber) { this.organizationalunitNumber = organizationalunitNumber; }

    public status getAwardStatus() {
        return awardStatus;
    }

    public void setAwardStatus(status awardStatus) {
        this.awardStatus = awardStatus;
    }

    public Identifier getInstitutionalId() {
        return institutionalId;
    }

    public void setInstitutionalId(Identifier institutionalId) {
        this.institutionalId = institutionalId;
    }


}
