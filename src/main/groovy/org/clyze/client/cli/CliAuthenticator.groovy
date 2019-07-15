package org.clyze.client.cli

import org.clyze.utils.FileOps
import org.apache.log4j.Logger

class CliAuthenticator {

    private static String token

    //TODO: Unsafe, yet convenient for the time being
    static void init(){
        String userHome = System.getProperty("user.home")
        String fileName = "${userHome}/.clue-client"
        try {
            File f = FileOps.findFileOrThrow(fileName, "File invalid: $fileName")
            token = f.text.trim()
        }
        catch(any) {
            Logger.getRootLogger().debug(any.getMessage())
        }
    }

    static String getUserToken() {
        return token
    }

    static void setUserToken(String token) {
        CliAuthenticator.token = token
        try {
            String userHome = System.getProperty("user.home")
            FileOps.writeToFile(new File("${userHome}/.clue-client"), token)
        }
        catch(e) {
            Logger.getRootLogger().error(e.getMessage(), e)
        }
    }

    //TODO: Can be safer if we avoid storing the password as a String
    static Map<String, String> askForCredentials() {
        Console console = System.console()
        if (console) {
            def credentials = [:]
            credentials.username = console.readLine("Username:")
            credentials.password = new String(console.readPassword("Password:"))
            return credentials
        }
        else {
            throw new RuntimeException("Could not get System Console")
        }
    }
}
