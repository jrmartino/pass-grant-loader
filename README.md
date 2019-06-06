# PASS Grant Loader

This project is comprised of code for retrieving grant data from some kind of data source, and using that data to update the PASS backend. Typically, the data pull 
will populate a data structure, which will then be consumed by a loader class. While this sounds simple in theory, there are several considerations 
which may add to the complexity of implementations. An implementor must first determine data requirements for the use of the data once it has been ingested into PASS,
and then map these requirements to data available from the data source. It may be that additional data from other services may be needed in order to
populate the data structures to be loded into PASS. On the loading side, the implementor may need to support different modes of ingesting data. 
Additional logic may be needed in the data loading apparatus to resolve the fields in the data assembled by the pull process. For example, we will
need to consider that several systems may be updating PASS objects, and that other services may be more authoritative for certain fields than the service 
providing the grant data. The JHU implementation is complex regarding these issues.

## Developer Notes
This project has been adapted to be able to build several jars for loading data for loading data into PASS instances for different institutions.
For the sake of efficiency, we do this in one project rather than in several projects. We abuse the shade plugin and provide a separate `<execution>` for each
artifact. Because different jars will have different revision schedules, we control the versioning for each implementation manually. We increment the current version
for each implementation at the end of the `<properties>` section of the main pom file for the project. This is reflected in the `<finalName>` element in the configuration
for the corresponding `<execution>` section for the implementation in the pass-grant-cli pom file.

## Implementations

### JHU

The JHU implementation is used to pull data from the COEUS Oracle database views for the purpose of performing regular updates.
We look at grants which have been updated since a particular time (typically the time of the previous update), join this
with user and funder information associated with the grant, and then use this information to update the data in the PASS backend.
The JHU implementation also treats the COEUS database as authoritative for certain fields in the data - the logic about whether
updates are required is contained in the PassEntityUtil implementation for JHU. Details about operation are available at
[JHU COEUS Loader](JHU-README.md)