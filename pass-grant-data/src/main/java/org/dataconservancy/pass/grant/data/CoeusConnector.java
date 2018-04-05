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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;


import static org.dataconservancy.pass.grant.data.CoeusFieldNames.*;

/**
 * This class connects to a COEUS database via the Oracle JDBC driver. The query string reflects local JHU
 * database views
 *
 * @author jrm@jhu.edu
 */
public class CoeusConnector {
    private static Logger LOG = LoggerFactory.getLogger(CoeusConnector.class);
    //property names
    private static final String COEUS_URL = "coeus.url";
    private static final String COEUS_USER = "coeus.user";
    private static final String COEUS_PASS = "coeus.pass";

    private String coeusUrl;
    private String coeusUser;
    private String coeusPassword;

    public CoeusConnector(Properties connectionProperties) {
        if (connectionProperties != null) {

            if (connectionProperties.getProperty(COEUS_URL) != null) {
                this.coeusUrl = connectionProperties.getProperty(COEUS_URL);
            }
            if (connectionProperties.getProperty(COEUS_USER) != null) {
                this.coeusUser = connectionProperties.getProperty(COEUS_USER);
            }
            if (connectionProperties.getProperty(COEUS_PASS) != null) {
                this.coeusPassword = connectionProperties.getProperty(COEUS_PASS);
            }
        }
    }

    /**
     * This method returns a {@code ResultSet} for a query for a specific set of fields in several views in COEUS.
     *
     * @param queryString the query string to the COEUS database needed to update the information
     * @return the {@code ResultSet} from the query
     */
    public Set<Map<String, String>> retrieveCoeusUpdates(String queryString) throws ClassNotFoundException, SQLException {

        Set<Map<String, String>> mapSet = new HashSet<>();

        Class.forName("oracle.jdbc.driver.OracleDriver");

        try (
                Connection con = DriverManager.getConnection(coeusUrl, coeusUser, coeusPassword);
                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery(queryString)
        ) {
            while (rs.next()) {
                Map<String, String> rowMap = new HashMap<>();

                rowMap.put(C_GRANT_AWARD_NUMBER, rs.getString(C_GRANT_AWARD_NUMBER));
                rowMap.put(C_GRANT_AWARD_STATUS, rs.getString(C_GRANT_AWARD_STATUS));
                rowMap.put(C_GRANT_LOCAL_AWARD_ID, rs.getString(C_GRANT_LOCAL_AWARD_ID));
                rowMap.put(C_GRANT_PROJECT_NAME, rs.getString(C_GRANT_PROJECT_NAME));
                rowMap.put(C_GRANT_AWARD_DATE, rs.getString(C_GRANT_AWARD_DATE));
                rowMap.put(C_GRANT_START_DATE, rs.getString(C_GRANT_START_DATE));
                rowMap.put(C_GRANT_END_DATE, rs.getString(C_GRANT_END_DATE));
                rowMap.put(C_DIRECT_FUNDER_LOCAL_ID, rs.getString(C_DIRECT_FUNDER_LOCAL_ID));
                rowMap.put(C_DIRECT_FUNDER_NAME, rs.getString(C_DIRECT_FUNDER_NAME));
                rowMap.put(C_PRIMARY_FUNDER_LOCAL_ID, rs.getString(C_PRIMARY_FUNDER_LOCAL_ID));
                rowMap.put(C_PRIMARY_FUNDER_NAME, rs.getString(C_PRIMARY_FUNDER_NAME));
                rowMap.put(C_PERSON_FIRST_NAME, rs.getString(C_PERSON_FIRST_NAME));
                rowMap.put(C_PERSON_MIDDLE_NAME, rs.getString(C_PERSON_MIDDLE_NAME));
                rowMap.put(C_PERSON_LAST_NAME, rs.getString(C_PERSON_LAST_NAME));
                rowMap.put(C_PERSON_EMAIL, rs.getString(C_PERSON_EMAIL));
                rowMap.put(C_PERSON_INSTITUTIONAL_ID, rs.getString(C_PERSON_INSTITUTIONAL_ID));
                rowMap.put(C_UPDATE_TIMESTAMP, rs.getString(C_UPDATE_TIMESTAMP));
                rowMap.put(C_ABBREVIATED_ROLE, rs.getString(C_ABBREVIATED_ROLE));
                LOG.debug("Record processed: " + rowMap.toString());
                mapSet.add(rowMap);
            }
        }
        System.out.println(mapSet.size());
        LOG.info("Retrieved result set from COEUS: " + mapSet.size() + " records processed");
        return mapSet;
    }

    /**
     * Method for building the query string against the COEUS database. We draw from four views.
     * Dates are stored in the views as strings, except for the UPDATE_TIMESTAMP, which is a timestamp.
     * We will pull all records which have been updated since the last update timestamp - this value becomes out startDate.
     *
     * Because we are only interested in the latest update for any grant number, we restrict the search to the latest
     * update timestamp for grants, even if these correspond to different institutional proposal numbers. This is because we
     * only need the granularity of grant number for the purposes of publication submission.
     *
     * NB: the join of the PROP view with the PRSN view will result in one row in the ResultSet for each investigator
     * on the grant. if there are co-pis in addition to a pi, there will be multiple rows.

     * COEUS.JHU_FACULTY_FORCE_PROP aliased to A
     * COEUS.JHU_FACULTY_FORCE_PRSN aliased to B
     * COEUS.JHU_FACULTY_FORCE_PRSN_DETAIL aliased to C
     * COEUS.SWIFT_SPONSOR aliased to D
     *
     * @param startDate - the date we want to start the query against UPDATE_TIMESTAMP
     * @return the SQL query string
     */
    public String buildQueryString(String startDate){

        String[] viewFields = {
                "A." + C_GRANT_AWARD_NUMBER,
                "A." + C_GRANT_AWARD_STATUS,
                "A." + C_GRANT_LOCAL_AWARD_ID,
                "A." + C_GRANT_PROJECT_NAME,
                "A." + C_GRANT_AWARD_DATE,
                "A." + C_GRANT_START_DATE,
                "A." + C_GRANT_END_DATE,
                "A." + C_DIRECT_FUNDER_NAME,
                "A." + C_DIRECT_FUNDER_LOCAL_ID, //"SPOSNOR_CODE"
                "A." + C_UPDATE_TIMESTAMP,

                "B." + C_ABBREVIATED_ROLE,

                "C." + C_PERSON_FIRST_NAME,
                "C." + C_PERSON_MIDDLE_NAME,
                "C." + C_PERSON_LAST_NAME,
                "C." + C_PERSON_EMAIL,
                "C." + C_PERSON_INSTITUTIONAL_ID,

                "D." + C_PRIMARY_FUNDER_NAME,
                "D." + C_PRIMARY_FUNDER_LOCAL_ID };

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ");
        sb.append(String.join(", ",viewFields));
        sb.append(" FROM");
        sb.append(" COEUS.JHU_FACULTY_FORCE_PROP A");
        sb.append(" INNER JOIN ");
        sb.append(" (SELECT GRANT_NUMBER, MAX(UPDATE_TIMESTAMP) AS MAX_UPDATE_TIMESTAMP");
        sb.append(" FROM COEUS.JHU_FACULTY_FORCE_PROP GROUP BY GRANT_NUMBER) LATEST");
        sb.append(" ON A.UPDATE_TIMESTAMP = LATEST.MAX_UPDATE_TIMESTAMP");
        sb.append(" AND A.GRANT_NUMBER = LATEST.GRANT_NUMBER");
        sb.append(" INNER JOIN COEUS.JHU_FACULTY_FORCE_PRSN B ON A.INST_PROPOSAL = B.INST_PROPOSAL");
        sb.append(" INNER JOIN COEUS.JHU_FACULTY_FORCE_PRSN_DETAIL C ON B.JHED_ID = C.JHED_ID");
        sb.append(" LEFT JOIN COEUS.SWIFT_SPONSOR D ON A.PRIME_SPONSOR_CODE = D.SPONSOR_CODE");
        sb.append(" WHERE A.UPDATE_TIMESTAMP > TIMESTAMP '");
        sb.append(startDate);
        sb.append("' ");
        sb.append("AND (A.AWARD_STATUS = 'Active' OR A.AWARD_STATUS = 'Terminated') ");
        sb.append("AND (B.ABBREVIATED_ROLE = 'P' OR B.ABBREVIATED_ROLE = 'C') ");
        sb.append("AND A.GRANT_NUMBER IS NOT NULL");


        String queryString = sb.toString();

        LOG.debug("Query string is: " + queryString);
        return queryString;
    }

}