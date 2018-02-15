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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.StringJoiner;

/**
 * This class connects to a COEUS database via the Oracle JDBC driver
 */
public class HttpCoeusConnector {

    //property names
    private static final String COEUS_URL = "coeus.url";
    private static final String COEUS_USER = "coeus.user";
    private static final String COEUS_PASS = "coeus.pass";

    private String coeusUrl;
    private String coeusUser;
    private String coeusPassword;

    HttpCoeusConnector(Map<String, String> connectionProperties) {
        if (connectionProperties != null) {

            if (connectionProperties.get(COEUS_URL) != null) {
                this.coeusUrl = connectionProperties.get(COEUS_URL);
            }
            if (connectionProperties.get(COEUS_USER) != null) {
                this.coeusUser = connectionProperties.get(COEUS_USER);
            }
            if (connectionProperties.get(COEUS_PASS) != null) {
                this.coeusPassword = connectionProperties.get(COEUS_PASS);
            }
        }
    }

    /**
     * This method returns a {@code ResultSet} for a query for a specific set of fields in several views in COEUS.
     *
     * @param queryString the query string to the COEUS database needed to update the information
     * @return the {@code ResultSet} from the query
     */
    ResultSet retrieveCoeusUpdates(String queryString) {

        ResultSet rs=null;

        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            Connection con = DriverManager.getConnection(coeusUrl, coeusUser, coeusPassword);
            Statement stmt = con.createStatement();
            rs = stmt.executeQuery(queryString);

            while (rs.next()) {
                Grant grant = new Grant();

            }
            con.close();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return rs;
    }

    /**
     * Method for building the query string against the COEUS database. We draw from three views, with the PRSN view
     * (aliased to B) serving just to contain keys for joining the PROP (alias A) and DETAIL (alias C) views.
     *
     * Since dates are stored in the views as strings, we need to process these strings using the TO_DATE function
     * String literals are protected by single quotes. This explains some of the strangeness in the query string
     * construction.
     *
     * Normally, we will pull for just one date - in this case, startDate and endDate will be the same. Dates must be
     * specified as mm/dd/yyyy. Dates are expected to be valid as supplied.
     *
     * If no dates are specified, we assume that this is part of a daily run to get the updates from the previous day,
     * so start date and end date will be yesterday.
     * If only the start date is specified, we take the end date as being yesterday.
     * If only the end date is specified, we throw an exception, as there is no natural default for start date.
     *
     * @param startDate - the date we want to start the query against LAST_MODIFIED
     * @param endDate - the date we want to end the query against LAST_MODIFIED
     * @return the SQL query string
     */
    String buildQueryString(String startDate, String endDate){

        //TODO handle dates to agree with javadoc on method

        String[] propViewFields = { "TITLE", "GRANT_NUMBER", "AWARD_DATE", "AWARD_END",
                "AWARD_NUMBER", "AWARD_STATUS", "PRIME_SPONSOR_CODE", "SPONSOR", "SPOSNOR_CODE", "DEPARTMENT",
                "DIVISION", "UNIT_NAME", "UNIT_NUMBER", "INST_PROPOSAL"};

        StringJoiner propViewQuery = new StringJoiner(", A.", "A.", ", ");
        for (String field : propViewFields){
            propViewQuery.add(field);
        }

        String[] personDetailViewFields = {"JHED_ID", "FIRST_NAME", "LAST_NAME", "EMAIL_ADDRESS"};

        StringJoiner personDetailViewQuery = new StringJoiner(", C.", "C.", "");
        for (String field : personDetailViewFields){
            personDetailViewQuery.add(field);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ");
        sb.append(propViewQuery.toString());
        sb.append(personDetailViewQuery.toString());
        sb.append(" FROM COEUS.JHU_FACULTY_FORCE_PROP A ");
        sb.append("INNER JOIN COEUS.JHU_FACULTY_FORCE_PRSN B ON A.INST_PROPOSAL = B.INST_PROPOSAL ");
        sb.append("INNER JOIN COEUS.JHU_FACULTY_FORCE_PRSN_DETAIL C ON B.JHED_ID = C.JHED_ID ");
        //TODO change to comparing A.LAST_MODIFIED when available. This is just for testing purposes
        sb.append("WHERE (TO_DATE(A.AWARD_DATE, 'mm/dd/yyyy') BETWEEN TO_DATE('");
        sb.append(startDate);
        sb.append("', 'mm/dd/yyyy') AND TO_DATE('");
        sb.append(endDate);
        sb.append("', 'mm/dd/yyyy')) AND A.PROPOSAL_STATUS = 'Funded'");

        return sb.toString();
    }

}
