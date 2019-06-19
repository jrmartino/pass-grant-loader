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

package org.dataconservancy.pass.grant.data;

import org.dataconservancy.pass.model.Funder;
import org.dataconservancy.pass.model.Grant;
import org.dataconservancy.pass.model.User;


import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A utility class for handling Grants, Users or Funders. One function performed is comparison of two instances of
 * these PASS entity classes. These comparisons are reduced to only those fields which are updatable by
 * data from COEUS, so that two objects are considered "COEUS equal" iff they agree on these fields.
 *
 * Another function performed by this utility class is to construct an updated version of an instance of one of these classes
 * by merging a (possibly) existing Pass object with new information obtained from a COEUS data pull.
 *
 * @author jrm@jhu.edu
 */
public class CoeusPassEntityUtil implements PassEntityUtil{

    /**
     *  This method takes a COEUS Funder, calculates whether it needs to be updated, and if so, returns the updated object
     *  to be be ingested into the repository. if not, returns null.
     * @param stored the Funder as it is stored in the PASS backend
     * @param system the version of the Funder from the COEUS pull
     * @return the updated Funder - null if the Funder does not need to be updated
     */
    public Funder update(Funder system, Funder stored) {
            if (!coeusFundersEqual(system, stored)) {
                return updateFunder(system, stored);
            }
            return null;
        }

    /**
     *  This method takes a COEUS User, calculates whether it needs to be updated, and if so, returns the updated object
     *  to be be ingested into the repository. if not, returns null.
     * @param stored the User as it is stored in the PASS backend
     * @param system the version of the user from the COEUS pull
     * @return the updated User - null if the User does not need to be updated
     */
    public User update(User system, User stored) {
        if (!coeusUsersEqual(system, stored)) {
            return updateUser(system, stored);
        }
        return null;
    }

    /**
     *  This method takes a COEUS Grant, calculates whether it needs to be updated, and if so, returns the updated object
     *  to be be ingested into the repository. if not, returns null.
     * @param stored the Grant as it is stored in the PASS backend
     * @param system the version of the Grant from the COEUS pull
     * @return the updated object - null if the Grant does not need to be updated
     */
    public Grant update(Grant system, Grant stored) {
        if (!coeusGrantsEqual(system, stored)) {
            return updateGrant(system, stored);
        }
        return null;
    }

    /**
     * Compare two Funder objects
     *
     * @param system the version of the Funder as seen in the COEUS system pull
     * @param stored the version of the Funder as read from Pass
     * @return a boolean which asserts whether the two supplied Funders are "COEUS equal"
     */
    private boolean coeusFundersEqual(Funder system, Funder stored) {

        //this adjustment handles the case where we take data from policy.properties file, which has no name info
        if (system.getName() != null ? !system.getName().equals(stored.getName()) : false ) return false;
        if (system.getLocalKey() != null ? !system.getLocalKey().equals(stored.getLocalKey()) : stored.getLocalKey() != null) return false;
        if (system.getPolicy() != null ? !system.getPolicy().equals(stored.getPolicy()) : stored.getPolicy() != null) return false;
        return true;
    }

    /**
     * Update a Pass Funder object with new information from COEUS
     *
     * @param system the version of the Funder as seen in the COEUS system pull
     * @param stored the version of the Funder as read from Pass
     * @return the Funder object which represents the Pass object, with any new information from COEUS merged in
     */
    private Funder updateFunder (Funder system, Funder stored) {
        stored.setLocalKey(system.getLocalKey());
        stored.setName(system.getName());
        if (system.getPolicy() != null) { stored.setPolicy(system.getPolicy()); }
        return stored;
    }

    /**
     * Compare two User objects. We only care about those fields for which COEUS is the authoritative source
     * After recent changes. this method would be more accurately named "storedUserDoesNotNeedToBeUpdated"
     *
     * @param system the version of the User as seen in the COEUS system pull
     * @param stored the version of the User as read from Pass
     * @return a boolean which asserts whether the two supplied Users are "COEUS equal"
     */
    private  boolean coeusUsersEqual(User system, User stored) {
        //first the fields for which COEUS is authoritative
        if (system.getFirstName() != null ? !system.getFirstName().equals(stored.getFirstName()) : stored.getFirstName() != null) return false;
        if (system.getMiddleName() != null ? !system.getMiddleName().equals(stored.getMiddleName()) : stored.getMiddleName() != null) return false;
        if (system.getLastName() != null ? !system.getLastName().equals(stored.getLastName()) : stored.getLastName() != null) return false;
        if (system.getLocatorIds() != null? !stored.getLocatorIds().containsAll(system.getLocatorIds()): stored.getLocatorIds() != null) return false;
        //next, other fields which require some reasoning to decide whether an system is necessary
        if (system.getEmail() != null && stored.getEmail() == null) return false;
        if (system.getDisplayName() != null && stored.getDisplayName() == null) return false;
        return true;
    }

    /**
     * Update a Pass User object with new information from COEUS. We check only those fields for which COEUS is
     * authoritative. Other fields will be managed by other providers (Shibboleth for example). The exceptions are
     * the localKey, which this application and Shibboleth both rely on; and  email, which this application only populates
     * if Shib hasn't done so already.
     *
     * @param system the version of the User as seen in the COEUS system pull
     * @param stored the version of the User as read from Pass
     * @return the User object which represents the Pass object, with any new information from COEUS merged in
     */
    private User updateUser (User system, User stored) {
        stored.setFirstName(system.getFirstName());
        stored.setMiddleName(system.getMiddleName());
        stored.setLastName(system.getLastName());
        //combine the locatorIds from both objects
        Set<String> idSet = new HashSet<>();
        idSet.addAll(stored.getLocatorIds());
        idSet.addAll(system.getLocatorIds());
        stored.setLocatorIds(idSet.stream().collect(Collectors.toList()));
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
     * Compare two Grant objects. Note that the Lists of Co-Pis are compared as Sets
     * @param system the version of the Grant as seen in the COEUS system pull
     * @param stored the version of the Grant as read from Pass
     * @return a boolean which asserts whether the two supplied Grants are "COEUS equal"
     */
    private boolean coeusGrantsEqual(Grant system, Grant stored) {
        if (system.getAwardNumber() != null ? !system.getAwardNumber().equals(stored.getAwardNumber()) : stored.getAwardNumber() != null) return false;
        if (system.getAwardStatus() != null? !system.getAwardStatus().equals(stored.getAwardStatus()) : stored.getAwardStatus() != null) return false;
        if (system.getLocalKey() != null? !system.getLocalKey().equals(stored.getLocalKey()) : stored.getLocalKey() != null) return false;
        if (system.getProjectName() != null? !system.getProjectName().equals(stored.getProjectName()) : stored.getProjectName() != null) return false;
        if (system.getPrimaryFunder() != null? !system.getPrimaryFunder().equals(stored.getPrimaryFunder()) : stored.getPrimaryFunder() != null) return false;
        if (system.getDirectFunder() != null? !system.getDirectFunder().equals(stored.getDirectFunder()) : stored.getDirectFunder() != null) return false;
        if (system.getPi() != null? !system.getPi().equals(stored.getPi()) : stored.getPi() != null) return false;
        if (system.getCoPis() != null? !new HashSet(system.getCoPis()).equals(new HashSet(stored.getCoPis())): stored.getCoPis() != null) return false;
        if (system.getAwardDate() != null? !system.getAwardDate().equals(stored.getAwardDate()) : stored.getAwardDate() != null) return false;
        if (system.getStartDate() != null? !system.getStartDate().equals(stored.getStartDate()) : stored.getStartDate() != null) return false;
        if (system.getEndDate() != null? !system.getEndDate().equals(stored.getEndDate()) : stored.getEndDate() != null) return false;
        return true;
    }

    /**
     * Update a Pass Grant object with new information from COEUS
     *
     * @param system the version of the Grant as seen in the COEUS system pull
     * @param stored the version of the Grant as read from Pass
     * @return the Grant object which represents the Pass object, with any new information from COEUS merged in
     */
    private Grant updateGrant(Grant system, Grant stored) {
        stored.setAwardNumber(system.getAwardNumber());
        stored.setAwardStatus(system.getAwardStatus());
        stored.setLocalKey(system.getLocalKey());
        stored.setProjectName(system.getProjectName());
        stored.setPrimaryFunder(system.getPrimaryFunder());
        stored.setDirectFunder(system.getDirectFunder());
        stored.setPi(system.getPi());
        stored.setCoPis(system.getCoPis());
        stored.setAwardDate(system.getAwardDate());
        stored.setStartDate(system.getStartDate());
        stored.setEndDate(system.getEndDate());
        return stored;
    }


}
