package org.dataconservancy.pass.grant.data;

import org.dataconservancy.pass.model.Funder;
import org.dataconservancy.pass.model.Grant;
import org.dataconservancy.pass.model.User;

public interface PassEntityUtil {

    /**
     *  This method takes a Funder from the data source, calculates whether it needs to be updated, and if so, returns the updated object
     *  to be be ingested into the repository. if not, returns null.
     * @param stored the Funder as it is stored in the PASS backend
     * @param system the version of the Funder from the data sourcee pull
     * @return the updated Funder - null if the Funder does not need to be updated
     *
     */
    Funder update(Funder system, Funder stored);

    /**
     *  This method takes a User from the data source, calculates whether it needs to be updated, and if so, returns the updated object
     *  to be be ingested into the repository. if not, returns null.
     * @param stored the User as it is stored in the PASS backend
     * @param system the version of the User from the data sourcee pull
     * @return the updated User - null if the User does not need to be updated
     *
     */
    User update(User system, User stored);

    /**
     *  This method takes a Grantfrom the data source, calculates whether it needs to be updated, and if so, returns the updated object
     *  to be be ingested into the repository. if not, returns null.
     * @param stored the Grant as it is stored in the PASS backend
     * @param system the version of the Grant from the data sourcee pull
     * @return the updated Grant - null if the Grant does not need to be updated
     *
     */
    Grant update(Grant system, Grant stored);

}
