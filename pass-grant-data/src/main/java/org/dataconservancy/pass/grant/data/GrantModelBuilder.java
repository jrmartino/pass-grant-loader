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

import org.dataconservancy.pass.grant.model.Grant;
import org.dataconservancy.pass.grant.model.Identifier;
import org.dataconservancy.pass.grant.model.Person;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class GrantModelBuilder {

    ResultSet rs;

    public GrantModelBuilder(ResultSet resultSet) {
        this.rs = resultSet;
    }

    public List<Grant> buildGrantList() {
        //populate model
        List<Grant> grantList = new ArrayList<>();

        try {
            while (rs.next()) {
                Grant grant = new Grant();
                Person person = new Person();

                grant.setProjectName(rs.getString("TITLE"));
                grant.setAwardNumber(rs.getString("GRANT_NUMBER"));
                grant.setAwardDate(rs.getString("AWARD_DATE"));
                grant.setEndDate(rs.getString("AWARD_END"));
                grant.setAwardNumber(rs.getString("AWARD_NUMBER"));
                grant.setStartDate(rs.getString("AWARD_START"));
                switch (rs.getString("AWARD_STATUS")) {
                    case "Active":
                        grant.setAwardStatus(Grant.status.ACTIVE);
                        break;
                    case "Pre-Award":
                        grant.setAwardStatus(Grant.status.PRE_AWARD);
                        break;
                    case "Terminated":
                        grant.setAwardStatus(Grant.status.TERMINATED);
                }
                grant.setPrimaryFunder(rs.getString("PRIME_SPONSOR_CODE"));
                grant.setDirectFunder(rs.getString("SPONSOR"));
                grant.setDepartment(rs.getString("DEPARTMENT"));
                grant.setDivision(rs.getString("DIVISION"));
                grant.setOrganizationalUnitName(rs.getString("UNIT_NAME"));
                grant.setLocalAwardId(rs.getString("AWARD_NUMBER"));

                person.setInstitutionalId(rs.getString("JHED_ID"));
                person.setFirstName(rs.getString("FIRST_NAME"));
                person.setLastName(rs.getString("LAST_NAME"));
                person.setEmail(rs.getString("EMAIL_ADDRESS"));
                //person.setAffiliation(rs.getString("AFFILIATION"));

                grant.setPi(person);
                grantList.add(grant);

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return grantList;
    }

}
