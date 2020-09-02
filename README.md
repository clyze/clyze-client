# Readme

This is the readme file for the doop client - a command line application for contacting a doop server through its Restful web API.

The client uses the doop Web API to:

* list the analyses of a remote server,
* get the information of a remote analysis,
* post a new analysis to the remote server,
* start a remote analysis,
* stop a remote analysis,
* (re)run the post processor of the remote analysis,
* reset (cleanup) a remote analysis,
* restart a remote analysis,
* query a remote analysis,
* delete a remote analysis.

## Directory Structure

The project contains the following directories:

* gradle: contains the gradle wrapper (gradlew) files.
* src: the source files of the project.

It also contains the gradle build files (build.gradle and settings.gradle) and the gradle invocation scripts (gradlew and gradlew.bat).

## Building the client

Building the project refers to generating the runtime/distribution artifacts of the client.

To do so, we issue the following:

    $ ./gradlew distZip

This builds the project and creates the doop client distribution zip in the build/distributions directory.

We can also issue the following:

    $ ./gradlew distTar

to create a tarball instead of a zip in the build/distributions directory.

## Installing the client

To install the doop client, we need to extract the distribution zip or tarball in a directory of our choice.

## Running the client

We can invoke the doop client by issuing:

    $ INSTALL_DIR>./bin/clyze-client -r [server:port] -c [command]

The available commands are the following.

### Login
Authenticates the user by the remote server. This is usually the first command to run before interacting with the server.

    $ INSTALL_DIR>./bin/clyze-client -r [server:port] -c login

### Ping
Pings the remote server (validates that a connection is available).

    $ INSTALL_DIR>./bin/clyze-client -r [server:port] -c ping

### List
Lists the analyses of the remote server.

    $ INSTALL_DIR>./bin/clyze-client -r [server:port] -c list

### Get
Gets the main information of an analysis of the remote server.

    $ INSTALL_DIR>./bin/clyze-client -r [server:port] -c get -id [analysis-id]

### Post
Posts a new analysis to the remote server.

    $ INSTALL_DIR>./bin/clyze-client -r [server:port] -c post -a context-insensitive -j [jar]

Use the -h flag to see the available options for creating a new analysis.

### Start
Starts an analysis on the remote server.

    $ INSTALL_DIR>./bin/clyze-client -r [server:port] -c start -id [analysis-id]

### Stop
Stops the execution of an analysis running on the remote server.

    $ INSTALL_DIR>./bin/clyze-client -r [server:port] -c stop -id [analysis-id]

### Post process
Runs (or re-runs) the post processor of an analysis.

    $ INSTALL_DIR>./bin/clyze-client -r [server:port] -c post_process -id [analysis-id]

### Reset
Resets (cleans-up) an analysis running on the remote server.

    $ INSTALL_DIR>./bin/clyze-client -r [server:port] -c reset -id [analysis-id]
    
### Restart
Restarts an analysis running on the remote server.

    $ INSTALL_DIR>./bin/clyze-client -r [server:port] -c restart -id [analysis-id]

### Query
Queries the results of an analysis that has completed its execution on the remote server.

    $ INSTALL_DIR>./bin/clyze-client -r [server:port] -c query -id [analysis-id] -q [datalog query] -p [optional print opt]

### Delete
Deletes an analysis from the remote server.

    $ INSTALL_DIR>./bin/clyze-client -r [server:port] -c delete -id [analysis-id]

## Local Install

Instead of generating the zip or tarball, we can instruct Gradle to install the doop client directly in our working directory:

    $ ./gradlew installDist

This will create a build/install directory, containing all the doop client runtime files 
(similar to generating the zip or tarball and extracting its files to the build/install directory).

Then we can switch to this directory and invoke the client from there.

## Run Directly

This is the most convenient way to invoke the doop client. We issue:

    $ ./gradlew run -Pargs="doop-client-command-line-arguments"

For example, the following invocation:

    $ ./gradlew run -Pargs="-c post -h"

will show the options supported for the post command.

