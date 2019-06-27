package org.dataconservancy.pass.grant.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.dataconservancy.pass.grant.data.CoeusFieldNames.*;

public class HarvardPilotConnector implements GrantConnector {


    private static final String HARVARD_DATA_FILE_PATH_PROPERTY = "harvard.data.file.path";

    private String xlsxDataFilePath;
    private Properties funderPolicyProperties;

    private static final Logger LOG = LoggerFactory.getLogger(HarvardPilotConnector.class);

    public HarvardPilotConnector(Properties connectionProperties, Properties funderPolicyProperties) {
        if (connectionProperties.getProperty(HARVARD_DATA_FILE_PATH_PROPERTY) != null) {
            this.xlsxDataFilePath = connectionProperties.getProperty(HARVARD_DATA_FILE_PATH_PROPERTY);
        }
        this.funderPolicyProperties = funderPolicyProperties;
    }

    public String buildQueryString(String startDate, String awardEndDate, String mode) {
        return null;
    }

    public List<Map<String, String>> retrieveUpdates(String queryString, String mode) throws IOException {

        Sheet grantSheet;
        //First associate funder IDs with their names
        Map<String, String> funderNameMap = new HashMap<>();
        try (FileInputStream excelFile = new FileInputStream(new File(xlsxDataFilePath))
        ) {

            XSSFWorkbook workbook = new XSSFWorkbook(excelFile);
            Sheet funderSheet = workbook.getSheetAt(1);
            for (Row cells : funderSheet) {
                if (cells.getRowNum() > 0) {//skip header
                    funderNameMap.put(stringify(cells.getCell(0)),
                            stringify(cells.getCell(1)));
                }
            }
            grantSheet = workbook.getSheetAt(0);
        }

        List<Map<String, String>> resultSet = new ArrayList<>();

        if (mode.equals("funder")) {

            for (Object localKey : funderNameMap.keySet()) {
                Map<String, String> rowMap = new HashMap<>();
                rowMap.put(C_PRIMARY_FUNDER_LOCAL_KEY, localKey.toString());
                rowMap.put(C_PRIMARY_FUNDER_NAME, funderNameMap.get(localKey.toString()));
                if (funderPolicyProperties.keySet().contains(localKey)) {
                    rowMap.put(C_PRIMARY_FUNDER_POLICY, funderPolicyProperties.getProperty(localKey.toString()));
                }
                resultSet.add(rowMap);
            }

        } else {//"grant" mode is default
            for (Row cells : grantSheet) {
                if (cells.getRowNum() > 0) {//skip header
                    LOG.debug("Processing grant  record ...");
                    String funderLocalKey = stringify(cells.getCell(7)); //H: Funder ID
                    Map<String, String> rowMap = new HashMap<>();
                    if (funderLocalKey != null) {
                        rowMap.put(C_DIRECT_FUNDER_LOCAL_KEY, funderLocalKey);
                        rowMap.put(C_DIRECT_FUNDER_NAME, funderNameMap.get(funderLocalKey));
                        rowMap.put(C_DIRECT_FUNDER_POLICY, funderPolicyProperties.getProperty(funderLocalKey));
                        rowMap.put(C_PRIMARY_FUNDER_LOCAL_KEY, funderLocalKey);
                        rowMap.put(C_PRIMARY_FUNDER_NAME, funderNameMap.get(funderLocalKey));
                        rowMap.put(C_PRIMARY_FUNDER_POLICY, funderPolicyProperties.getProperty(funderLocalKey));
                    }
                    rowMap.put(C_GRANT_LOCAL_KEY, stringify(cells.getCell(0))); //A: Harvard grant ID
                    rowMap.put(C_GRANT_AWARD_NUMBER, stringify(cells.getCell(1))); //B: Funder grant ID
                    rowMap.put(C_GRANT_PROJECT_NAME, stringify(cells.getCell(2))); //C: Grant Name
                    rowMap.put(C_USER_FIRST_NAME, stringify(cells.getCell(3))); //D: PI First Name
                    rowMap.put(C_USER_LAST_NAME, stringify(cells.getCell(4))); //E: PI Last Name
                    rowMap.put(C_USER_EMPLOYEE_ID, stringify(cells.getCell(5))); //F: PI Harvard ID
                    rowMap.put(C_USER_EMAIL, stringify(cells.getCell(6))); //G: PI Email
                    rowMap.put(C_GRANT_START_DATE, stringifyDate(cells.getCell(8))); //I: Grant Start Date
                    rowMap.put(C_GRANT_END_DATE, stringifyDate(cells.getCell(9))); //J: Grant End Date
                    rowMap.put(C_ABBREVIATED_ROLE, "P"); //for now, until we get this data , everyone's a PI
                    resultSet.add(rowMap);
                }
            }
        }

        return resultSet;

    }

    private String stringify(Cell cell) {
        if (cell.getCellType().equals(CellType.STRING)) {
            return cell.getStringCellValue().trim();
        } else if (cell.getCellType().equals(CellType.NUMERIC)) {
            return String.valueOf((int) cell.getNumericCellValue());//it's an integer
        }
        return null;
    }

    private String stringifyDate(Cell cell) {
        String pattern = "MM/dd/yyyy";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        return simpleDateFormat.format(cell.getDateCellValue());
    }

}
