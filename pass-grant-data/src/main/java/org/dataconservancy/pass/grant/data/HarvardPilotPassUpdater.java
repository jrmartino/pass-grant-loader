package org.dataconservancy.pass.grant.data;

import org.dataconservancy.pass.client.PassClient;

public class HarvardPilotPassUpdater extends DefaultPassUpdater {

HarvardPilotPassUpdater (PassClient passClient, PassEntityUtil passEntityUtil ) {
    super(passClient, passEntityUtil);
    super.setDomain("johnshopkins.edu");

}


}
