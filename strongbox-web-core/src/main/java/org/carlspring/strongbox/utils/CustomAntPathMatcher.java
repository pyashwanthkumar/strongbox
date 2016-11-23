package org.carlspring.strongbox.utils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

/**
 * Re-implements URL processing to let {path:.+} behaves like **. Also populates necessary path variables.
 *
 * @author Alex Oreshkevich
 */
@Component
public class CustomAntPathMatcher
        extends AntPathMatcher
{

    private static final Logger logger = LoggerFactory.getLogger(CustomAntPathMatcher.class);

    private String pathSeparator;

    // pattern that will be processed in the same way as **
    public final static String TWO_STARS_ANALOGUE = ".+";

    /**
     * Create a new instance with the {@link #DEFAULT_PATH_SEPARATOR}.
     */
    public CustomAntPathMatcher()
    {
        this.pathSeparator = DEFAULT_PATH_SEPARATOR;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Any URL pattern like {PATH_VARIABLE:.+} will be treated as **, it means like any subPath with any numbers of / in it.
     *
     * @param pattern   the pattern to match against
     * @param path      the path String to test
     * @param fullMatch whether a full pattern match is required (else a pattern match
     *                  as far as the given base path goes is sufficient)
     * @return {@code true} if the supplied {@code path} matched, {@code false} if it didn't
     */
    @Override
    protected boolean doMatch(String pattern,
                              String path,
                              boolean fullMatch,
                              Map<String, String> uriTemplateVariables)
    {
        String pathVariableName = null;

        // pattern should ends with ':.+}' char sequence (but not with }.xml or }.json)
        if (pattern.endsWith(":" + TWO_STARS_ANALOGUE + "}") && pattern.lastIndexOf("}") == pattern.length() - 1)
        {

            // extract actual name of the path variable from pattern (and then replace :.+ with **)
            pathVariableName = getPathVariableName(pattern);
            pattern = pattern.replace("{" + pathVariableName + ":" + TWO_STARS_ANALOGUE + "}", "**");
        }

        // get pattern matching result from superclass (the default one)
        // if .+ was present it was replaced by **, so we could reuse base class implementation
        boolean defaultMatchResult = super.doMatch(pattern, path, fullMatch, uriTemplateVariables);

        if (pathVariableName != null && defaultMatchResult)
        {

            if (uriTemplateVariables == null)
            {
                uriTemplateVariables = new LinkedHashMap<>();
            }

            uriTemplateVariables.put(pathVariableName, getPathVariableValue(pattern, path));
        }

        logger.trace("[doMatch] pattern " + pattern + "\n\tpath " + path + "\n\tfullMatch "
                     + fullMatch + "\n\turiTemplateVariables "
                     + uriTemplateVariables + "\n\tdefaultMatchResult " + defaultMatchResult);

        return defaultMatchResult;
    }

    // get actual PATH_VARIABLE_NAME from patterns like /ANY_STRING/ANY_STRING/../{PATH_VARIABLE_NAME:.+}
    private String getPathVariableName(String pattern)
    {

        // get the rest of source path based on the path variables count and path prefix
        String[] patternDirs = pattern.split(pathSeparator);
        int subPathIndex = patternDirs.length;

        return patternDirs[subPathIndex - 1].substring(1, patternDirs[subPathIndex - 1].indexOf(":"));
    }

    // get subPath value that matches actual pattern
    private String getPathVariableValue(String pattern,
                                        String path)
    {

        // get the rest of source path based on the path variables count and path prefix
        String[] pathDirs = path.split(pathSeparator);
        String[] patternDirs = pattern.split(pathSeparator);

        logger.trace("pathDirs " + pathDirs.length + " " + Arrays.deepToString(pathDirs));
        logger.trace("patternDirs " + patternDirs.length + " " + Arrays.deepToString(patternDirs));

        int subPathIndex = patternDirs.length;

        // for cases like /metadata/{storageId}/{repositoryId}/**  and  /metadata/storage0/releases/
        if (pathDirs.length + 1 == subPathIndex)
        {
            return "";
        }

        int subPathLength = 0;
        for (String subPath : pathDirs)
        {
            subPathLength += subPath.length();
            if (--subPathIndex == 0)
            {
                break;
            }
        }

        return path.substring(subPathLength);
    }
}
