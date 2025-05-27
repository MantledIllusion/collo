package com.mantledillusion.data.collo;

import java.util.regex.Pattern;

import com.mantledillusion.data.collo.KeywordAnalyzer.KeywordAnalyzerBuilder;

/**
 * Interface for keywords of input terms.
 * <p>
 * Every keyword represents one segment of a term which is matchable by a regular expression.
 * <p>
 * For example, a naive {@link Keyword} setup using an {@link Enum} for address matching parts might look
 * like this :
 * <p>
 * <code>
 * public static enum Keywords implements Keyword {<br>
 * <br>
 * &nbsp;&nbsp;STREET("[A-Z]{1}[A-Za-z]+"),<br>
 * &nbsp;&nbsp;HOUSENR("[1-9]{1}[0-9]*"),<br>
 * &nbsp;&nbsp;ZIP("\\d{4,6}"),<br>
 * &nbsp;&nbsp;STREET("[A-Z]{1}[A-Za-z]+");<br>
 * <br>
 * &nbsp;&nbsp;private final String matcher;<br>
 * <br>
 * &nbsp;&nbsp;private Keywords(String matcher) {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;this.matcher = matcher;<br>
 * &nbsp;&nbsp;}<br>
 * <br>
 * &nbsp;&nbsp;public String getMatcher() {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;return this.matcher;<br>
 * &nbsp;&nbsp;}<br>
 * }
 * </code>
 * <p>
 * 1-&gt;n keywords can define a {@link KeywordAnalyzer} using an {@link KeywordAnalyzerBuilder} started
 * with {@link KeywordAnalyzer#forKeyword(Keyword)} or {@link KeywordAnalyzer#forKeyword(Keyword, KeywordOccurrence)}.
 */
public interface Keyword {

	/**
	 * Returns the regular expression that is able to evaluate as {@link String#matches(String)}=true when used on
	 * a {@link String} input that represents a valid value for the {@link Keyword}.
	 * 
	 * @return A regular expression; never null and parseable by {@link Pattern}
	 */
	String getMatcher();

	/**
	 * Returns the weight used by the {@link KeywordAnalyzer} to sort analyzed keyword sets in descending manner, sets
	 * of higher summed weight will appear first in analysis results.
	 * <p>
	 * Applies the weight <code>1.0</code> by default.
	 *
	 * @param input The input that was analyzed; might <b>not</b> be null.
	 * @param segment The segment of the input that matched the keyword; might <b>not</b> be null.
	 * @return The weight
	 */
	default double weight(String input, String segment) {
		return 1.0;
	}
}
