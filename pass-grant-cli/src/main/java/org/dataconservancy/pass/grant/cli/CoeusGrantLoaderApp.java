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

package org.dataconservancy.pass.grant.cli;

import org.apache.commons.codec.binary.Base64InputStream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import org.dataconservancy.pass.grant.data.CoeusConnector;
import org.dataconservancy.pass.grant.data.DateTimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class CoeusGrantLoaderApp {
    private static Logger LOG = LoggerFactory.getLogger(CoeusGrantLoaderApp.class);
    private static String ERR_HOME_DIRECTORY_NOT_FOUND = "No home directory found for the application. Please specify a valid absolute path.";
    private static String ERR_HOME_DIRECTORY_NOT_READABLE_AND_WRITABLE = "Supplied home directory must be readable" +
            " and writable by the user running this application.";
    private static String ERR_REQUIRED_CONFIGURATION_FILE_MISSING = "Required file %s is missing in the specified home directory.";
    private static String ERR_COULD_NOT_OPEN_CONFIGURATION_FILE = "Could not open configuration file";
    private static String ERR_INVALID_COMMAND_LINE_TIMESTAMP = "An invalid timestamp was specified on the command line: %s. Please make sure it" +
            "is of the form 'yyyy-mm-dd hh:mm:ss.m{mm}";
    private static String ERR_INVALID_TIMESTAMP = "An invalid timestamp was found at the last line of the update timestamp file. Please make sure it" +
            "is of the form 'yyyy-mm-dd hh:mm:ss.m{mm}";

    private static String connectionPropertiesFileName= "connection.properties";
    private static String mailPropertiesFileName = "mail.properties";
    private static String updateTimestampsFileName = "update_timestamps";

    private EmailService emailService;
    private CoeusConnector coeusConnector;

    private File appHome;
    private String startDate;
    private File connectionPropertiesFile;
    private File mailPropertiesFile;
    private File updateTimestampsFile;


    public CoeusGrantLoaderApp(File coeusLoaderHome, String startDate) {
        this.appHome = coeusLoaderHome;
        this.startDate = startDate;
        }

    void run() throws CoeusCliException {
        connectionPropertiesFile = new File(appHome, connectionPropertiesFileName);
        mailPropertiesFile = new File(appHome, mailPropertiesFileName);
        updateTimestampsFile = new File(appHome, updateTimestampsFileName);
        Map<String, String> connectionPropertiesMap;
        Properties mailProperties;

        //first check that we have the minimum required files
        checkFilesAreOK(appHome);

        //now define our email service and build our database connection info
        //from the properties files
        try {
            connectionPropertiesMap = decodeProperties(connectionPropertiesFile);
            mailProperties=loadProperties(mailPropertiesFile);
            emailService = new EmailService(mailProperties);
        } catch (IOException | RuntimeException e) {
            LOG.error(ERR_COULD_NOT_OPEN_CONFIGURATION_FILE, e.getMessage());
            emailService.sendEmailMessage(ERR_COULD_NOT_OPEN_CONFIGURATION_FILE + e.getMessage());
            throw new CoeusCliException(ERR_COULD_NOT_OPEN_CONFIGURATION_FILE, e);
        }

        //establish the start dateTime - it is either given as an option, or it is
        //the last entry in the update_timestamps file
        String queryString = null;

        if(startDate.length()>0 && !DateTimeUtil.verifyDateTimeFormat(startDate)) {//we have a startDate specified on the command line
               LOG.error(format(ERR_INVALID_COMMAND_LINE_TIMESTAMP, startDate));
               emailService.sendEmailMessage(format(ERR_INVALID_COMMAND_LINE_TIMESTAMP, startDate));
               throw new CoeusCliException(format(ERR_INVALID_COMMAND_LINE_TIMESTAMP, startDate));
        } else {
            startDate = getLatestTimestamp();
        }

        if (!DateTimeUtil.verifyDateTimeFormat(startDate)){
            LOG.error(format(ERR_INVALID_TIMESTAMP, startDate));
            emailService.sendEmailMessage(format(ERR_INVALID_TIMESTAMP, startDate));
            throw new CoeusCliException(format(ERR_INVALID_TIMESTAMP, startDate));
        }

        //now do things;

        coeusConnector = new CoeusConnector(connectionPropertiesMap);
        queryString = coeusConnector.buildQueryString(startDate);



    }

    private void checkFilesAreOK(File appHome) throws CoeusCliException {
        //First make sure the specified home directory exists and is suitable
        if (!appHome.exists()) {
            LOG.error(ERR_HOME_DIRECTORY_NOT_FOUND);
            emailService.sendEmailMessage(ERR_HOME_DIRECTORY_NOT_FOUND);
            throw new CoeusCliException(ERR_HOME_DIRECTORY_NOT_FOUND);
        }
        if (!appHome.canRead() || !appHome.canWrite()) {
            LOG.error(ERR_HOME_DIRECTORY_NOT_READABLE_AND_WRITABLE);
            emailService.sendEmailMessage(ERR_HOME_DIRECTORY_NOT_READABLE_AND_WRITABLE);
            throw new CoeusCliException(ERR_HOME_DIRECTORY_NOT_READABLE_AND_WRITABLE);
        }

        // and also the required properties files
        if (!connectionPropertiesFile.exists()) {
            LOG.error(format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, connectionPropertiesFileName));
            emailService.sendEmailMessage(format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, connectionPropertiesFileName));
            throw new CoeusCliException(format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, connectionPropertiesFileName));
        }
        if (!mailPropertiesFile.exists()) {
            LOG.error(format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, mailPropertiesFileName));
            emailService.sendEmailMessage(format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, mailPropertiesFileName));
            throw new CoeusCliException(format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, mailPropertiesFileName));
        }
    }

    private Map<String, String> decodeProperties(File propertiesFile) throws IOException, RuntimeException {

        String resource = propertiesFile.getCanonicalPath();
        InputStream resourceStream = this.getClass().getResourceAsStream(resource);

        if (resourceStream == null) {
            LOG.error(format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, resource));
            emailService.sendEmailMessage(format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, resource));
            throw new RuntimeException(new CoeusCliException(
                    format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, resource)));
        }

        Properties connectionProperties = new Properties();
        connectionProperties.load(new Base64InputStream(resourceStream));

        @SuppressWarnings("unchecked")
        Map<String, String> connectionMap =  (Map) connectionProperties;
        //check types to be safe - if there's a problem, we will catch it here
        for(String k : connectionMap.keySet());
        for(String v : connectionMap.values());

        return connectionMap;
        //return ((Map<String, String>) (Map) connectionProperties);
    }

    private Properties loadProperties(File propertiesFile) throws IOException, RuntimeException {

        String resource = propertiesFile.getCanonicalPath();
        InputStream resourceStream = this.getClass().getResourceAsStream(resource);

        if (resourceStream == null) {
            emailService.sendEmailMessage(format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, resource));
            LOG.error(format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, resource));
            throw new RuntimeException(new CoeusCliException(
                    format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, resource)));
        }

        Properties properties = new Properties();
        properties.load(resourceStream);

        return properties;
    }

    private String getLatestTimestamp() throws CoeusCliException {
        String lastLine="";
        if (!updateTimestampsFile.exists()) {
            LOG.error(format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, updateTimestampsFileName));
            emailService.sendEmailMessage(format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, updateTimestampsFileName));
            throw new CoeusCliException(format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, updateTimestampsFileName));
        } else {
            //TODO open timestamp file and read the last line; assign it to startDate, check validity

            try {
                BufferedReader br = new BufferedReader(new FileReader(updateTimestampsFile));
                String readLine;
                while ((readLine = br.readLine()) != null) {
                    lastLine=readLine;
                }
            } catch (IOException e) {
                LOG.error(ERR_COULD_NOT_OPEN_CONFIGURATION_FILE, e.getMessage());
                emailService.sendEmailMessage(ERR_COULD_NOT_OPEN_CONFIGURATION_FILE + e.getMessage());
                throw new CoeusCliException(ERR_COULD_NOT_OPEN_CONFIGURATION_FILE, e);
            }
            lastLine = lastLine.replaceAll("\\r|\\n","");
        }
        return lastLine;
    }

}
