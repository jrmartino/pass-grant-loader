#PASS Grant Loader

This project is comprised of code for retrieving grant data from some kind of data source, and using that data to update the PASS backend.


## Developer Notes
This project has been adapted to be able to build several jars for loading data for loading data into PASS instances for different institutions.
For the sake of efficiency, we do this in one project rather than in several projects. We abuse the shade plugin and provide a separate `<execution>` for each
artifact. Because different jars will have different revision schedules, we control the versioning for each implementation manually. We increment the current version
for each implementation at the end of the `<properties>` section of the main pom file for the project. This is reflected in the `<finalName>` element in the configuration
for the corresponding `<execution>` section for the implementation in the pass-grant-cli pom file.

## Implementations

###JHU

The JhUS implementation is used to pull data from Oracle database views for the purpose of performing regular updates.
We look at grants which have been updated since a particular time (typically the time of the previous update), join this
with user and funder information associated with the grant, and then use this information to update the data in the PASS backend.
The JHU implementation also treats the COEUS database as authoritative for certain fields in the data - the logic about whether
updates are required is contained in the PassEntityUtil implementation for JHU. Details about operation are available at
[JHU COEUS Loader](JHU-README.md)