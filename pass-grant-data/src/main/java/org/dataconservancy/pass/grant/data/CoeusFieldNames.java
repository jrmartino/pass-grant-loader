package org.dataconservancy.pass.grant.data;

/**
 * constants class containing the column names for fields we need for our COEUS data pulls
 * the names reflect the mapping to our model.
 *
 * these come from four views in the COUEUS database, and the aliases used in the query string in CoeusConnector:
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

    static final String C_GRANT_AWARD_NUMBER = "AWARD_ID";// view A
    static final String C_GRANT_AWARD_STATUS ="AWARD_STATUS";// view A
    static final String C_GRANT_LOCAL_AWARD_ID ="GRANT_NUMBER";// view A
    static final String C_GRANT_PROJECT_NAME ="TITLE";// view A
    static final String C_GRANT_AWARD_DATE = "AWARD_DATE";// view A
    static final String C_GRANT_START_DATE = "START_DATE";// view A
    static final String C_GRANT_END_DATE = "AWARD_END";// view A

    static final String C_DIRECT_FUNDER_LOCAL_ID = "SPOSNOR_CODE";// view A; misspelling in COEUS view
    static final String C_DIRECT_FUNDER_NAME = "SPONSOR";// view A
    static final String C_PRIMARY_FUNDER_LOCAL_ID = "SPONSOR_CODE";// view D
    static final String C_PRIMARY_FUNDER_NAME = "SPONSOR_NAME";// view D


    static final String C_PERSON_FIRST_NAME = "FIRST_NAME";// view C
    static final String C_PERSON_MIDDLE_NAME = "MIDDLE_NAME";// view C
    static final String C_PERSON_LAST_NAME = "LAST_NAME";// view C
    static final String C_PERSON_EMAIL = "EMAIL_ADDRESS";// view C
    static final String C_PERSON_INSTITUTIONAL_ID = "JHED_ID";// view C
    //static final String C_PERSON_AFFILIATION = "";
    //static final String C_PERSON_ORCID_ID = "";

    //these fields are accessed for processing, but are not mapped to PASS objects
    static final String C_UPDATE_TIMESTAMP = "UPDATE_TIMESTAMP";// view A
    static final String C_ABBREVIATED_ROLE = "ABBREVIATED_ROLE";// view B

}
