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

import org.junit.Assert;
import org.junit.Test;

import static org.dataconservancy.pass.grant.data.GrantUpdater.returnLaterUpdate;

/**
 * Test class for building the {@code List} of {@code Grant}s
 *
 * @author jrm@jhu.edu
 */
public class GrantUpdaterTest {

    /**
     * Test static timestamp utility method to verify it returns the later of two supplied timestamps
     */
    @Test
    public void testReturnLatestUpdate(){
        String baseString = "1980-01-01 00:00:00.0";
        String earlyDate  = "2018-01-02 03:04:05.0";
        String laterDate  = "2018-01-02 04:08:09.0";

        String latestDate = returnLaterUpdate(baseString, earlyDate);
        Assert.assertEquals(earlyDate, latestDate);
        latestDate = returnLaterUpdate(latestDate, laterDate);
        Assert.assertEquals(laterDate, latestDate);

        Assert.assertEquals(earlyDate, returnLaterUpdate(earlyDate, earlyDate));
    }
}
