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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import org.dataconservancy.pass.grant.data.CoeusConnector;
import org.dataconservancy.pass.grant.data.GrantUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static org.dataconservancy.pass.grant.data.DateTimeUtil.verifyDateTimeFormat;

/**
 * This class does the orchestration for the pulling of COEUS grant data. The basic steeps are to read in all of the configuration
 * files needed by the various classes; construct the query string for the COEUS Oracle DB to pull in all of the grants updated since the
 * timestamp at the end of the updated timestamps file; execute the query against this database; use the {@code ResultSet} to populate
 * a list of {@code Grant}s in our java model; and finally to push this data into our Fedora instance via the java Fedora client.
 *
 * A large percentage of the code here is handling exceptional paths, as this is intended to be run in an automated fashion, so
 * care must be taken to log errors, report them to STDOUT, and also send email notifications.
 *
 * @author jrm@jhu.edu
 */
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
    private static String ERR_COULD_NOT_APPEND_UPDATE_TIMESTAMP = "The updated succeeded, but could not append last modified date %s to update timestamp file";
    private static String ERR_SQL_EXCEPTION = "An SQL error occurred querying the COEUS database";
    private static String ERR_ORACLE_DRIVER_NOT_FOUND = "Could not find the oracle db driver on classpath.";

    private static String connectionPropertiesFileName= "connection.properties";
    private static String mailPropertiesFileName = "mail.properties";
    private static String updateTimestampsFileName = "update_timestamps";

    private EmailService emailService;

    private File appHome;
    private String startDate;
    private File connectionPropertiesFile;
    private File mailPropertiesFile;
    private File updateTimestampsFile;

    /**
     * Constructor for this class
     * @param coeusLoaderHome - the full path for the directory where configuration file, and the file containing all of the
     *                        timestamps for updates which have previously been run.
     *
     * @param startDate - the latest successful update timestamp, occurring as the last line of the update timestamps file
     */
    CoeusGrantLoaderApp(File coeusLoaderHome, String startDate) {
        this.appHome = coeusLoaderHome;
        this.startDate = startDate;
        }

    /**
     * The orchestration method for everything. This is called by the {@code CoeusGrantLoaderCLI}, which only manages the
     * command line interaction.
     *
     * @throws CoeusCliException if there was any error occurring during the grant loading or updating processes
     */
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
        } catch (RuntimeException e) {
            throw processException(ERR_COULD_NOT_OPEN_CONFIGURATION_FILE,e);
        }

        //establish the start dateTime - it is either given as an option, or it is
        //the last entry in the update_timestamps file
        if(startDate.length()>0 && !verifyDateTimeFormat(startDate)) {//we have a startDate specified on the command line
            throw processException(format(ERR_INVALID_COMMAND_LINE_TIMESTAMP, startDate),null);
        } else {
            startDate = getLatestTimestamp();
        }

        //we need a valid dateTime to start the query
        if (!verifyDateTimeFormat(startDate)){//the start timestamp file is not in valid date time format
            throw processException(format(ERR_INVALID_TIMESTAMP, startDate), null);
        }

        //now do things;
        CoeusConnector coeusConnector = new CoeusConnector(connectionPropertiesMap);
        String queryString = coeusConnector.buildQueryString(startDate);
        ResultSet resultSet;
        GrantUpdater modelBuilder = null;
        try {
            resultSet = coeusConnector.retrieveCoeusUpdates(queryString);
            modelBuilder = new GrantUpdater(resultSet);
            modelBuilder.updateGrants();

        } catch (ClassNotFoundException e) {
            throw processException(ERR_ORACLE_DRIVER_NOT_FOUND, e);
        } catch (SQLException e) {
            throw processException(ERR_SQL_EXCEPTION, e);
        }

        //apparently the hard part has succeeded, let's write the timestamp to out update timestamps file
        try{
            appendLineToFile(updateTimestampsFile, modelBuilder.getLatestUpdate());
        } catch (IOException e) {
            throw processException(format(ERR_COULD_NOT_APPEND_UPDATE_TIMESTAMP, modelBuilder.getLatestUpdate()), null);
        }

        //now everything succeeded - log this result and send email
        String message = modelBuilder.getReport();
        LOG.info(message);
        emailService.sendEmailMessage("COEUS Data Loader SUCCESS", message);
    }

    /**
     * This method checks to make sure we can access the necessary configuration files
     *
     * @param appHome - the full path to the directory containing the configuration files and update timestamps file
     * @throws CoeusCliException if the configuration files are not present and accessible
     */
    private void checkFilesAreOK(File appHome) throws CoeusCliException {
        //First make sure the specified home directory exists and is suitable
        if (!appHome.exists()) {
            throw processException(ERR_HOME_DIRECTORY_NOT_FOUND,null);
            }
        if (!appHome.canRead() || !appHome.canWrite()) {
            throw processException(ERR_HOME_DIRECTORY_NOT_READABLE_AND_WRITABLE,null);
        }

        // and also the required properties files
        if (!connectionPropertiesFile.exists()) {
            throw processException(format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, connectionPropertiesFileName),null);
        }
        if (!mailPropertiesFile.exists()) {
            throw processException(format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, mailPropertiesFileName),null);
        }
    }

    /**
     * This method takes an encoded properties file and returns a map representing the encoded properties
     * @param propertiesFile - the encoded properties file
     * @return the map representing the properties in the encoded file
     * @throws CoeusCliException if configuration files are not accessible
     */
    private Map<String, String> decodeProperties(File propertiesFile) throws CoeusCliException {
        Properties connectionProperties = new Properties();
        String resource="";
        try {
            resource = propertiesFile.getCanonicalPath();
            InputStream resourceStream = this.getClass().getResourceAsStream(resource);

            if (resourceStream == null) {
                throw processException(format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, resource), null);
            }
            connectionProperties.load(new Base64InputStream(resourceStream));
        } catch (IOException e) {
            throw processException(format(ERR_COULD_NOT_OPEN_CONFIGURATION_FILE, resource), null);
        }
        @SuppressWarnings("unchecked")
        Map<String, String> connectionMap =  (Map) connectionProperties;
        //check types to be safe - if there's a problem, we will catch it here
        for(String k : connectionMap.keySet());
        for(String v : connectionMap.values());

        return connectionMap;
        //return ((Map<String, String>) (Map) connectionProperties);
    }

    /**
     * This method processes a plain text properties file and returns a property object
     * @param propertiesFile - the properties {@code File} to be read
     * @return the Properties object derived from the supplied File
     * @throws CoeusCliException if the properties file could not be accessed.
     */
    private Properties loadProperties(File propertiesFile) throws CoeusCliException {
        Properties properties = new Properties();
        try{
            String resource = propertiesFile.getCanonicalPath();
            InputStream resourceStream = this.getClass().getResourceAsStream(resource);

            if (resourceStream == null) {
                throw processException(format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, resource), null);
            }
                properties.load(resourceStream);
        } catch (IOException e) {
            throw processException(ERR_COULD_NOT_OPEN_CONFIGURATION_FILE, e);
        }
        return properties;
    }

    /**
     * Ths method returns  a string representing the timestamp on the last line of the updated timestamps file
     * @return the timestamp string
     * @throws CoeusCliException if the updated timestamps file could not be accessed
     */
    private String getLatestTimestamp() throws CoeusCliException {
        String lastLine="";
        if (!updateTimestampsFile.exists()) {
            throw processException(format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, updateTimestampsFileName),null);
        } else {
            try {
                BufferedReader br = new BufferedReader(new FileReader(updateTimestampsFile));
                String readLine;
                while ((readLine = br.readLine()) != null) {
                    lastLine=readLine;
                }
            } catch (IOException e) {
                throw processException(ERR_COULD_NOT_OPEN_CONFIGURATION_FILE, e);
            }
            lastLine = lastLine.replaceAll("[\\r\\n]","");
        }
        return lastLine;
    }

    /**
     * This method appends the timestamp representing the latest update timestamp of all of the {@code Grant}s being processed
     * in this running of the loader
     * @param file - the {@code File} to write to
     * @param updateString - the timestamp string to append to the {@code File}
     *
     * @throws IOException if the append fails
     */
    private void appendLineToFile(File file, String updateString) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(file.getCanonicalPath(), true), "UTF-8");
        BufferedWriter fbw = new BufferedWriter(writer);
        fbw.write(updateString);
        fbw.newLine();
        fbw.close();
    }

    /**
     * This method logs the supplied message and exception, reports the {@code Exception} to STDOUT, and
     * causes an email regarding this Exception to be sent to the address configures in the mail properties file
     * @param message - the error message
     * @param e - the Exception
     * @return = the {@code CoeusCliException} wrapper
     */
    private CoeusCliException processException (String message, Exception e){
        String errorSubject = "COEUS Data Loader ERROR";
        if(e != null) {
            LOG.error(message, e);
            emailService.sendEmailMessage(errorSubject,message + " " + e.getMessage());
            return new CoeusCliException(message, e);
        } else {
            LOG.error(message);
            emailService.sendEmailMessage(errorSubject, message);
            return new CoeusCliException(message);
        }
    }


}
