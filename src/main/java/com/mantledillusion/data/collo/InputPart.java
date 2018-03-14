package com.mantledillusion.data.collo;

import java.util.regex.Pattern;

import com.mantledillusion.data.collo.InputGroup.InputGroupBuilder;

/**
 * Interface for {@link Enum}s that define parts of input terms.
 * <p>
 * Every {@link Enum} entry defines one part of an input term which is matchable
 * by a regular expression.
 * <p>
 * For example, a naive {@link InputPart} {@link Enum} for address matching
 * parts might look like this :
 * <p>
 * <code>
 * public static enum Parts implements InputParts {<br>
 * <br>
 * &nbsp;&nbsp;STREET("[A-Z]{1}[A-Za-z]+"),<br>
 * &nbsp;&nbsp;HOUSENR("[1-9]{1}[0-9]*"),<br>
 * &nbsp;&nbsp;ZIP("\\d{4,6}"),<br>
 * &nbsp;&nbsp;STREET("[A-Z]{1}[A-Za-z]+");<br>
 * <br>
 * &nbsp;&nbsp;private final String matcher;<br>
 * <br>
 * &nbsp;&nbsp;private Parts(String matcher) {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;this.matcher = matcher;<br>
 * &nbsp;&nbsp;}<br>
 * <br>
 * &nbsp;&nbsp;public String getMatcher() {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;return this.matcher;<br>
 * &nbsp;&nbsp;}<br>
 * }
 * </code>
 * <p>
 * 1-&gt;n parts of such an {@link Enum} can define an {@link InputGroup} using
 * an {@link InputGroupBuilder} started with {@link InputGroup#forPart(Enum)} or
 * {@link InputGroup#forPart(Enum, boolean)}.
 */
public interface InputPart {

	/**
	 * Returns the regular expression that is able to
	 * {@link String#matches(String)}=true when used on a {@link String} that
	 * represents a valid value for the {@link InputPart}.
	 * 
	 * @return A regular expression; never null and parseable by {@link Pattern}
	 */
	public String getMatcher();
}
