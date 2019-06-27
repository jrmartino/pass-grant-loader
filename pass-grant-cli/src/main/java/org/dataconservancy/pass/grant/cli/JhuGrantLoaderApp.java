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
