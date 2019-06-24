package org.dataconservancy.pass.grant.data;

import org.dataconservancy.pass.client.PassClient;

public class HarvardPilotPassUpdater extends DefaultPassUpdater {

    HarvardPilotPassUpdater () {
        super(new DefaultPassEntityUtil());
        super.setDomain("harvard.edu");
    }

}
