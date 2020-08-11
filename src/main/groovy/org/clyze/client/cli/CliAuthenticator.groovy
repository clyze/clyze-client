package org.clyze.client.cli

import groovy.transform.CompileStatic
import org.clyze.utils.FileOps
import org.apache.log4j.Logger

@CompileStatic
class CliAuthenticator {

    private static String username
    private static String token

    //TODO: Unsafe, yet convenient for the time being
    static void init(){
        String userHome = System.getProperty("user.home")
        String fileName = "${userHome}/.clue-client"
        try {
            File f = FileOps.findFileOrThrow(fileName, "File invalid: $fileName")
            def data = f.text.trim().split('\n')
            username = data[0]
            token = data[1]
        }  catch(any) {
            Logger.getRootLogger().debug(any.getMessage())
        }
    }

    enum Selector { TOKEN, USERNAME }
    static String getUserInfo(Selector s) {
        if (s == Selector.TOKEN) {
            return token
        } else if (s == Selector.USERNAME) {
            return username
        } else {
            throw new RuntimeException('Internal error: selector is invalid: ' + s)
        }
    }

    static void setUserInfo(String username, String token) {
        CliAuthenticator.username = username
        CliAuthenticator.token = token
        try {
            String userHome = System.getProperty("user.home")
            String data = username + '\n' + token
            FileOps.writeToFile(new File("${userHome}/.clue-client"), data)
        }
        catch(e) {
            Logger.getRootLogger().error(e.getMessage(), e)
        }
    }

    //TODO: Can be safer if we avoid storing the password as a String
    static Map<String, String> askForCredentials() {
        Console console = System.console()
        if (console) {
            def credentials = [:] as Map<String, String>
            credentials.put('username', console.readLine("Username: "))
            credentials.put('password', new String(console.readPassword("Password: ")))
            return credentials
        } else {
            throw new RuntimeException("Could not get System Console")
        }
    }
}
