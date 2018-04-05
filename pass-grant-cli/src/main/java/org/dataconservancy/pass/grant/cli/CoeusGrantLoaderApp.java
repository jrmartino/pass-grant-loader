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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.dataconservancy.pass.grant.data.CoeusConnector;
import org.dataconservancy.pass.grant.data.GrantUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static org.dataconservancy.pass.grant.cli.CoeusGrantLoaderErrors.*;
import static org.dataconservancy.pass.grant.data.DateTimeUtil.verifyDateTimeFormat;

/**
 * This class does the orchestration for the pulling of COEUS grant data. The basic steeps are to read in all of the
 * configuration files needed by the various classes; construct the query string for the COEUS Oracle DB to pull in all
 * of the grants updated since the timestamp at the end of the updated timestamps file; execute the query against this
 * database; use a {@code List} representing the {@code ResultSet} to populate a list of {@code Grant}s in our java
 * model; and finally to push this data into our Fedora instance via the java Fedora client.
 *
 * A large percentage of the code here is handling exceptional paths, as this is intended to be run in an automated
 * fashion, so care must be taken to log errors, report them to STDOUT, and also send email notifications.
 *
 * @author jrm@jhu.edu
 */
public class CoeusGrantLoaderApp {
    private static Logger LOG = LoggerFactory.getLogger(CoeusGrantLoaderApp.class);

    private static String updateTimestampsFileName = "update_timestamps";

    private EmailService emailService;

    private File appHome;
    private String startDate;
    private File updateTimestampsFile;

    /**
     * Constructor for this class
     * @param startDate - the latest successful update timestamp, occurring as the last line of the update timestamps file
     */
    CoeusGrantLoaderApp(String startDate) {
        this.appHome = new File(System.getProperty("COEUS_HOME"));
        this.startDate = startDate;
        }

    /**
     * The orchestration method for everything. This is called by the {@code CoeusGrantLoaderCLI}, which only manages the
     * command line interaction.
     *
     * @throws CoeusCliException if there was any error occurring during the grant loading or updating processes
     */
    void run() throws CoeusCliException {
        String connectionPropertiesFileName = "connection.properties";
        File connectionPropertiesFile = new File(appHome, connectionPropertiesFileName);
        String mailPropertiesFileName = "mail.properties";
        File mailPropertiesFile = new File(appHome, mailPropertiesFileName);
        String systemPropertiesFileName = "system.properties";
        File systemPropertiesFile = new File(appHome, systemPropertiesFileName);
        //let's be careful about overwriting system properties
        String[] systemProperties  = {"pass.fedora.user", "pass.fedora.password", "pass.fedora.baseurl"};

        updateTimestampsFile = new File(appHome, updateTimestampsFileName);
        Properties connectionProperties;
        Properties mailProperties;

        //first check that we have the required files
        if (!appHome.exists()) {
            throw processException(ERR_HOME_DIRECTORY_NOT_FOUND, null);
        }
        if (!appHome.canRead() || !appHome.canWrite()) {
            throw processException(ERR_HOME_DIRECTORY_NOT_READABLE_AND_WRITABLE, null);
        }

        //add new system properties if we have any
        if(systemPropertiesFile.exists() && systemPropertiesFile.canRead()){
           Properties sysProps = loadProperties(systemPropertiesFile);
           for(String key : systemProperties) {
               String value = sysProps.getProperty(key);
               if (value != null){
                   System.setProperty(key, value);
               }
           }
        }


        //create mail properties and instantiate email service
        if (!mailPropertiesFile.exists()) {
            throw processException(format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, mailPropertiesFileName), null);
        }
        try {
            mailProperties = loadProperties(mailPropertiesFile);
            emailService = new EmailService(mailProperties);
        } catch (RuntimeException e) {
            throw processException(ERR_COULD_NOT_OPEN_CONFIGURATION_FILE, e);
        }

        //create connection properties - check for a user-space defined clear text file first
        //if not found, use the base64 encoded file in the jar
        if (!connectionPropertiesFile.exists()) {
            try {
                connectionProperties = decodeProperties(connectionPropertiesFileName);
            } catch (CoeusCliException e) {
                throw processException(format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, connectionPropertiesFileName), null);
            }
        } else {
            try {
                connectionProperties = loadProperties(connectionPropertiesFile);
            } catch (RuntimeException e) {
                throw processException(ERR_COULD_NOT_OPEN_CONFIGURATION_FILE, e);
            }
        }

        //establish the start dateTime - it is either given as an option, or it is
        //the last entry in the update_timestamps file
        if (startDate.length() > 0) {
            if (!verifyDateTimeFormat(startDate)) {
                throw processException(format(ERR_INVALID_COMMAND_LINE_TIMESTAMP, startDate),null);
            }
        } else {
            startDate = getLatestTimestamp();
        }

        //we need a valid dateTime to start the query
        if (!verifyDateTimeFormat(startDate)){//the start timestamp file is not in valid date time format
            throw processException(format(ERR_INVALID_TIMESTAMP, startDate), null);
        }

        //now do things;
        CoeusConnector coeusConnector = new CoeusConnector(connectionProperties);
        String queryString = coeusConnector.buildQueryString(startDate);
        Set<Map<String,String>> resultsSet;
        GrantUpdater grantUpdater;
        try {
            resultsSet = coeusConnector.retrieveCoeusUpdates(queryString);
            grantUpdater = new GrantUpdater(resultsSet);
            grantUpdater.updateGrants();

        } catch (ClassNotFoundException e) {
            throw processException(ERR_ORACLE_DRIVER_NOT_FOUND, e);
        } catch (SQLException e) {
            throw processException(ERR_SQL_EXCEPTION, e);
        } catch (RuntimeException e) {
            throw processException ("Runtime Exception", e);
        }

        //apparently the hard part has succeeded, let's write the timestamp to our update timestamps file
        try{
            appendLineToFile(updateTimestampsFile,  grantUpdater.getLatestUpdate());
        } catch (IOException e) {
            throw processException(format(ERR_COULD_NOT_APPEND_UPDATE_TIMESTAMP,  grantUpdater.getLatestUpdate()), null);
        }

        //now everything succeeded - log this result and send email
        String message =  grantUpdater.getReport();
        LOG.info(message);
        emailService.sendEmailMessage("COEUS Data Loader SUCCESS", message);

    }

    /**
     * This method takes an encoded properties file and returns a map representing the encoded properties
     * @param propertiesFileName - the name of the encoded properties file
     * @return the properties in the encoded file
     * @throws CoeusCliException if configuration files are not accessible
     */
    private Properties decodeProperties(String propertiesFileName) throws CoeusCliException {
        Properties connectionProperties = new Properties();
        String resource="/" + propertiesFileName;
        try(InputStream resourceStream = this.getClass().getResourceAsStream(resource)) {
            if (resourceStream == null) {
                throw processException(format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, resource), null);
            }
            connectionProperties.load(new Base64InputStream(resourceStream));
        } catch (IOException e) {
            throw processException(format(ERR_COULD_NOT_OPEN_CONFIGURATION_FILE, resource), null);
        }
        return connectionProperties;
    }

    /**
     * This method processes a plain text properties file and returns a {@code Properties} object
     * @param propertiesFile - the properties {@code File} to be read
     * @return the Properties object derived from the supplied {@code File}
     * @throws CoeusCliException if the properties file could not be accessed.
     */
    private Properties loadProperties(File propertiesFile) throws CoeusCliException {
        Properties properties = new Properties();
        String resource;
        try{
            resource = propertiesFile.getCanonicalPath();
        } catch (IOException e) {
            throw processException(ERR_COULD_NOT_OPEN_CONFIGURATION_FILE, e);
        }
        try(InputStream resourceStream = new FileInputStream(resource)){
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
            try( BufferedReader br = new BufferedReader(new FileReader(updateTimestampsFile))) {
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
     * causes an email regarding this {@code Exception} to be sent to the address configured in the mail properties file
     * @param message - the error message
     * @param e - the Exception
     * @return = the {@code CoeusCliException} wrapper
     */
    private CoeusCliException processException (String message, Exception e){
        CoeusCliException clie = new CoeusCliException(message, e);

        String errorSubject = "COEUS Data Loader ERROR";
        if(e != null) {
            LOG.error(message, clie);
            emailService.sendEmailMessage(errorSubject, clie.getMessage());
        } else {
            LOG.error(message);
            emailService.sendEmailMessage(errorSubject, message);
        }
        return new CoeusCliException(message);
    }


}
