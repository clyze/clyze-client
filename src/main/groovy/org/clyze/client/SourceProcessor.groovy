package org.clyze.client

import static groovy.io.FileType.FILES
import java.io.FileInputStream
import java.nio.charset.Charset
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.file.Path
import java.nio.file.Files
import org.apache.commons.io.IOUtils
import org.mozilla.universalchardet.UniversalDetector

/**
 * Analyzes the sources of a project and optionally converts them to
 * UTF-8. This class is to be used by tools using the clue-client as a
 * library (such as the Gradle plugin and the crawler). The UTF-8
 * conversion addresses the issue of feeding sources with different
 * encodings to the jcPlugin.
 */
class SourceProcessor {

    // Encoding detector.
    UniversalDetector detector = new UniversalDetector(null);

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
    public Map<String, Object> process(File sourcesDir, boolean convertUTF8) {
        List<String> sourceFiles = []
        boolean foundAndroidSource = false
        String baseSrcPath = sourcesDir.canonicalPath + File.separator
        int scalaFilesCount = 0
        int groovyFilesCount = 0
        int lineCount = 0

        sourcesDir.eachFileRecurse(FILES) {
            if (it.name.endsWith(".java")) {
                sourceFiles << it.canonicalPath
                if (convertUTF8) {
                    ensureUTF8(it.canonicalPath)
                }
                def lines = (new File(it.canonicalPath)).readLines()
                for (String l : lines) {
                    // Heuristic to report creation of dynamic proxies.
                    if (l.contains("newProxyInstance(") && !l.contains("//") && !l.contains("*")) {
                        System.out.println("Dynamic proxies found.");
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
            throw new RuntimeException("This looks like a Scala project.");
        }
        if (groovyFilesCount > sourceFilesCount) {
            throw new RuntimeException("This looks like a Groovy project.");
        }

        return [ sourceFiles        : sourceFiles,
                 scalaFilesCount    : scalaFilesCount,
                 groovyFilesCount   : groovyFilesCount,
                 lineCount          : lineCount,
                 foundAndroidSource : foundAndroidSource
               ]
    }

    // If the given file is not encoded as UTF-8, its encoding is
    // detected and its contents are converted to UTF-8.
    private void ensureUTF8(String filename) {
        FileInputStream fis = new FileInputStream(filename)

        byte[] buf = new byte[4096];
        int nread;
        while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
            detector.handleData(buf, 0, nread);
        }
        detector.dataEnd()

        String encoding = detector.getDetectedCharset()
        detector.reset()
        fis.close()

        if ((encoding == null) || (encoding.equals("UTF-8"))) {
            return
        }

        // Try to convert source file to UTF-8.
        try {
            Charset sourceEncoding = Charset.forName(encoding)
            Charset targetEncoding = Charset.forName("UTF-8")
            byte[] buf2 = IOUtils.toByteArray(new FileInputStream(filename))
            CharBuffer data = sourceEncoding.decode(ByteBuffer.wrap(buf2))
            ByteBuffer outBuf = targetEncoding.encode(data)
            def bufWriter = new BufferedWriter(new FileWriter(filename))
            int outDataLength = 0
            while (outBuf.remaining() > 0) {
                bufWriter.write(outBuf.get())
                outDataLength++
                    }
            bufWriter.close()
            println "Converted ${encoding} to UTF-8: ${filename}, ${buf2.length} vs ${outDataLength} bytes"
        } catch (Exception ex) {
            ex.printStackTrace()
            throw new RuntimeException("Cannot convert encoding " + encoding + " to UTF-8")
        }
    }

    private static boolean isAndroidSource(String path) {
        def lines = (new File(path)).readLines()
        return lines.findAll({ it.startsWith("import android.") }).size() > 0
    }

}
