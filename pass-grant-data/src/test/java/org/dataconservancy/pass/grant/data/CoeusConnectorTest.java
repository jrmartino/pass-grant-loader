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

import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for the COEUS connector
 *
 * @author jrm@jhu.edu
 */
public class CoeusConnectorTest {

    @Test
    public void testCoeusConnector(){

    }

    /**
     * Test that the query string produces is as expected
     */
    @Test
    public void testBuildString() {

        /*String expectedQueryString = "SELECT AWARD_ID, AWARD_STATUS, GRANT_NUMBER, TITLE, AWARD_DATE, AWARD_START," +
                " AWARD_END, SPONSOR, SPOSNOR_CODE, UPDATE_TIMESTAMP, ABBREVIATED_ROLE, FIRST_NAME, MIDDLE_NAME," +
                " LAST_NAME, EMAIL_ADDRESS, JHED_ID, SPONSOR_NAME, SPONSOR_CODE " +
                "FROM COEUS.JHU_FACULTY_FORCE_PROP A INNER JOIN COEUS.JHU_FACULTY_FORCE_PRSN B " +
                "ON A.INST_PROPOSAL = B.INST_PROPOSAL INNER JOIN COEUS.JHU_FACULTY_FORCE_PRSN_DETAIL C O" +
                "N B.JHED_ID = C.JHED_ID LEFT JOIN COEUS.SWIFT_SPONSOR D " +
                "ON A.PRIME_SPONSOR_CODE = D.SPONSOR_CODE " +
                "WHERE A.UPDATE_TIMESTAMP > TIMESTAMP '2018-13-14 06:00:00.0' " +
                "AND (A.AWARD_STATUS = 'Active' OR A.AWARD_STATUS = 'Terminated')";
*/
        String expectedQueryString= "SELECT A.AWARD_ID, A.AWARD_STATUS, A.GRANT_NUMBER, A.TITLE, A.AWARD_DATE," +
                " A.AWARD_START, A.AWARD_END, A.SPONSOR, A.SPOSNOR_CODE, A.UPDATE_TIMESTAMP, B.ABBREVIATED_ROLE," +
                " C.FIRST_NAME, C.MIDDLE_NAME, C.LAST_NAME, C.EMAIL_ADDRESS, C.JHED_ID, D.SPONSOR_NAME, D.SPONSOR_CODE" +
                " FROM" +
                " COEUS.JHU_FACULTY_FORCE_PROP A INNER JOIN " +
                " (SELECT GRANT_NUMBER, MAX(UPDATE_TIMESTAMP) AS MAX_UPDATE_TIMESTAMP" +
                    " FROM COEUS.JHU_FACULTY_FORCE_PROP" +
                    " GROUP BY GRANT_NUMBER) LATEST" +
                " ON A.UPDATE_TIMESTAMP = LATEST.MAX_UPDATE_TIMESTAMP AND A.GRANT_NUMBER = LATEST.GRANT_NUMBER" +
                " INNER JOIN COEUS.JHU_FACULTY_FORCE_PRSN B" +
                " ON A.INST_PROPOSAL = B.INST_PROPOSAL" +
                " INNER JOIN COEUS.JHU_FACULTY_FORCE_PRSN_DETAIL C" +
                " ON B.JHED_ID = C.JHED_ID" +
                " LEFT JOIN COEUS.SWIFT_SPONSOR D" +
                " ON A.PRIME_SPONSOR_CODE = D.SPONSOR_CODE" +
                " WHERE A.UPDATE_TIMESTAMP > TIMESTAMP '2018-13-14 06:00:00.0'" +
                " AND (A.AWARD_STATUS = 'Active' OR A.AWARD_STATUS = 'Terminated')" +
                " AND (B.ABBREVIATED_ROLE = 'P' OR B.ABBREVIATED_ROLE = 'C')" +
                " AND A.GRANT_NUMBER IS NOT NULL";


        CoeusConnector connector = new CoeusConnector(null);
        Assert.assertEquals(expectedQueryString, connector.buildQueryString("2018-13-14 06:00:00.0"));

    }

}
