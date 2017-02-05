# WeSee 2017
iRaiders (FRC Team 2713)'s vision coprocessor code for the 2017 season.

## Building
OpenCV 3.2.0 and its Java bindings must be installed to `/usr/local` on
the host. All other dependencies are managed through Gradle.

Build using the command `gradlew assemble`. This will create a
zip file and tarball in `build/distributions`.

## Deploying
Copy the newly built zip/tar to a Raspberry Pi (model B generation 2 or higher)
with OpenCV 3.2.0 and its Java bindings installed. Unzip/untar the file and run
using the script `bin/wesee`.
