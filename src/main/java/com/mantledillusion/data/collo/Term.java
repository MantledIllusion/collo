package com.mantledillusion.data.collo;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Interface for input terms.
 * <p>
 * Every term represents a set of keywords which is matchable by a regular expression.
 * <p>
 * For example, a naive {@link Term} setup using an {@link Enum} for address matching parts might look
 * like this :
 * <p>
 * <code>
 * public static enum Terms implements Term {<br>
 * <br>
 * &nbsp;&nbsp;ADDRESS;<br>
 * <br>
 * }
 * </code>
 *
 * @param <K> The {@link Keyword} type identifying the term's keywords.
 */
public interface Term<K extends Keyword> {

    /**
     * Returns the weight used by the {@link TermAnalyzer} to sort analyzed terms in descending manner, terms of higher
     * weight will appear first in analysis results.
     * <p>
     * Applies the weight <code>1.0</code> by default.
     *
     * @param input The input that was analyzed; might <b>not</b> be null.
     * @param keywords The keywords that matched the input; might <b>not</b> be null.
     * @return The weight
     */
    default double weight(String input, List<LinkedHashMap<K, String>> keywords) {
        return 1.0;
    }
}
