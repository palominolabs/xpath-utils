package com.palominolabs.xpath;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.strip;

/**
 * Utility class for common XPath operations.
 */
@Immutable
public final class XPathUtils {

    private static final Pattern XPATH_DOUBLE_QUOTE_SPLIT_PATTERN = Pattern.compile("\"");
    private static final Pattern XPATH_SINGLE_QUOTE_SPLIT_PATTERN = Pattern.compile("'");

    private XPathUtils() {
        // no op
    }

    /**
     * Returns an XPath expression that is true if an element has a CSS class. It handles both the case where it is the
     * only CSS class for the element, and the case where it is one of many CSS classes.
     *
     * As an example, you could use it like this to find any element that has the CSS class "foo":
     *
     * <pre>
     * String x = "//*[" + hasCssClass("foo") + "]";
     * </pre>
     *
     * @param cls The CSS class name to check for
     * @return String The XPath expression
     */
    @Nonnull
    public static String hasCssClass(@Nonnull String cls) {
        return "contains(concat(' ', normalize-space(@class), ' '), " + getXPathString(" " + strip(cls, " ") + " ") +
            ")";
    }

    /**
     * <p> Returns an XPath literal of the input string if possible; if not, returns an XPath expression that will match
     * the value. </p>
     *
     * <p> XPath has very limited string literals. They can either be a single-quoted string or a double-quoted string,
     * but there's no concept of escaping, so a single-quoted string cannot contain a single quote. Instead, the string
     * must be broken apart and reassembled using the concat() XPath function. </p>
     *
     * <p> If str contains only single or double quotes (or neither), then the unused type of quote is used to turn the
     * input into a simple string literal. If str contains both, an XPath expression using concat() is generated that
     * evaluates to str. </p>
     *
     * @param str The string
     * @return an XPath string literal or concat expression
     * @see <a href="http://stackoverflow.com/questions/1341847/special-character-in-xpath-query">http://stackoverflow.com/questions/1341847/special-character-in-xpath-query</a>
     */
    @Nonnull
    public static String getXPathString(@Nonnull String str) {
        // if the value contains only single or double quotes, construct
        // an XPath literal
        if (!str.contains("'")) {
            return "'" + str + "'";
        }
        if (!str.contains("\"")) {
            return "\"" + str + "\"";
        }

        // if the value contains both single and double quotes, construct an
        // expression that concatenates all non-double-quote substrings with
        // the quotes, e.g.:
        //
        //    concat("foo", '"', "bar")

        int singleCount = StringUtils.countMatches(str, "'");
        int doubleCount = StringUtils.countMatches(str, "\"");

        // the quote that gets split on
        String quote;
        // an inverse-quoted quote to represent the quote that was a split point
        String quotedQuote;
        // a pattern that splits on the designated split quote
        Pattern splitPattern;

        // use either single or double quote depending on which one is used less
        if (singleCount > doubleCount) {
            quote = "\"";
            quotedQuote = "'\"'";
            splitPattern = XPATH_DOUBLE_QUOTE_SPLIT_PATTERN;
        } else {
            quote = "'";
            quotedQuote = "\"'\"";
            splitPattern = XPATH_SINGLE_QUOTE_SPLIT_PATTERN;
        }

        List<String> xpathConcatList = new ArrayList<String>();

        // regex perhaps isn't the most efficient but its split behavior (with -1) is correct
        String[] substrings = splitPattern.split(str, -1);

        for (int i = 0; i < substrings.length; i++) {
            String chunk = substrings[i];

            // if this isn't just the space between subsequent quotes, include the chunk
            if (!chunk.equals("")) {
                xpathConcatList.add(quote + chunk + quote);
            }

            // if this isn't the last item, include a literal to represent the quote that was split on
            if (i < substrings.length - 1) {
                xpathConcatList.add(quotedQuote);
            }
        }

        return "concat(" + StringUtils.join(xpathConcatList, ", ") + ")";
    }
}
