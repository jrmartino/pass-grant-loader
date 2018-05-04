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
import org.joda.time.DateTimeZone;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This utility class provides static methods for intermunging Joda DateTime objects and timestamp strings
 */
public class DateTimeUtil {

    /**
     * A method to convert a timestamp string from our database to a Joda DateTime object
     * @param sqlDateTime the timestamp string
     * @return the corresponding DataTime object
     */
    static DateTime createJodaDateTime(String sqlDateTime) {

        if(sqlDateTime != null) {
            DateTime dateTime;

            String[] parts = sqlDateTime.split(" ");

            //do we have time parts? the timestamp form is yyyy-mm-dd hh:mm:ss.m
            if (parts.length > 1) {
                String date = parts[0]; //yyyy-mm-dd
                String[] dateParts = date.split("-");
                int year = Integer.parseInt(dateParts[0]);
                int month = Integer.parseInt(dateParts[1]);
                int day = Integer.parseInt(dateParts[2]);
                String time = parts[1]; //hh:mm:ss.m{mm}
                String[] timeParts = time.split(":");
                int hour = Integer.parseInt(timeParts[0]);
                int minute = Integer.parseInt(timeParts[1]);
                String[] secondParts = timeParts[2].split("\\.");
                int second = Integer.parseInt(secondParts[0]);
                int millisecond = Integer.parseInt(secondParts[1]);//seems to be always 0 in our data
                dateTime = new DateTime(year, month, day, hour, minute, second, millisecond, DateTimeZone.UTC);
            } else {//we have just a date - date format is mm/day/year
                parts = sqlDateTime.split("/");
                int month = Integer.parseInt(parts[0]);
                int day = Integer.parseInt(parts[1]);
                int year = Integer.parseInt(parts[2]);
                dateTime = new DateTime(year, month, day, 0, 0, DateTimeZone.UTC);
            }
            return dateTime;
        } else {
            return null;
        }
    }

    /**
     * Dates must be specified in the format "yyyy-mm-dd hh:mm:ss.m{mm}" . We only check for this format, and not for validity
     * (for example, "2018-02-31 ... " passes)
     * @param date the date to be checked
     * @return a boolean indicating whether the date matches the required format
     */
    public static boolean verifyDateTimeFormat(String date) {
        String regex = "^[0-9]{4}-(1[0-2]|0[1-9])-(3[01]|[12][0-9]|0[1-9]) ([2][0-3]|[01][0-9]):[0-5][0-9]:[0-5][0-9]\\.[0-9]{1,3}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(date);
        return matcher.matches();
    }

}
