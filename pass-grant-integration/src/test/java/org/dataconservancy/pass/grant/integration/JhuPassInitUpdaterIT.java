    package org.dataconservancy.pass.grant.integration;


    import org.dataconservancy.pass.grant.data.JhuPassInitUpdater;
    import org.dataconservancy.pass.grant.data.PassUpdateStatistics;
    import org.dataconservancy.pass.model.Grant;
    import org.dataconservancy.pass.model.User;
    import org.junit.Test;

    import java.net.URI;
    import java.util.ArrayList;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;

    import static java.lang.Thread.sleep;
    import static org.dataconservancy.pass.grant.data.CoeusFieldNames.*;
    import static org.dataconservancy.pass.grant.data.DateTimeUtil.createJodaDateTime;
    import static org.junit.Assert.*;

    public class JhuPassInitUpdaterIT extends PassUpdaterIT {

        JhuPassInitUpdater passUpdater = new JhuPassInitUpdater(passClient);
        PassUpdateStatistics statistics = passUpdater.getStatistics();

        @Test
        public void  processInitGrantIT() throws InterruptedException {
            List<Map<String, String>> resultSet = new ArrayList<>();

            Map<String, String> piRecord1 = makeBaseRowMap();
            resultSet.add(piRecord1);

            //Add a different user as a co-pi
            //(Map<String, String> startMap, String firstName, String middleName, String lastName,
            // String email, String instId, String employId, String hopkinsId, String abbrRole)
            Map<String, String> coPiRecord1 = changeUserInMap(makeBaseRowMap(), "Albert", "Jones",
                    "Einstein", "aeinst1@jhu.edu", "aeinst1", userEmployeeId2, "WWTTRE", "C" );

            resultSet.add(coPiRecord1);

            passUpdater.updatePass(resultSet, "grant");
            sleep(10000);

            URI passGrantUri = passClient.findByAttribute(Grant.class, "localKey", grantIdPrefix + grantLocalKey1);
            assertNotNull( passGrantUri );
            URI passUser1Uri = passClient.findByAttribute(User.class, "locatorIds", employeeidPrefix + userEmployeeId1 );
            assertNotNull( passUser1Uri );
            URI passUser2Uri = passClient.findByAttribute(User.class, "locatorIds", employeeidPrefix + userEmployeeId2 );
            assertNotNull( passUser2Uri );

            Grant passGrant = passClient.readResource( passGrantUri, Grant.class );

            assertEquals( grantAwardNumber1, passGrant.getAwardNumber() );
            assertEquals( Grant.AwardStatus.ACTIVE, passGrant.getAwardStatus() );
            assertEquals( grantIdPrefix + grantLocalKey1, passGrant.getLocalKey() );
            assertEquals( grantProjectName1, passGrant.getProjectName() );
            assertEquals( createJodaDateTime(grantAwardDate1), passGrant.getAwardDate() );
            assertEquals( createJodaDateTime(grantStartDate1), passGrant.getStartDate() );
            assertEquals( createJodaDateTime(grantEndDate1), passGrant.getEndDate() );
            assertEquals( passUser1Uri, passGrant.getPi() );
            assertEquals( 1, passGrant.getCoPis().size() );
            assertEquals( passUser2Uri, passGrant.getCoPis().get(0) );

            //now do a multi-iteration pull, and check the result

            resultSet.clear();

            //add another CoPi at the second iteration
            Map<String, String> adjustments = new HashMap<>();
            adjustments.put(C_GRANT_AWARD_DATE, grantAwardDate2);
            adjustments.put(C_GRANT_START_DATE, grantStartDate2);
            adjustments.put(C_GRANT_END_DATE, grantEndDate2);
            adjustments.put(C_GRANT_PROJECT_NAME, grantProjectName2);
            adjustments.put(C_GRANT_AWARD_NUMBER, grantAwardNumber2);
            adjustments.put(C_UPDATE_TIMESTAMP, grantUpdateTimestamp2);

            Map<String, String> piRecord2 = makeAdjustedBaseRowMap( adjustments );
            Map<String, String> coPiRecord2 =  makeAdjustedRowMap( coPiRecord1, adjustments );
            Map<String, String> newCoPiRecord2 = changeUserInMap( coPiRecord2, "Junie", "Beatrice",
                    "Jones", "jjones1@jhu.edu", "ajjones1", userEmployeeId3, "THIHKX", "C" );

            //change PI at the third iteration
            adjustments.clear();
            adjustments.put(C_GRANT_AWARD_DATE, grantAwardDate3);
            adjustments.put(C_GRANT_START_DATE, grantStartDate3);
            adjustments.put(C_GRANT_END_DATE, grantEndDate3);
            adjustments.put(C_GRANT_PROJECT_NAME, grantProjectName3);
            adjustments.put(C_GRANT_AWARD_NUMBER, grantAwardNumber3);
            adjustments.put(C_UPDATE_TIMESTAMP, grantUpdateTimestamp3);

            //user 2 is the new PI, drop user 1;  user 1 should show up as a co-pi
            Map<String, String> piRecord3 = makeAdjustedBaseRowMap( adjustments );
            Map<String, String> newPiRecord3 = changeUserInMap(piRecord3, "Albert", "Jones",
                    "Einstein", "aeinst1@jhu.edu", "aeinst1", userEmployeeId2,
                    "WWTTRE", "P" );
            Map<String, String> newCoPiRecord3 = makeAdjustedRowMap(newCoPiRecord2, adjustments);

            //in the initial pull, we will findall of the records (check?)
            resultSet.add(piRecord1);
            resultSet.add(coPiRecord1);
            resultSet.add(coPiRecord2);
            resultSet.add(newCoPiRecord2);
            resultSet.add(newPiRecord3);
            resultSet.add(newCoPiRecord3);

            passUpdater.updatePass(resultSet, "grant");
            sleep(10000);

            passGrant = passClient.readResource( passGrantUri, Grant.class );

            URI passUser3Uri = passClient.findByAttribute(User.class, "locatorIds", employeeidPrefix + userEmployeeId3 );
            assertNotNull( passUser3Uri );

            assertEquals( grantAwardNumber1, passGrant.getAwardNumber() );
            assertEquals( Grant.AwardStatus.ACTIVE, passGrant.getAwardStatus() );
            assertEquals( grantIdPrefix + grantLocalKey1, passGrant.getLocalKey() );
            assertEquals( grantProjectName1, passGrant.getProjectName() );
            assertEquals( createJodaDateTime(grantAwardDate1), passGrant.getAwardDate() );
            assertEquals( createJodaDateTime(grantStartDate1), passGrant.getStartDate() );
            assertEquals( createJodaDateTime(grantEndDate3), passGrant.getEndDate() );
            assertEquals( passUser2Uri, passGrant.getPi() );
            assertEquals( 2, passGrant.getCoPis().size() );
            assertTrue( passGrant.getCoPis().contains(passUser1Uri) );
            assertTrue( passGrant.getCoPis().contains(passUser3Uri) );
        }
    }
