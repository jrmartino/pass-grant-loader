package org.dataconservancy.pass.grant.data;

/**
 * constants class containing the column names for fields we need for our COEUS data pulls
 * the names reflect the mapping to our model.
 *
 * the values come from four views in the COEUS database; and the aliases are used in the query string in
 * {@code CoeusConnector} and to refer to the columns in the ResultSet in {@code GrantUpdater}
 *
 * COEUS.JHU_FACULTY_FORCE_PROP aliased to A
 * COEUS.JHU_FACULTY_FORCE_PRSN aliased to B
 * COEUS.JHU_FACULTY_FORCE_PRSN_DETAIL aliased to C
 * COEUS.SWIFT_SPONSOR aliased to D
 *
 * these are consumed in the {@code CoeusConnector} class for the pull from COEUS, and in the {@code GrantUpdater} class
 * for the push into Fedora
 *
 * @author jrm@jhu.edu
 */
class CoeusFieldNames {

    static final String C_GRANT_AWARD_NUMBER = "A.AWARD_ID";
    static final String C_GRANT_AWARD_STATUS ="A.AWARD_STATUS";
    static final String C_GRANT_LOCAL_AWARD_ID ="A.GRANT_NUMBER";
    static final String C_GRANT_PROJECT_NAME ="A.TITLE";
    static final String C_GRANT_AWARD_DATE = "A.AWARD_DATE";
    static final String C_GRANT_START_DATE = "A.START_DATE";
    static final String C_GRANT_END_DATE = "A.AWARD_END";

    static final String C_DIRECT_FUNDER_LOCAL_ID = "A.SPOSNOR_CODE";// misspelling in COEUS view
    static final String C_DIRECT_FUNDER_NAME = "A.SPONSOR";
    static final String C_PRIMARY_FUNDER_LOCAL_ID = "D.SPONSOR_CODE";
    static final String C_PRIMARY_FUNDER_NAME = "D.SPONSOR_NAME";


    static final String C_PERSON_FIRST_NAME = "C.FIRST_NAME";
    static final String C_PERSON_MIDDLE_NAME = "C.MIDDLE_NAME";
    static final String C_PERSON_LAST_NAME = "C.LAST_NAME";
    static final String C_PERSON_EMAIL = "C.EMAIL_ADDRESS";
    static final String C_PERSON_INSTITUTIONAL_ID = "C.JHED_ID";
    //static final String C_PERSON_AFFILIATION = "";
    //static final String C_PERSON_ORCID_ID = "";

    //these fields are accessed for processing, but are not mapped to PASS objects
    static final String C_UPDATE_TIMESTAMP = "A.UPDATE_TIMESTAMP";
    static final String C_ABBREVIATED_ROLE = "B.ABBREVIATED_ROLE";

}
