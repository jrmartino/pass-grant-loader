# PASS Grant Loader
This repo contains code that, when executed, will query the JHU institutional grant database for desired data and load
it into local persistent storage

## Description of Operation
The tool is invoked from the command line to query several views in 
the JHU COEUS grant management system to pull the records of all 
grants which have been updated since a certain timestamp. The normal path 
is that the timestamp is read from the last line of a file which 
records all such timestamps after successfully completed runs. 
Optionally, a timestamp may be supplied as the -s option on the command line.

The ResultSet which is returned from COEUS is transformed into an intermediate List
of data structures which contain the information we require to update our grants.
We finally use our java Fedora client to merge this update information with existing 
Grant information in Fedora to have the Fedora repository reflect the current information obtained
in the update. Auxilliary objects belongng to the Grant (Persons and Funders) 
will also receive updates.

Because the application is intended to run unattended in slack hours, we
send email notification indicating success, or reporting errors which prevented the 
application from performing an update.

## Configuration
The user running this application must supply a full path to a home
or base directory for the application. This directory will contain any 
configuration files, and will also contain the log file and the file containing the 
update timestamps. Necessary configuration files are as follows.

### COEUS connection properties file (connection.properties)

This file must contain the values for the URL, user, and password needed to
attach to COEUS's Oracle database. The URL for a database used with the Oracle driver typically looks like 
this: jdbc:oracle:thin:@host.name.institution:1521:service-name


coeus.url = 

coeus.user = 

coeus.password =

### Mail server properties file (mail.properties)
This file contains values for parameters needed to send mail out from the application.
These values suggest using a gmail server for example.

mail.transport.protocol=SMTPS

mail.smtp.starttls.enable=true

mail.smtp.host=smtp.gmail.com

mail.smtp.port=587

mail.smtp.auth=true

mail.smtp.user=

mail.smtp.password=

mail.from=

mail.to=

### Fedora configuration (system.properties)
This file contains parameters which must be set as system properties so that  the java Fedora client 
can configure itself to attach to the desired fedora instance. The base URL must contain
the port number and path to the base container (for example, http://localhost:8080/fcrepo/rest/)

pass.fedora.user=

pass.fedora.password=

pass.fedora.baseurl=

## Invocation
The application is provided as an executable jar file. The absolute path for the base directory COEUS_HOME must be provided as a command line
option to java in order to inject it into the java context. The command line looks like this

java -DCOEUS_HOME=full-path-to-base-directory -jar full-path-to-jar-file 

when we are taking the timestamp as the last line of the update timestamps file, or 

java -DCOEUS_HOME=full-path-to-base-directory -jar full-path-to-jar-file- -s "yyyy-mm-dd hh:mm:ss.0"

when invoking the application to process all updates occurring after the specified timestamps.

For example:

 java -DCOEUS_HOME="/home/luser/coeus" -jar pass-grant-cli-1.0.0-SNAPSHOT-shaded.jar -s "2018-03-29 14:30:00.0"


## Implementation Details

The processing of the ResultSet is straightforward - we simply construct a list of hash maps which represent the 
column names and the values for each record. We do not assume that the PASS objects in Fedora are updated 
only by this application, as there may be some fields on these objects which are not known to COEUS, but 
may be populated by other applications eventually (for example ORCID id on Person, or submissions on Grant). 
So, our approach is, for each grant record, to see if Fedora knows about it yet, and if so, pull back the current version
of the grant. We then look at the hash map and overwrite any information on the existing object with the new 
information. Fields which are themselves PASS objects are also updated. In order to keep processing as efficient
as possible, we do track which PASS objects have been updated in the current session, as some of them may 
appear many times (Funders or Persons for example). We update these only once in the session.

After we have processed each record, we save the state of the Grant objects in a List. After all records 
are processed we know that each Grant object on the List is current, and so we update these grants in Fedora.



