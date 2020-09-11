# Clyze Client

This is the Clyze client, a command-line application and library that can contact a [Clyze server](https://clyze.com/) throught a RESTful Web API.

The client uses the Web API to list the projects and builds of a server, trigger analysis/packing, manage configurations, and retrieve results.

## Building the client (command-line application)

To build the command-line version of the client, issue the following command:

    $ ./gradlew distZip

This builds the project and creates the distribution archive in directory `build/distributions`. You can extract this archive to a directory of your choice and run executable `bin/clyze-client` (on Linux, macOS) or `bin/clyze-client.bat` (on Windows).

## Running the client

We can invoke the client by running commands:

    $ INSTALL_DIR>./bin/clyze-client -r [server:port] -c [command]

Some example commands follow, for the full list, run the client without arguments, to see usage instructions.

### Login
Authenticates the user. This is usually the first command to run before interacting with the server.

    $ INSTALL_DIR>./bin/clyze-client -r [server:port] -c login

### Ping
Pings the server (validates that a connection is available).

    $ INSTALL_DIR>./bin/clyze-client -r [server:port] -c ping

### List projects
Lists the projects of the server.

    $ INSTALL_DIR>./bin/clyze-client -r [server:port] -c list_projects

## Local Install

Instead of generating the distribution archive, we can instruct Gradle to install the client directly in our working directory:

    $ ./gradlew installDist

This will create a build/install directory, containing all the client runtime files 
(similar to generating the archive and extracting its files to the build/install directory).

Then we can switch to this directory and invoke the client from there.

## Use as a library

This project can also be built as a library. Run `./gradlew jar` for a JAR output or publish to your mavenLocal() repo via `./gradlew publishToMavenLocal`.

Releases of this library go to https://bintray.com/clyze/clients/clyze-client.

