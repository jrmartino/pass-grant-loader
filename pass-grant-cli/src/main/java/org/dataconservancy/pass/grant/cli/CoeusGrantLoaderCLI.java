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
package org.dataconservancy.pass.grant.cli;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoeusGrantLoaderCLI {

    /**
     * Arguments - just the property file containing the submission elements
     */
    @Argument(required = true, index = 0, metaVar = "[properties file]", usage = "properties file for the COEUS connection")
    public static File propertiesFile = null;

    /**
     *
     * General Options
     */

    /** Request for help/usage documentation */
    @Option(name = "-h", aliases = { "-help", "--help" }, usage = "print help message")
    public boolean help = false;

    /** Requests the current version number of the cli application. */
    @Option(name = "-v", aliases = { "-version", "--version" }, usage = "print version information")
    public boolean version = false;

    @Option(name = "-s", aliases = { "-start", "-startDate", "--start", "--startDate" }, usage = "start date")
    public static String startDate="";

    @Option(name = "-e", aliases = {"-end", "-endDate", "--end", "--endDate"}, usage = "end date")
    public static String endDate="";

    public static void main(String[] args) {

        final CoeusGrantLoaderCLI application = new CoeusGrantLoaderCLI();
        CmdLineParser parser = new CmdLineParser(application);
        //parser.setUsageWidth(80);

        try {
            parser.parseArgument(args);
            /* Handle general options such as help, version */
            if (application.help) {
                parser.printUsage(System.err);
                System.err.println();
                System.exit(0);
            } else if (application.version) {
                System.err.println(CoeusCliException.class.getPackage()
                        .getImplementationVersion());
                System.exit(0);
            } else if (application.startDate.length()>0 && !verifyDateFormat(startDate)){
                System.err.println(startDate + " is not a valid date format. Date must be in the format mm/dd/yyyy");
            } else if (application.endDate.length()>0 && !verifyDateFormat(endDate)) {
                System.err.println(endDate + " is not a valid date format. Date must be in the format mm/dd/yyyy");
            } else if (application.startDate.length() == 0 && application.endDate.length()>0){
                System.err.println("Start date is required when end date is specified");
            }

            /* Run the package generation application proper */
            CoeusGrantLoaderApp app = new CoeusGrantLoaderApp(propertiesFile, startDate, endDate);
            app.run();
            System.exit((0));
        } catch (CmdLineException e) {
            /**
             * This is an error in command line args, just print out usage data
             * and description of the error.
             */
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            System.err.println();
            System.exit(1);
        } catch (CoeusCliException e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }


    /**
     * Dates must be specified in the format mm/dd/yyyy. We only check for this format, and not for validity
     * (for example, 2/31/2018 passes)
     * @param date the date to be checked
     * @return a boolean indicating whether the date matches the mm/dd/yyyy format
     */
    static boolean verifyDateFormat(String date) {
        String regex = "^(1[0-2]|0[1-9])/(3[01]|[12][0-9]|0[1-9])/[0-9]{4}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(date);
        return matcher.matches();
    }

}
