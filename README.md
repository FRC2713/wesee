# WeSee 2017
iRaiders (FRC Team 2713)'s vision coprocessor code for the 2017 season.

## Building
Building requires JDK 8 to be installed on the host machine. The
`arm-linux-gnueabihf` GCC toolchain is required in order to build for
the Raspberry Pi. Build using the command `gradlew
-Pjni.include.dir=$JAVA_HOME/include -Ptarget.platform=host assemble`.
This will create a zip file and tarball in `build/distributions`.

`$JAVA_HOME/include` should be replaced, if JAVA_HOME is unset, with
the path including `jni.h`, and `host` should be replaced with `pi` in
order to build for the Raspberry Pi.

## Deploying
Copy the newly built zip/tar to a Raspberry Pi (model B generation 2 or higher)
with OpenCV 3.2.0 and its Java bindings installed at `/usr/local`. Unzip/untar
the file and run using the script `bin/wesee`.
