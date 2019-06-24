package org.dataconservancy.pass.grant.cli;

public class JhuGrantLoaderApp extends DefaultGrantLoaderApp {

    JhuGrantLoaderApp(String startDate, String awardEndDate, boolean email, String mode, String action, String dataFileName) {
        super(startDate, awardEndDate, email, mode, action, dataFileName);
        super.setTimestamp(true);
    }
}
