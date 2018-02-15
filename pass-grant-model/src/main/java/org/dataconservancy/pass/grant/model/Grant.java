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

    private Identifier id;
    private String awardNumber;
    private String projectName;
    private String funder;
    private DateTime startDate;
    private DateTime endDate;
    private List<Identifier> alternateIds;
    private Person creator;
    private DateTime creationDate;
    private Person pi;
    private List<Person> copis;

    private String school; //could be enum?
    private String department;
    private String organizationalUnit;

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

    public String getFunder() {
        return funder;
    }

    public void setFunder(String funder) {
        this.funder = funder;
    }

    public DateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(DateTime startDate) {
        this.startDate = startDate;
    }

    public DateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(DateTime endDate) {
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

    public DateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(DateTime creationDate) {
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

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getOrganizationalUnit() {
        return organizationalUnit;
    }

    public void setOrganizationalUnit(String organizationalUnit) {
        this.organizationalUnit = organizationalUnit;
    }
}
