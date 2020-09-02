package org.clyze.client

import groovy.transform.CompileStatic
import static groovy.io.FileType.FILES
import org.clyze.utils.JHelper

/**
 * Analyzes the sources of a project and optionally converts them to
 * UTF-8. This class is to be used by tools using the clyze-client as a
 * library (such as the Gradle plugin and the crawler). The UTF-8
 * conversion addresses the issue of feeding sources with different
 * encodings to the jcPlugin.
 */
@CompileStatic
class SourceProcessor {

    /**
     * Arguments:
     *
     *   convertUTF8 auto-detect charset and convert to UTF-8
     *
     * Returns a map containing the following information:
     *
     * List<String> sourceFiles     the source files found
     * int scalaFilesCount          number of .scala files found
     * int groovyFilesCount         number of .groovy files found
     * int lineCount                line count of source files
     * boolean foundAndroidSource   if Android source was detected (via heuristic)
     */
    static Map<String, Object> process(File sourcesDir, boolean convertUTF8) {
        List<String> sourceFiles = []
        boolean foundAndroidSource = false
        int scalaFilesCount = 0
        int groovyFilesCount = 0
        int lineCount = 0

        sourcesDir.eachFileRecurse(FILES) {
            if (it.name.endsWith(".java")) {
                sourceFiles << it.canonicalPath
                if (convertUTF8) {
                    JHelper.ensureUTF8(it.canonicalPath, false)
                }
                def lines = (new File(it.canonicalPath)).readLines()
                for (String l : lines) {
                    // Heuristic to report creation of dynamic proxies.
                    if (l.contains("newProxyInstance(") && !l.contains("//") && !l.contains("*")) {
                        System.out.println("Dynamic proxies found.")
                    }
                }
                lineCount += lines.size()
                // Heuristic to find Android sources.
                if (!foundAndroidSource && (isAndroidSource(it.canonicalPath))) {
                    foundAndroidSource = true
                }
            } else if (it.name =~ /.*\.scala/) {
                scalaFilesCount++
            } else if (it.name =~ /.*\.groovy/) {
                groovyFilesCount++
            } else {
                println "Ignoring source file: ${it}"
            }
        }
        int sourceFilesCount = sourceFiles.size()
        if (sourceFilesCount == 0) {
            throw new RuntimeException("No Java sources were found.")
        }
        if (scalaFilesCount > sourceFilesCount) {
            throw new RuntimeException("This looks like a Scala project.")
        }
        if (groovyFilesCount > sourceFilesCount) {
            throw new RuntimeException("This looks like a Groovy project.")
        }

        return [ sourceFiles        : sourceFiles,
                 scalaFilesCount    : scalaFilesCount,
                 groovyFilesCount   : groovyFilesCount,
                 lineCount          : lineCount,
                 foundAndroidSource : foundAndroidSource
               ]
    }

    private static boolean isAndroidSource(String path) {
        def lines = (new File(path)).readLines()
        return lines.findAll({ it.startsWith("import android.") }).size() > 0
    }

}
