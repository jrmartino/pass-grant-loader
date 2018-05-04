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
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.dataconservancy.pass.client.PassClient;
import org.dataconservancy.pass.client.PassClientFactory;
import org.dataconservancy.pass.grant.data.CoeusConnector;
import org.dataconservancy.pass.grant.data.PassUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static org.dataconservancy.pass.grant.cli.CoeusGrantLoaderErrors.*;
import static org.dataconservancy.pass.grant.data.DateTimeUtil.verifyDateTimeFormat;

/**
 * This class does the orchestration for the pulling of COEUS grant and user data. The basic steps are to read in all of the
 * configuration files needed by the various classes; construct the query string for the COEUS Oracle DB to pull in all
 * of the grants or users updated since the timestamp at the end of the updated timestamps file; execute the query against this
 * database; use a {@code List} representing the {@code ResultSet} to populate a list of {@code Grant}s or {@code User}s in our java
 * model; and finally to push this data into our pass instance via the java pass client.
 *
 * A large percentage of the code here is handling exceptional paths, as this is intended to be run in an automated
 * fashion, so care must be taken to log errors, report them to STDOUT, and also send email notifications.
 *
 * @author jrm@jhu.edu
 */
class CoeusGrantLoaderApp {
    private static Logger LOG = LoggerFactory.getLogger(CoeusGrantLoaderApp.class);


    private EmailService emailService;

    private File appHome;
    private String startDate;
    private File updateTimestampsFile;
    private boolean email;
    private String mode;

    private String updateTimestampsFileName;

    /**
     * Constructor for this class
     * @param startDate - the latest successful update timestamp, occurring as the last line of the update timestamps file
     * @param email - a boolean which indicates whether or not to send email notification of the result of the current run
     */
    CoeusGrantLoaderApp(String startDate, boolean email, String mode) {
        this.appHome = new File(System.getProperty("COEUS_HOME"));
        this.startDate = startDate;
        this.email = email;
        this.mode = mode;
        this.updateTimestampsFileName = mode + "_update_timestamps";
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
        String[] systemProperties = {"pass.fedora.user", "pass.fedora.password", "pass.fedora.baseurl"};

        updateTimestampsFile = new File(appHome, updateTimestampsFileName);
        Properties connectionProperties;
        Properties mailProperties;

        //check that we have a good value for mode
        if (!mode.equals("grant") && !mode.equals("user")) {
            throw processException(format(ERR_MODE_NOT_VALID,mode), null);
        }

        //first check that we have the required files
        if (!appHome.exists()) {
            throw processException(ERR_HOME_DIRECTORY_NOT_FOUND, null);
        }
        if (!appHome.canRead() || !appHome.canWrite()) {
            throw processException(ERR_HOME_DIRECTORY_NOT_READABLE_AND_WRITABLE, null);
        }

        //add new system properties if we have any
        if (systemPropertiesFile.exists() && systemPropertiesFile.canRead()) {
            Properties sysProps = loadProperties(systemPropertiesFile);
            for (String key : systemProperties) {
                String value = sysProps.getProperty(key);
                if (value != null) {
                    System.setProperty(key, value);
                }
            }
        }


        //create mail properties and instantiate email service if we are using the service
        if (email) {
            if (!mailPropertiesFile.exists()) {
                throw processException(format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, mailPropertiesFileName), null);
            }
            try {
                mailProperties = loadProperties(mailPropertiesFile);
                emailService = new EmailService(mailProperties);
            } catch (RuntimeException e) {
                throw processException(ERR_COULD_NOT_OPEN_CONFIGURATION_FILE, e);
            }
        }

        //create connection properties - check for a user-space defined clear text file
        if (!connectionPropertiesFile.exists()) {
            throw processException(format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, connectionPropertiesFileName), null);
        }
        try {
            connectionProperties = loadProperties(connectionPropertiesFile);
            } catch (RuntimeException e) {
                throw processException(ERR_COULD_NOT_OPEN_CONFIGURATION_FILE, e);
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
        String queryString = coeusConnector.buildQueryString(startDate, mode);
        Set<Map<String,String>> resultsSet;
        PassUpdater passUpdater;
        PassClient passClient = PassClientFactory.getPassClient();

        try {
            resultsSet = coeusConnector.retrieveUpdates(queryString, mode);
            passUpdater = new PassUpdater(passClient);
            passUpdater.updatePass(resultsSet, mode);

        } catch (ClassNotFoundException e) {
            throw processException(ERR_ORACLE_DRIVER_NOT_FOUND, e);
        } catch (SQLException e) {
            throw processException(ERR_SQL_EXCEPTION, e);
        } catch (RuntimeException e) {
            throw processException ("Runtime Exception", e);
        }

        //apparently the hard part has succeeded, let's write the timestamp to our update timestamps file
        try{
            appendLineToFile(updateTimestampsFile,  passUpdater.getLatestUpdate());
        } catch (IOException e) {
            throw processException(format(ERR_COULD_NOT_APPEND_UPDATE_TIMESTAMP,  passUpdater.getLatestUpdate()), null);
        }

        //now everything succeeded - log this result and send email if enabled
        String message =  passUpdater.getReport();
        LOG.info(message);
        System.out.println(message);
        if(email) {
            emailService.sendEmailMessage("COEUS Data Loader SUCCESS", message);
        }
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
     * optionally causes an email regarding this {@code Exception} to be sent to the address configured
     * in the mail properties file
     * @param message - the error message
     * @param e - the Exception
     * @return = the {@code CoeusCliException} wrapper
     */
    private CoeusCliException processException (String message, Exception e){
        CoeusCliException clie;

        String errorSubject = "COEUS Data Loader ERROR";
        if(e != null) {
            clie = new CoeusCliException(message, e);
            LOG.error(message, e);
            e.printStackTrace();
            if (email) {
                emailService.sendEmailMessage(errorSubject, clie.getMessage());
            }
        } else {
            clie = new CoeusCliException(message);
            LOG.error(message);
            System.err.println(message);
            if(email) {
                emailService.sendEmailMessage(errorSubject, message);
            }
        }
        return clie;
    }

}
