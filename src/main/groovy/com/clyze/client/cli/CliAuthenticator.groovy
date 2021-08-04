package com.clyze.client.cli

import groovy.transform.CompileStatic
import org.clyze.utils.FileOps
import org.apache.log4j.Logger

@CompileStatic
class CliAuthenticator {

    private static String username
    private static String token

    static File getDataFile() {
        return new File(System.getProperty('user.home'), '.clyze-client')
    }

    //TODO: Unsafe, yet convenient for the time being
    static void init(){
        String fileName = getDataFile().canonicalPath
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
            String data = username + '\n' + token
            FileOps.writeToFile(getDataFile(), data)
        }
        catch(e) {
            Logger.getRootLogger().error(e.getMessage(), e)
        }
    }

}
