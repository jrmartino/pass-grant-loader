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

import org.joda.time.DateTime;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
 public class DateTimeUtil {

    public static DateTime createJodaDateTime(String sqlDateTime) {
        String[] parts = sqlDateTime.split(" ");
        String date = parts[0]; //yyyy-mm-dd
        String time = parts[1]; //hh:mm:ss.m{mm}

        String[] dateParts = date.split("-");
        String year = dateParts[0];
        String month = dateParts[1];
        String day = dateParts[2];

        String[] timeParts = time.split(":");
        String hour = timeParts[0];
        String minute = timeParts[1];

        String[] secondParts = timeParts[2].split("\\.");
        String second = secondParts[0];
        String milliseconds = secondParts[1];

        DateTime dateTime = new DateTime(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day),
                Integer.parseInt(hour), Integer.parseInt(minute), Integer.parseInt(second), Integer.parseInt(milliseconds));
        return dateTime;

    }

    /**
     * Dates must be specified in the format "yyyy-mm-dd hh:mm:ss.m{mm}" . We only check for this format, and not for validity
     * (for example, "2018-02-31 ... " passes)
     * @param date the date to be checked
     * @return a boolean indicating whether the date matches the required format
     */
    public static boolean verifyDateTimeFormat(String date) {
        //String regex = "^(1[0-2]|0[1-9])/(3[01]|[12][0-9]|0[1-9])/[0-9]{4}$";
        String regex = "^[0-9]{4}-(1[0-2]|0[1-9])-(3[01]|[12][0-9]|0[1-9]) ([2][0-3]|[01][0-9]):[0-5][0-9]:[0-5][0-9]\\.[0-9]{1,3}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(date);
        return matcher.matches();
    }

}
