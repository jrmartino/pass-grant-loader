package org.dataconservancy.pass.grant.data;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class HarvardPilotConnectorTest {

    private HarvardPilotConnector connector;

    private File policyPropertiesFile = new File(getClass().getClassLoader().getResource("policy.properties").getFile());

    private Properties policyProperties = new Properties();

    private Properties connectionProperties = new Properties();

    @Before
    public void setup() throws Exception {

        System.setProperty("pass.fedora.baseurl", "https://localhost:8080/fcrepo/rest");

        try (InputStream resourceStream = new FileInputStream(policyPropertiesFile)) {
            policyProperties.load(resourceStream);
        }

        File dataFile = new File(getClass().getClassLoader().getResource("HarvardPASSTestData.xlsx").getFile());


        connectionProperties.setProperty(connector.HARVARD_DATA_FILE_PATH_PROPERTY, dataFile.getAbsolutePath());


        connector = new HarvardPilotConnector(connectionProperties, policyProperties);
    }

    @Test
    public void testRetrieveGrantUpdates() throws IOException {

        List<Map<String, String>> grantResultSet =  connector.retrieveUpdates(null, "grant");
        assertEquals(8, grantResultSet.size());

    }

    @Test
    public void testRetrieveFunderUpdates() throws IOException {

        List<Map<String, String>>  funderResultSet = connector.retrieveUpdates(null, "funder");
        assertEquals(5, funderResultSet.size());

    }

}
