package org.dataconservancy.pass.grant.data;

import org.dataconservancy.pass.model.Funder;
import org.dataconservancy.pass.model.Grant;
import org.dataconservancy.pass.model.User;

public class DefaultPassEntityUtil implements PassEntityUtil {

    /**
     *  This method takes a Harvard Pilot Funder, calculates whether it needs to be updated, and if so, returns the updated object
     *  to be be ingested into the repository. if not, returns null.
     * @param stored the Funder as it is stored in the PASS backend
     * @param system the version of the Funder from the Harvard Pilot pull
     * @return the updated Funder - null if the Funder does not need to be updated
     */
    public Funder update(Funder system, Funder stored) {
        if (!system.equals(stored)) {
            return updateFunder(system, stored);
        }
        return null;
    }

    /**
     *  This method takes a Harvard Pilot User, calculates whether it needs to be updated, and if so, returns the updated object
     *  to be be ingested into the repository. if not, returns null.
     * @param stored the User as it is stored in the PASS backend
     * @param system the version of the user from the Harvard Pilot pull
     * @return the updated User - null if the User does not need to be updated
     */
    public User update(User system, User stored) {
        if (!system.equals(stored)) {
            return updateUser(system, stored);
        }
        return null;
    }

    /**
     * Update a Pass Funder object with new information from Harvard Pilot
     *
     * @param system the version of the Funder as seen in the Harvard Pilot system pull
     * @param stored the version of the Funder as read from Pass
     * @return the Funder object which represents the Pass object, with any new information from Harvard Pilot merged in
     */
    private Funder updateFunder (Funder system, Funder stored) {
        stored.setLocalKey(system.getLocalKey());
        if (system.getName() != null) {  stored.setName(system.getName()); }
        if (system.getPolicy() != null) { stored.setPolicy(system.getPolicy()); }
        return stored;
    }
    /**
     *  This method takes a Harvard Pilot Grant, calculates whether it needs to be updated, and if so, returns the updated object
     *  to be be ingested into the repository. if not, returns null.
     * @param stored the Grant as it is stored in the PASS backend
     * @param system the version of the Grant from the Harvard Pilot pull
     * @return the updated object - null if the Grant does not need to be updated
     */
    public Grant update(Grant system, Grant stored) {
        if (!system.equals(stored)) {
            return updateGrant(system, stored);
        }
        return null;
    }
    /**
     * Update a Pass User object with new information from Harvard Pilot. We check only those fields for which Harvard Pilot is
     * authoritative. Other fields will be managed by other providers (Shibboleth for example). The exceptions are
     * the localKey, which this application and Shibboleth both rely on; and  email, which this application only populates
     * if Shib hasn't done so already.
     *
     * @param system the version of the User as seen in the Harvard Pilot system pull
     * @param stored the version of the User as read from Pass
     * @return the User object which represents the Pass object, with any new information from Harvard Pilot merged in
     */
    private User updateUser (User system, User stored) {
        stored.setFirstName(system.getFirstName());
        stored.setLastName(system.getLastName());
        //populate null fields if we can
        if((stored.getEmail() == null) && (system.getEmail() != null)) {
            stored.setEmail(system.getEmail());
        }
        if((stored.getDisplayName() == null && system.getDisplayName() != null)) {
            stored.setDisplayName(system.getDisplayName());
        }
        return stored;
    }


    /**
     * Update a Pass Grant object with new information from Harvard Pilot
     *
     * @param system the version of the Grant as seen in the Harvard Pilot system pull
     * @param stored the version of the Grant as read from Pass
     * @return the Grant object which represents the Pass object, with any new information from Harvard Pilot merged in
     */
    private Grant updateGrant(Grant system, Grant stored) {
        stored.setAwardNumber(system.getAwardNumber());
        stored.setLocalKey(system.getLocalKey());
        stored.setProjectName(system.getProjectName());
        stored.setPrimaryFunder(system.getPrimaryFunder());
        stored.setDirectFunder(system.getDirectFunder());
        stored.setPi(system.getPi());
        stored.setCoPis(system.getCoPis());
        stored.setStartDate(system.getStartDate());
        stored.setEndDate(system.getEndDate());
        return stored;
    }

}
