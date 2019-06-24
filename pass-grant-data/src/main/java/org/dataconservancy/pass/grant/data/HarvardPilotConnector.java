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

    private String FUNDER_CSV_FILE_PATH;
    private String GRANT_CSV_FILE_PATH;
    private Properties funderPolicyProperties;

    private static final Logger LOG = LoggerFactory.getLogger(HarvardPilotConnector.class);

    HarvardPilotConnector (String funderCsvFilePath, String grantCsvFilePath, Properties funderPolicyProperties) {
        this.FUNDER_CSV_FILE_PATH = funderCsvFilePath;
        this.GRANT_CSV_FILE_PATH = grantCsvFilePath;
        this.funderPolicyProperties = funderPolicyProperties;
    }

    public String buildQueryString(String startDate, String awardEndDate, String mode) {
        return null;
    }

    public List<Map<String, String>> retrieveUpdates(String queryString, String mode) throws IOException {

        //First associate funder IDs with their names
        Map<String, String> funderNameMap = new HashMap<>();
        try (
                Reader reader = Files.newBufferedReader(Paths.get(FUNDER_CSV_FILE_PATH));
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
                    Reader reader = Files.newBufferedReader(Paths.get(GRANT_CSV_FILE_PATH));
                    CSVParser csvParser = new CSVParser(reader, CSVFormat.RFC4180
                            .withFirstRecordAsHeader()
                            .withIgnoreHeaderCase()
                            .withTrim())
            ) {
                for (CSVRecord csvRecord : csvParser) {
                    LOG.debug("Processing grant csv record ...");
                    String funderLocalKey = csvRecord.get("Funder ID");
                    Map<String, String> rowMap = new HashMap<>();
                    rowMap.put(C_DIRECT_FUNDER_LOCAL_KEY, funderLocalKey);
                    rowMap.put(C_DIRECT_FUNDER_NAME, funderNameMap.get(funderLocalKey));
                    rowMap.put(C_DIRECT_FUNDER_POLICY, funderNameMap.get(funderLocalKey));
                    rowMap.put(C_PRIMARY_FUNDER_LOCAL_KEY, funderLocalKey);
                    rowMap.put(C_PRIMARY_FUNDER_NAME, funderNameMap.get(funderLocalKey));
                    rowMap.put(C_PRIMARY_FUNDER_POLICY, funderPolicyProperties.getProperty(funderLocalKey));
                    rowMap.put(C_GRANT_LOCAL_KEY, csvRecord.get("Harvard grant ID"));
                    rowMap.put(C_GRANT_AWARD_NUMBER, csvRecord.get("Funder grant ID"));
                    rowMap.put(C_GRANT_PROJECT_NAME, csvRecord.get("Grant name"));
                    rowMap.put(C_USER_FIRST_NAME, csvRecord.get("PI First Name"));
                    rowMap.put(C_USER_LAST_NAME, csvRecord.get("PI Last Name"));
                    rowMap.put(C_USER_EMPLOYEE_ID, csvRecord.get("PI Harvard ID"));
                    rowMap.put(C_USER_EMAIL, csvRecord.get("PI Email"));
                    rowMap.put(C_GRANT_START_DATE, csvRecord.get("Grant start date").split(" ")[0]);//just want mm/dd/yyyy
                    rowMap.put(C_GRANT_END_DATE, csvRecord.get("Grant end date").split(" ")[0]);//just want mm/dd/yyyy

                    resultSet.add(rowMap);
                }
            }
        }
        return resultSet;

    }



}
