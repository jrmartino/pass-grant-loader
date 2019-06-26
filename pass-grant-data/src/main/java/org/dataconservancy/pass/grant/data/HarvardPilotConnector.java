package org.dataconservancy.pass.grant.data;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.dataconservancy.pass.grant.data.CoeusFieldNames.*;

public class HarvardPilotConnector implements GrantConnector {

    private static final String H_FUNDER_ID = "Funder ID";//harvard's local key
    private static final String H_GRANT_ID = "Harvard grant ID";// harvard's local key
    private static final String H_FUNDER_GRANT_ID = "Funder grant ID";//ID assigned by funder
    private static final String H_GRANT_NAME = "Grant name";
    private static final String H_INV_FIRST_NAME = "PI First Name";
    private static final String H_INV_LAST_NAME ="PI Last Name";
    private static final String H_INV_ID = "PI Harvard ID";//guaranteed to be in grant data
    private static final String H_INV_EMAIL = "PI Email";
    //private static final String H_INV_ROLE =
    private static final String H_GRANT_START_DATE = "Grant start date";
    private static final String H_GRANT_END_DATE = "Grant end date";

    private static final String GRANT_FILE_PATH_PROPERTY = "grant.file.path";
    private static final String FUNDER_FILE_PATH_PROPERTY = "funder.file.path";
    private String funderCsvFilePath;
    private String grantCsvFilePath;
    private Properties funderPolicyProperties;

    private static final Logger LOG = LoggerFactory.getLogger(HarvardPilotConnector.class);

    public HarvardPilotConnector (Properties connectionProperties, Properties funderPolicyProperties) {
        if (connectionProperties.getProperty(GRANT_FILE_PATH_PROPERTY) != null) {
            this.grantCsvFilePath = connectionProperties.getProperty(GRANT_FILE_PATH_PROPERTY);
        }
        if (connectionProperties.getProperty(FUNDER_FILE_PATH_PROPERTY) != null) {
            this.funderCsvFilePath = connectionProperties.getProperty(FUNDER_FILE_PATH_PROPERTY);
        }
        this.funderPolicyProperties = funderPolicyProperties;
    }

    public String buildQueryString(String startDate, String awardEndDate, String mode) {
        return null;
    }

    public List<Map<String, String>> retrieveUpdates(String queryString, String mode) throws IOException {

        //First associate funder IDs with their names
        Map<String, String> funderNameMap = new HashMap<>();
        try (
                Reader reader = Files.newBufferedReader(Paths.get(funderCsvFilePath));
                CSVParser csvParser = new CSVParser(reader, CSVFormat.RFC4180
                        .withFirstRecordAsHeader()
                        .withIgnoreHeaderCase()
                        .withTrim())
        ) {
            for (CSVRecord csvRecord : csvParser) {
                LOG.debug("Processing funder csv record ...");
                funderNameMap.put(csvRecord.get("Funder ID"), csvRecord.get("Funder Name"));
            }

        }

        List<Map<String, String>> resultSet = new ArrayList<>();

        if (mode.equals("funder")) {

            for (Object localKey : funderPolicyProperties.keySet()) {
                Map<String, String> rowMap = new HashMap<>();
                rowMap.put(C_PRIMARY_FUNDER_LOCAL_KEY, localKey.toString());
                rowMap.put(C_PRIMARY_FUNDER_NAME, funderNameMap.get(localKey.toString()));
                rowMap.put(C_PRIMARY_FUNDER_POLICY, funderPolicyProperties.getProperty(localKey.toString()));
                resultSet.add(rowMap);
            }

        } else {//"grant" mode is default

            try (
                    Reader reader = Files.newBufferedReader(Paths.get(grantCsvFilePath));
                    CSVParser csvParser = new CSVParser(reader, CSVFormat.RFC4180
                            .withFirstRecordAsHeader()
                            .withIgnoreHeaderCase()
                            .withTrim())
            ) {
                for (CSVRecord csvRecord : csvParser) {
                    LOG.debug("Processing grant csv record ...");
                    String funderLocalKey = csvRecord.get(H_FUNDER_ID);
                    Map<String, String> rowMap = new HashMap<>();
                    rowMap.put(C_DIRECT_FUNDER_LOCAL_KEY, funderLocalKey);
                    rowMap.put(C_DIRECT_FUNDER_NAME, funderNameMap.get(funderLocalKey));
                    rowMap.put(C_DIRECT_FUNDER_POLICY, funderNameMap.get(funderLocalKey));
                    rowMap.put(C_PRIMARY_FUNDER_LOCAL_KEY, funderLocalKey);
                    rowMap.put(C_PRIMARY_FUNDER_NAME, funderNameMap.get(funderLocalKey));
                    rowMap.put(C_PRIMARY_FUNDER_POLICY, funderPolicyProperties.getProperty(funderLocalKey));
                    rowMap.put(C_GRANT_LOCAL_KEY, csvRecord.get(H_GRANT_ID));
                    rowMap.put(C_GRANT_AWARD_NUMBER, csvRecord.get(H_FUNDER_GRANT_ID));
                    rowMap.put(C_GRANT_PROJECT_NAME, csvRecord.get(H_GRANT_NAME));
                    rowMap.put(C_USER_FIRST_NAME, csvRecord.get(H_INV_FIRST_NAME));
                    rowMap.put(C_USER_LAST_NAME, csvRecord.get(H_INV_LAST_NAME));
                    rowMap.put(C_USER_EMPLOYEE_ID, csvRecord.get(H_INV_ID));
                    rowMap.put(C_USER_EMAIL, csvRecord.get(H_INV_EMAIL));
                    rowMap.put(C_GRANT_START_DATE, csvRecord.get(H_GRANT_START_DATE).split(" ")[0]);//just want mm/dd/yyyy
                    rowMap.put(C_GRANT_END_DATE, csvRecord.get(H_GRANT_END_DATE).split(" ")[0]);//just want mm/dd/yyyy

                    resultSet.add(rowMap);
                }
            }
        }
        return resultSet;

    }



}
