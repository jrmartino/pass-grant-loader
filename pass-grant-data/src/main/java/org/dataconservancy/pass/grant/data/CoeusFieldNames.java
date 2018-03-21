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
public class CoeusFieldNames {

    public static final String C_GRANT_AWARD_NUMBER = "AWARD_ID";// view A
    public static final String C_GRANT_AWARD_STATUS="AWARD_STATUS";// view A
    public static final String C_GRANT_LOCAL_AWARD_ID="GRANT_NUMBER";// view A
    public static final String C_GRANT_PROJECT_NAME="TITLE";// view A
    public static final String C_GRANT_AWARD_DATE = "AWARD_DATE";// view A
    public static final String C_GRANT_START_DATE = "START_DATE";// view A
    public static final String C_GRANT_END_DATE = "AWARD_END";// view A

    public static final String C_DIRECT_FUNDER_LOCAL_ID = "SPOSNOR_CODE";// view A; misspelling in COEUS view
    public static final String C_DIRECT_FUNDER_NAME = "SPONSOR";// view A
    public static final String C_PRIMARY_FUNDER_LOCAL_ID = "SPONSOR_CODE";// view D
    public static final String C_PRIMARY_FUNDER_NAME = "SPONSOR_NAME";// view D


    public static final String C_PERSON_FIRST_NAME = "FIRST_NAME";// view C
    public static final String C_PERSON_MIDDLE_NAME = "MIDDLE_NAME";// view C
    public static final String C_PERSON_LAST_NAME = "LAST_NAME";// view C
    public static final String C_PERSON_EMAIL = "EMAIL_ADDRESS";// view C
    public static final String C_PERSON_INSTITUTIONAL_ID = "JHED_ID";// view C
    //public static final String C_PERSON_AFFILIATION = "";
    //public static final String C_PERSON_ORCID_ID = "";

    //these fields are accessed for processing, but are not mapped to PASS objects
    public static final String C_UPDATE_TIMESTAMP = "UPDATE_TIMESTAMP";// view A
    public static final String C_ABBREVIATED_ROLE = "ABBREVIATED_ROLE";// view B

}
