package org.dataconservancy.pass.grant.cli;

public class HarvardPilotGrantLoaderApp extends DefaultGrantLoaderApp {
    HarvardPilotGrantLoaderApp(String startDate, String awardEndDate, boolean email, String mode, String action, String dataFileName) {
        super(startDate, awardEndDate, email, mode, action, dataFileName);
        super.setTimestamp(false);
    }

    @Override
    boolean checkMode(String s) {
        return (s.equals("grant") || s.equals("funder"));
    }
}