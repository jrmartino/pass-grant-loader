/*
 * Copyright 2019 Johns Hopkins University
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

import org.dataconservancy.pass.grant.data.CoeusConnector;
import org.dataconservancy.pass.grant.data.DateTimeUtil;
import org.dataconservancy.pass.grant.data.GrantConnector;
import org.dataconservancy.pass.grant.data.JhuPassUpdater;
import org.dataconservancy.pass.grant.data.PassUpdater;

import java.util.Properties;

class JhuGrantLoaderApp extends BaseGrantLoaderApp {

    JhuGrantLoaderApp(String startDate, String awardEndDate, boolean email, String mode, String action, String dataFileName) {
        super(startDate, awardEndDate, email, mode, action, dataFileName);
        super.setTimestamp(true);
    }

    @Override
    boolean checkMode(String s) {
        return (s.equals("user") || s.equals("grant") || s.equals("funder"));
    }

    @Override
    GrantConnector configureConnector(Properties connectionProperties, Properties policyProperties) {
        return new CoeusConnector(connectionProperties, policyProperties);
    }

    @Override
    PassUpdater configureUpdater() {
        return new JhuPassUpdater();
    }

}
