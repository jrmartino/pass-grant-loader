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
import org.dataconservancy.pass.grant.data.CoeusConnector;
import org.dataconservancy.pass.grant.data.GrantModelBuilder;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.lang.String.format;

public class CoeusGrantLoaderApp {
    private static String ERR_PROPERTIES_FILE_NOT_FOUND = "No classpath resource found for COEUS connection configuration. File '%s' " +
            " not found on classpath.";
    private File propertiesFile;
    private String startDate;
    private String endDate;

    public CoeusGrantLoaderApp(File propertiesFile, String startDate, String endDate) {

        this.propertiesFile = propertiesFile;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    void run() throws CoeusCliException {
        Map<String, String> connectionProperties = new HashMap<>();

        CoeusConnector connector = new CoeusConnector(decodeProperties(propertiesFile));
        String queryString = connector.buildQueryString(startDate, endDate);
        ResultSet rs = connector.retrieveCoeusUpdates(queryString);
        GrantModelBuilder builder = new GrantModelBuilder(rs);
        List grantList = builder.buildGrantList();

    }
        private Map<String, String> decodeProperties (File propertiesFile){

            String resource = null;
            try {
                resource = propertiesFile.getCanonicalPath();
            } catch (IOException e) {
                e.printStackTrace();
            }

            InputStream resourceStream = this.getClass().getResourceAsStream(resource);

            if (resourceStream == null) {
                throw new RuntimeException(new CoeusCliException(
                        format(ERR_PROPERTIES_FILE_NOT_FOUND, resource)));
            }

            Properties connectionProperties = new Properties();
            try {
                connectionProperties.load(new Base64InputStream(resourceStream));

            } catch (IOException e) {
                e.printStackTrace();
            }
            return ((Map<String, String>) (Map) connectionProperties);
        }


        private void normalizeStartAndEndDates () {
            String today = "";


        }

}
