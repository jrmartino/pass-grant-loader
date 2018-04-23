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

/**
 * A utility class for handling Grants, Users or Funders. One function performed is comparison of two instances of
 * these PASS entity classes. These comparisons are reduced to only those fields which are updatable by
 * data from COEUS, so that two objects are considered "COEUS equal" iff they agree on these fields.
 *
 * Another function performed by this utility class is to construct an updated version of an instance of one of these classes
 * by merging a (possibly) existing Fedora object with new information obtained from a COEUS data pull.
 *
 * @author jrm@jhu.edu
 */
public class PassEntityUtil {

    /**
     * Compare two Funder objects
     *
     * @param update the version of the Funder as seen in the COEUS update pull
     * @param stored the version of the Funder as read from Fedora
     * @return a boolean which asserts whether the two supplied Funders are "COEUS equal"
     */
    public static boolean coeusFundersEqual(Funder update, Funder stored) {

        if (update.getName() != null ? !update.getName().equals(stored.getName()) : stored.getName() != null) return false;
        if (update.getLocalKey() != null ? !update.getLocalKey().equals(stored.getLocalKey()) : stored.getLocalKey() != null) return false;
        return true;
    }

    /**
     * Update a Fedora Funder object with new information from COEUS
     *
     * @param update the version of the Funder as seen in the COEUS update pull
     * @param stored the version of the Funder as read from Fedora
     * @return the Funder object which represents the Fedora object, with any new information from COEUS merged in
     */
    static Funder updateFunder (Funder update, Funder stored) {
        stored.setLocalKey(update.getLocalKey());
        stored.setName(update.getName());
        return stored;
    }

    /**
     * Compare two User objects
     *
     * @param update the version of the User as seen in the COEUS update pull
     * @param stored the version of the User as read from Fedora
     * @return a boolean which asserts whether the two supplied Users are "COEUS equal"
     */
    public static boolean coeusUsersEqual(User update, User stored) {
        if (update.getFirstName() != null ? !update.getFirstName().equals(stored.getFirstName()) : stored.getFirstName() != null) return false;
        if (update.getMiddleName() != null ? !update.getMiddleName().equals(stored.getMiddleName()) : stored.getMiddleName() != null) return false;
        if (update.getLastName() != null ? !update.getLastName().equals(stored.getLastName()) : stored.getLastName() != null) return false;
        if (update.getDisplayName() != null ? !update.getDisplayName().equals(stored.getDisplayName()) : stored.getDisplayName() != null) return false;
        if (update.getEmail() != null ? !update.getEmail().equals(stored.getEmail()) : stored.getEmail() != null) return false;
        if (update.getInstitutionalId() != null ? !update.getInstitutionalId().equals(stored.getInstitutionalId()) : stored.getInstitutionalId() != null) return false;
        if (update.getLocalKey() != null ? !update.getLocalKey().equals(stored.getLocalKey()) : stored.getLocalKey() != null) return false;
        if (update.getRoles() != null ? !stored.getRoles().contains(User.Role.SUBMITTER):stored.getRoles() != null) return false;
        //if (update.getAffiliation() != null ? !update.getAffiliation().equals(stored.getAffiliation()) : stored.getAffiliation() != null) return false;
        return true;
    }

    /**
     * Update a Fedora User object with new information from COEUS
     *
     * @param update the version of the User as seen in the COEUS update pull
     * @param stored the version of the User as read from Fedora
     * @return the User object which represents the Fedora object, with any new information from COEUS merged in
     */
    static User updateUser (User update, User stored) {
        stored.setFirstName(update.getFirstName());
        stored.setMiddleName(update.getMiddleName());
        stored.setLastName(update.getLastName());
        stored.setDisplayName(update.getDisplayName());
        stored.setEmail(update.getEmail());
        stored.setInstitutionalId(update.getInstitutionalId());
        stored.setLocalKey(update.getLocalKey());
        //stored.setAffiliation(update.getAffiliation());
        return stored;
    }

    /**
     * Compare two Grant objects. Note that the Lists of Co-Pis are essentially compared as Sets
     * @param update the version of the Grant as seen in the COEUS update pull
     * @param stored the version of the Grant as read from Fedora
     * @return a boolean which asserts whether the two supplied Grants are "COEUS equal"
     */
    public static boolean coeusGrantsEqual(Grant update, Grant stored) {
        if (update.getAwardNumber() != null ? !update.getAwardNumber().equals(stored.getAwardNumber()) : stored.getAwardNumber() != null) return false;
        if (update.getAwardStatus() != null? !update.getAwardStatus().equals(stored.getAwardStatus()) : stored.getAwardStatus() != null) return false;
        if (update.getLocalKey() != null? !update.getLocalKey().equals(stored.getLocalKey()) : stored.getLocalKey() != null) return false;
        if (update.getProjectName() != null? !update.getProjectName().equals(stored.getProjectName()) : stored.getProjectName() != null) return false;
        if (update.getPrimaryFunder() != null? !update.getPrimaryFunder().equals(stored.getPrimaryFunder()) : stored.getPrimaryFunder() != null) return false;
        if (update.getDirectFunder() != null? !update.getDirectFunder().equals(stored.getDirectFunder()) : stored.getDirectFunder() != null) return false;
        if (update.getPi() != null? !update.getPi().equals(stored.getPi()) : stored.getPi() != null) return false;
        if (update.getCoPis() != null? !(update.getCoPis().size()==stored.getCoPis().size()) || !update.getCoPis().containsAll(stored.getCoPis()) || !stored.getCoPis().containsAll(update.getCoPis()) : stored.getCoPis() != null) return false;
        if (update.getAwardDate() != null? !update.getAwardDate().equals(stored.getAwardDate()) : stored.getAwardDate() != null) return false;
        if (update.getStartDate() != null? !update.getStartDate().equals(stored.getStartDate()) : stored.getStartDate() != null) return false;
        if (update.getEndDate() != null? !update.getEndDate().equals(stored.getEndDate()) : stored.getEndDate() != null) return false;
        return true;
    }

    /**
     * Update a Fedora Grant object with new information from COEUS
     *
     * @param update the version of the Grant as seen in the COEUS update pull
     * @param stored the version of the Grant as read from Fedora
     * @return the Grant object which represents the Fedora object, with any new information from COEUS merged in
     */
    static Grant updateGrant(Grant update, Grant stored) {
        stored.setAwardNumber(update.getAwardNumber());
        stored.setAwardStatus(update.getAwardStatus());
        stored.setLocalKey(update.getLocalKey());
        stored.setProjectName(update.getProjectName());
        stored.setPrimaryFunder(update.getPrimaryFunder());
        stored.setDirectFunder(update.getDirectFunder());
        stored.setPi(update.getPi());
        stored.setCoPis(update.getCoPis());
        stored.setAwardDate(update.getAwardDate());
        stored.setStartDate(update.getStartDate());
        stored.setEndDate(update.getEndDate());
        return stored;
    }

}
