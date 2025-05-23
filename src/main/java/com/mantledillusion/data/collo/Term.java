package com.mantledillusion.data.collo;

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
 */
public interface Term {

}
