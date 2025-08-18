# Purpose

This document describes the complete process for releasing a new version of the FEntwumS NetlistReaderBackend.

# Creating a new release

1. Modify the `<version>` field in `pom.xml` to reflect the version that is intended to be released
2. Modify the `<parent.version>` field in the `pom.xml` of all modules (e.g. `fentwums-netlist-reader-server/pom.xml`)
to contain the new releases version
3. Commit the changes to master
4. Tag the commit using the following scheme `v<major>.<minor>.<patch>`
5. Push the commit and tag to GitHub
6. Create a new release on https://github.com/FEntwumS/NetlistReaderBackend/releases
   1. Use the just created tag as base for the release
   2. Use the tag of the previous release as comparison
   3. Release notes may be auto-generated or manually written
   4. Make sure the new release is tagged as the latest release
7. Wait until the release pipeline has finished building and publishing the release artifact
8. If the release pipeline successfully finishes, add a new entry to the `NetlistViewerBackendPackage.Versions` field in
`src/FEntwumS.NetlistViewer/FEntwumSNetlistReaderFrontendModule.cs` in the repository containing the FEntwumS Netlist
Viewer Extension for OneWare Studio (https://github.com/FEntwumS/FEntwumS.NetlistViewer)

# Updating the dependency license overrides

Since some of the dependencies used in this project are available under multiple licenses, overrides are used to
identify under which license these dependencies are used here.
These overrides are stored in `fentwums-netlist-reader-server/src/license/override-THIRD-PARTY.properties`.
Updates to the dependencies listed in the override file will necessitate a modification of their override.
The build will fail if the overrides are not updated.
The output produced during the build will warn about all packages using a "*forbidden*" license.
The overrides will be modified or added for these packages.