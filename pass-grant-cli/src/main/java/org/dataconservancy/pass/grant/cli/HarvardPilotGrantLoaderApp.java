package org.dataconservancy.pass.grant.cli;

import org.dataconservancy.pass.grant.data.GrantConnector;
import org.dataconservancy.pass.grant.data.HarvardPilotConnector;
import org.dataconservancy.pass.grant.data.HarvardPilotPassUpdater;
import org.dataconservancy.pass.grant.data.PassUpdater;

import java.util.Properties;

class HarvardPilotGrantLoaderApp extends DefaultGrantLoaderApp {
    HarvardPilotGrantLoaderApp(String startDate, String awardEndDate, boolean email, String mode, String action, String dataFileName) {
        super(startDate, awardEndDate, email, mode, action, dataFileName);
        super.setTimestamp(false);
    }

    @Override
    boolean checkMode(String s) {
        return (s.equals("grant") || s.equals("funder"));
    }

    @Override
    GrantConnector configureConnector(Properties connectionProperties, Properties policyProperties) {
        return new HarvardPilotConnector(connectionProperties, policyProperties);
    }

    @Override
    PassUpdater configureUpdater() {
        return new HarvardPilotPassUpdater();
    }
}