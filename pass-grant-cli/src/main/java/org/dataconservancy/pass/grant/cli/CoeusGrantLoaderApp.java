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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.dataconservancy.pass.client.PassClient;
import org.dataconservancy.pass.client.PassClientFactory;
import org.dataconservancy.pass.grant.data.CoeusConnector;
import org.dataconservancy.pass.grant.data.DateTimeUtil;
import org.dataconservancy.pass.grant.data.JhuPassUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static org.dataconservancy.pass.grant.cli.DataLoaderErrors.*;
import static org.dataconservancy.pass.grant.data.DateTimeUtil.verifyDateTimeFormat;
import static org.dataconservancy.pass.grant.data.DateTimeUtil.verifyDate;

/**
 * This class does the orchestration for the pulling of grant and user data. The basic steps are to read in all of the
 * configuration files needed by the various classes; call the GrantLoader to pull in all
 * of the grants or users updated since the timestamp at the end of the updated timestamps file;
 * use the PassLoader to take {@code List} representing the {@code ResultSet} to push this data into our PASS instance
 * via the java pass client.
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
    private String awardEndDate;
    private File updateTimestampsFile;
    private boolean email;
    private String mode;
    private String action;
    private String dataFileName;

    private String updateTimestampsFileName;

    /**
     * Constructor for this class
     * @param startDate - the latest successful update timestamp, occurring as the last line of the update timestamps file
     * @param email - a boolean which indicates whether or not to send email notification of the result of the current run
     * @param mode - a String indicating whether we are updating grants, or existing users in PASS
     * @param action - a String indicating an optional restriction to just pulling data from COEUS and saving a serialized
     *               version to a file, or just taking serialized data in a file and loading it into PASS
     * @param dataFileName - a String representing the path to an output file for a pull, or input for a load
     */
    CoeusGrantLoaderApp(String startDate, String awardEndDate, boolean email, String mode, String action, String dataFileName) {
        this.appHome = new File(System.getProperty("COEUS_HOME"));
        this.startDate = startDate;
        this.awardEndDate = awardEndDate;
        this.email = email;
        this.mode = mode;
        this.action = action;
        this.dataFileName = dataFileName;
        this.updateTimestampsFileName = mode + "_update_timestamps";
        }

    /**
     * The orchestration method for everything. This is called by the {@code CoeusGrantLoaderCLI}, which only manages the
     * command line interaction.
     *
     * @throws PassCliException if there was any error occurring during the grant loading or updating processes
     */
    void run() throws PassCliException {
        String connectionPropertiesFileName = "connection.properties";
        File connectionPropertiesFile = new File(appHome, connectionPropertiesFileName);
        String mailPropertiesFileName = "mail.properties";
        File mailPropertiesFile = new File(appHome, mailPropertiesFileName);
        String systemPropertiesFileName = "system.properties";
        File systemPropertiesFile = new File(appHome, systemPropertiesFileName);
        File dataFile = new File(dataFileName);

        //let's be careful about overwriting system properties
        String[] systemProperties = {"pass.fedora.user", "pass.fedora.password", "pass.fedora.baseurl",
        "pass.elasticsearch.url", "pass.elasticsearch.limit"};

        updateTimestampsFile = new File(appHome, updateTimestampsFileName);
        Properties connectionProperties;
        Properties mailProperties;

        //check that we have a good value for mode
        if (!mode.equals("grant") && !mode.equals("user")) {
            throw processException(format(ERR_MODE_NOT_VALID,mode), null);
        }

        //check that we have a good value for action
        if (!action.equals("") && !action.equals("pull") && !action.equals("load")) {
            throw processException(format(ERR_ACTION_NOT_VALID,action), null);
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

        //check suitability of our input file
        if(action.equals("load")) {
            if (!dataFile.exists()) {
                throw processException(format(ERR_REQUIRED_DATA_FILE_MISSING, dataFileName), null);
            } else if (!dataFile.canRead()) {
                throw processException(format(ERR_DATA_FILE_CANNOT_READ, dataFileName), null);
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

        List<Map<String,String>> resultSet = null;

        //now do things;
        if (!action.equals("load")) {//action includes a pull - need to build a result set
            //establish the start dateTime - it is either given as an option, or it is
            //the last entry in the update_timestamps file

            if (startDate.length() > 0) {
                if (!verifyDateTimeFormat(startDate)) {
                    throw processException(format(ERR_INVALID_COMMAND_LINE_TIMESTAMP, startDate),null);
                }
            } else {
                startDate = getLatestTimestamp();
                if (!verifyDateTimeFormat(startDate)) {
                    throw processException(format(ERR_INVALID_TIMESTAMP, startDate),null);
                }
            }

            if (!verifyDate(awardEndDate)) {
                throw processException(format(ERR_INVALID_COMMAND_LINE_DATE, awardEndDate), null);
            }

            CoeusConnector coeusConnector = new CoeusConnector(connectionProperties);
            String queryString = coeusConnector.buildQueryString(startDate, awardEndDate, mode);
            try {
                resultSet = coeusConnector.retrieveUpdates(queryString, mode);
            } catch (ClassNotFoundException e) {
                throw processException(ERR_ORACLE_DRIVER_NOT_FOUND, e);
            } catch (SQLException e) {
                throw processException(ERR_SQL_EXCEPTION, e);
            } catch (RuntimeException e) {
                throw processException("Runtime Exception", e);
            } catch (IOException e) {
                throw processException(ERR_DIRECTORY_LOOKUP_ERROR, e);
            }
        } else {//just doing a PASS load, must have results set in the data file
            try (FileInputStream fis = new FileInputStream(dataFile);
                 ObjectInputStream in = new ObjectInputStream(fis)
                ) {
                resultSet = (List<Map<String, String>>)in.readObject();
            } catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        }

        if (resultSet == null) {//this shouldn't happen
            throw processException(ERR_RESULT_SET_NULL, null);
        }

        //update PASS if required
        if (!action.equals("pull")) {
            JhuPassUpdater passUpdater;
            PassClient passClient = PassClientFactory.getPassClient();
            try {
                passUpdater = new JhuPassUpdater(passClient);
                passUpdater.updatePass(resultSet, mode);
            } catch (RuntimeException e) {
                throw processException("Runtime Exception", e);
            }

            //apparently the hard part has succeeded, let's write the timestamp to our update timestamps file
            String updateTimestamp = passUpdater.getLatestUpdate();
            if (DateTimeUtil.verifyDateTimeFormat(updateTimestamp)) {
                try {
                    appendLineToFile(updateTimestampsFile, passUpdater.getLatestUpdate());
                } catch (IOException e) {
                    throw processException(format(ERR_COULD_NOT_APPEND_UPDATE_TIMESTAMP, passUpdater.getLatestUpdate()), null);
                }
            }

            //now everything succeeded - log this result and send email if enabled
            String message = passUpdater.getReport();
            LOG.info(message);
            System.out.println(message);
            if (email) {
                emailService.sendEmailMessage("COEUS Data Pull SUCCESS", message);
            }
        } else {//don't need to update, just write the result set out to the data file
            try (FileOutputStream fos = new FileOutputStream(dataFile);
                 ObjectOutputStream out  = new ObjectOutputStream(fos)
            ) {
                out.writeObject(resultSet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //do some notification
            int size = resultSet.size();
            StringBuilder sb = new StringBuilder();
            sb.append("Wrote result set for ");
            sb.append(size);
            sb.append(" ");
            sb.append(mode);
            sb.append(" record");
            sb.append((size==1?"":"s")); //handle plural correctly
            sb.append(" into file ");
            sb.append(dataFileName);
            sb.append("\n");
            String message = sb.toString();
            LOG.info(message);
            System.out.println(message);
            if (email) {
                emailService.sendEmailMessage("COEUS Data Loader SUCCESS", message);
            }
        }
    }

    /**
     * This method processes a plain text properties file and returns a {@code Properties} object
     * @param propertiesFile - the properties {@code File} to be read
     * @return the Properties object derived from the supplied {@code File}
     * @throws PassCliException if the properties file could not be accessed.
     */
    private Properties loadProperties(File propertiesFile) throws PassCliException {
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
     * @throws PassCliException if the updated timestamps file could not be accessed
     */
    private String getLatestTimestamp() throws PassCliException {
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
                new FileOutputStream(file.getCanonicalPath(), true), StandardCharsets.UTF_8);
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
     * @return = the {@code PassCliException} wrapper
     */
    private PassCliException processException (String message, Exception e){
        PassCliException clie;

        String errorSubject = "COEUS Data Loader ERROR";
        if(e != null) {
            clie = new PassCliException(message, e);
            LOG.error(message, e);
            e.printStackTrace();
            if (email) {
                emailService.sendEmailMessage(errorSubject, clie.getMessage());
            }
        } else {
            clie = new PassCliException(message);
            LOG.error(message);
            System.err.println(message);
            if(email) {
                emailService.sendEmailMessage(errorSubject, message);
            }
        }
        return clie;
    }
}
