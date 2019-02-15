
## Developer Notes
This project has been adapted to be able to build several jars for loading data for loading data into PASS instances for different institutions.
For the sake of efficiency, we do this in one project rather than in several projects. We abuse the shade plugin and provide a separate `<execution>` for each
artifact. Because different jars will have different revision schedules, we control the versioning for each implementation manually. We increment the current version
for each implementation at the end of the `<properties>` section of the main pom file for the project. This is reflected in the `<finalName>` element in the configuration
for the corresponding `<execution>` section for the implementation in the pass-grant-cli pom file.

## Implementations

[JHU COEUS Loader](JHU-README.md)