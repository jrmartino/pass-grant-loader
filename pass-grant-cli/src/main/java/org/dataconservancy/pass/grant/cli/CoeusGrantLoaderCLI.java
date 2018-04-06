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

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;


/**
 * This Class manages the command line interaction for the loading and updating processes
 *
 * @author jrm@jhu.edu
 */
public class CoeusGrantLoaderCLI {

    /*
     * General Options
     */

    /** Request for help/usage documentation */
    @Option(name = "-h", aliases = { "-help", "--help" }, usage = "print help message")
    private boolean help = false;

    /** Requests the current version number of the cli application. */
    @Option(name = "-v", aliases = { "-version", "--version" }, usage = "print version information")
    private boolean version = false;

    @Option(name = "-e", aliases = {"-email", "--email"}, usage = "flag to use the internal email server for notification")
    private static boolean email = false;

    /** Specifies a start datetime timestamp for basing the database query */
    @Option(name = "-s", aliases = { "-startDateTime", "--startDateTime" }, usage = "DateTime to start the query against COEUS. This will cause " +
            "a return of all records updated since this DateTime. Syntax must be yyyy-mm-dd hh:mm:ss.m{mm}. This value will override the most recent " +
            "dateTime listed in the updates file.")
    private static String startDate = "";

    /**
     * The main method which parses the command line arguments and options; also reports errors and exit statuses
     * when the {@code CoeusGrantLoaderApp} executes
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        final CoeusGrantLoaderCLI application = new CoeusGrantLoaderCLI();
        CmdLineParser parser = new CmdLineParser(application);

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
            }

            /* Run the package generation application proper */
            CoeusGrantLoaderApp app = new CoeusGrantLoaderApp(startDate, email);
            app.run();
            System.exit((0));
        } catch (CmdLineException e) {
            /*
             * This is an error in command line args, just print out usage data
             *and description of the error.
             * */
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

}
