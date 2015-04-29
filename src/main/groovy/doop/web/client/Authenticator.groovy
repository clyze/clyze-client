package doop.web.client

import doop.core.Helper
import org.apache.log4j.Logger

/**
 * Created by saiko on 29/4/2015.
 */
class Authenticator {

    private static String token

    //TODO: Unsafe, yet convenient for the time being
    static void init(){
        try {
            String userHome = System.getProperty("user.home")
            token = new File("${userHome}/.jdoop-client").text.trim()
        }
        catch(e) {
            Logger.getRootLogger().error(e.getMessage(),e )
        }
    }

    static String getUserToken() {
        return token
    }

    static void setUserToken(String token) {
        Authenticator.token = token
        try {
            String userHome = System.getProperty("user.home")
            Helper.writeToFile(new File("${userHome}/.jdoop-client"), token)
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
