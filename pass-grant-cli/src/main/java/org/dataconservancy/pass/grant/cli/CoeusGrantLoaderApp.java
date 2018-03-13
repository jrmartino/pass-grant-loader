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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class CoeusGrantLoaderApp {
    private static Logger LOG = LoggerFactory.getLogger(CoeusGrantLoaderApp.class);

    private static String ERR_HOME_DIRECTORY_NOT_READABLE_AND_WRITABLE = "Supplied home directory must be readable" +
            " and writable by the user running this application.";
    private static String ERR_REQUIRED_CONFIGURATION_FILE_MISSING = "Required file %s is missing in the specified home directory.";
    private static String ERR_COULD_NOT_OPEN_CONFIGURATION_FILE = "Could not open configuration file";
    private static String connectionPropertiesFileName= "connection.properties";
    private static String mailPropertiesFileName = "mail.properties";

    private EmailService emailService;

    private File appHome;
    private File  mailPropertiesFile = new File(appHome, mailPropertiesFileName);
    private File connectionPropertiesFile = new File(appHome, connectionPropertiesFileName);

    Map<String, String> connectionPropertiesMap = new HashMap<>();
    Properties mailProperties;
    String queryString = null;

    public CoeusGrantLoaderApp(File coeusLoaderHome) {
        this.appHome = coeusLoaderHome;
        }

    void run() throws CoeusCliException {
        checkFilesAreOK(appHome);

        try {
            connectionPropertiesMap = decodeProperties(connectionPropertiesFile);
            mailProperties = loadProperties(mailPropertiesFile);
            emailService = new EmailService(mailProperties);
        } catch (IOException | RuntimeException e) {
            LOG.error(ERR_COULD_NOT_OPEN_CONFIGURATION_FILE, e.getMessage());
            throw new CoeusCliException(ERR_COULD_NOT_OPEN_CONFIGURATION_FILE, e);
        }


    }

    private void checkFilesAreOK(File appHome) throws CoeusCliException {
        //First make sure the specified home directory exists and is suitable
        if (!appHome.exists()) {
            String ERR_HOME_DIRECTORY_NOT_FOUND = "No home directory found for the application. " +
                    "Please check the supplied absolute path";
            throw new CoeusCliException(ERR_HOME_DIRECTORY_NOT_FOUND);
        }
        if (!appHome.canRead() || !appHome.canWrite()) {
            LOG.error(ERR_HOME_DIRECTORY_NOT_READABLE_AND_WRITABLE);
            throw new CoeusCliException(ERR_HOME_DIRECTORY_NOT_READABLE_AND_WRITABLE);
        }

        // and also the required properties files
        if (!connectionPropertiesFile.exists()) {
            LOG.error(format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, connectionPropertiesFileName));
            throw new CoeusCliException(format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, connectionPropertiesFileName));
        }
        if (!mailPropertiesFile.exists()) {
            LOG.error(format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, mailPropertiesFileName));
            throw new CoeusCliException(format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, mailPropertiesFileName));
        }
    }

    private Map<String, String> decodeProperties(File propertiesFile) throws IOException, RuntimeException {

        String resource = propertiesFile.getCanonicalPath();
        InputStream resourceStream = this.getClass().getResourceAsStream(resource);

        if (resourceStream == null) {
            LOG.error(format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, resource));
            throw new RuntimeException(new CoeusCliException(
                    format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, resource)));
        }

        Properties connectionProperties = new Properties();
        connectionProperties.load(new Base64InputStream(resourceStream));

        @SuppressWarnings("unchecked")
        Map<String, String> connectionMap =  (Map) connectionProperties;
        //check types to be safe
        for(String k : connectionMap.keySet());
        for(String v : connectionMap.values());
        return connectionMap;
        //return ((Map<String, String>) (Map) connectionProperties);
    }

    private Properties loadProperties(File propertiesFile) throws IOException, RuntimeException {

        String resource = propertiesFile.getCanonicalPath();
        InputStream resourceStream = this.getClass().getResourceAsStream(resource);

        if (resourceStream == null) {
            LOG.error(format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, resource));
            throw new RuntimeException(new CoeusCliException(
                    format(ERR_REQUIRED_CONFIGURATION_FILE_MISSING, resource)));
        }

        Properties properties = new Properties();
        properties.load(resourceStream);

        return properties;
    }

}
