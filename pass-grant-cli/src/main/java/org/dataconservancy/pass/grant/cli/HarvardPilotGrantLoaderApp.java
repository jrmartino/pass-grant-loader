package org.dataconservancy.pass.grant.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.lang.String.format;
import static org.dataconservancy.pass.grant.cli.DataLoaderErrors.*;

public class HarvardPilotGrantLoaderApp {

    private static Logger LOG = LoggerFactory.getLogger(CoeusGrantLoaderApp.class);
    private EmailService emailService;

    private File appHome;
    private String startDate;
    private String awardEndDate;
    private boolean email;
    private String mode;
    private String action;
    private String dataFileName;

    HarvardPilotGrantLoaderApp(String startDate, String awardEndDate, boolean email, String mode, String action, String dataFileName) {
        this.appHome = new File(System.getProperty("COEUS_HOME"));
        this.startDate = startDate;
        this.awardEndDate = awardEndDate;
        this.email = email;
        this.mode = mode;
        this.action = action;
        this.dataFileName = dataFileName;


    }

    void run() throws PassCliException {

        String mailPropertiesFileName = "mail.properties";
        File mailPropertiesFile = new File(appHome, mailPropertiesFileName);
        String systemPropertiesFileName = "system.properties";
        File systemPropertiesFile = new File(appHome, systemPropertiesFileName);
        String policyPropertiesFileName = "policy.properties";
        File policyPropertiesFile = new File(appHome, policyPropertiesFileName);
        File dataFile = new File(dataFileName);

        //let's be careful about overwriting system properties
        String[] systemProperties = {"pass.fedora.user", "pass.fedora.password", "pass.fedora.baseurl",
                "pass.elasticsearch.url", "pass.elasticsearch.limit"};

        Properties connectionProperties;
        Properties mailProperties;
        Properties policyProperties;


        //check that we have a good value for mode
        if (!mode.equals("grant") &&  !mode.equals("funder")) {
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

        List<Map<String,String>> resultSet = null;

        //now do things;
    }



    /**
     * This method processes a plain text properties file and returns a {@code Properties} object
     * @param propertiesFile - the properties {@code File} to be read
     * @return the Properties object derived from the supplied {@code File}
     * @throws PassCliException if the properties file could not be accessed.
     */
    protected Properties loadProperties(File propertiesFile) throws PassCliException {
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
     * This method logs the supplied message and exception, reports the {@code Exception} to STDOUT, and
     * optionally causes an email regarding this {@code Exception} to be sent to the address configured
     * in the mail properties file
     * @param message - the error message
     * @param e - the Exception
     * @return = the {@code PassCliException} wrapper
     */
    private PassCliException processException (String message, Exception e){
        PassCliException clie;

        String errorSubject = "Harvard Pilot Data Loader ERROR";
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

