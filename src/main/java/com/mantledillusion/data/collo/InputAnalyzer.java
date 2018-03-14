package com.mantledillusion.data.collo;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An analyzer that might be used in cases where an input term has to be checked
 * against a set of {@link InputGroup}.
 * <p>
 * To form an {@link InputAnalyzer}, use {@link #forGroup(Enum, InputGroup)} to
 * start an {@link InputAnalyzerBuilder}.
 *
 * @param <G>
 *            The {@link Enum} type whose entries identify an
 *            {@link InputGroup}.
 * @param <P>
 *            The {@link Enum} type implementing {@link InputPart} for the
 *            group's parts.
 */
public final class InputAnalyzer<G extends Enum<G>, P extends Enum<P> & InputPart> {

	/**
	 * Builder for {@link InputAnalyzer}s.
	 *
	 * @param <G>
	 *            The {@link Enum} type whose entries identify an
	 *            {@link InputGroup}.
	 * @param <P>
	 *            The {@link Enum} type implementing {@link InputPart} for the
	 *            group's parts.
	 */
	public static final class InputAnalyzerBuilder<G extends Enum<G>, P extends Enum<P> & InputPart> {

		private final Map<G, InputGroup<P>> inputGroups = new HashMap<>();

		private InputAnalyzerBuilder() {
		}

		/**
		 * Adds the given {@link InputGroup} identified by the given identifier to the
		 * {@link InputAnalyzer} to build.
		 * 
		 * @param identifier
		 *            The identifier the given group will be identified by the
		 *            {@link InputAnalyzer}; might <b>not</b> be null.
		 * @param group
		 *            The group to add to the {@link InputAnalyzer}; might <b>not</b> be
		 *            null.
		 * @return this
		 */
		public InputAnalyzerBuilder<G, P> andGroup(G identifier, InputGroup<P> group) {
			if (identifier == null) {
				throw new IllegalArgumentException("Cannot add a group for a null identifier.");
			} else if (group == null) {
				throw new IllegalArgumentException("Cannot add a null group.");
			} else if (this.inputGroups.containsKey(identifier)) {
				throw new IllegalArgumentException("Cannot add a group for the identifier '" + identifier + "' twice.");
			}
			this.inputGroups.put(identifier, group);
			return this;
		}

		/**
		 * Builds a new {@link InputAnalyzer} out of the {@link InputGroup}s currently
		 * contained by this {@link InputAnalyzerBuilder}.
		 * 
		 * @return A new {@link InputAnalyzer}; never null
		 */
		public InputAnalyzer<G, P> build() {
			return new InputAnalyzer<>(new HashMap<>(this.inputGroups));
		}
	}

	private final Map<G, InputGroup<P>> inputGroups;

	private InputAnalyzer(Map<G, InputGroup<P>> inputGroups) {
		this.inputGroups = inputGroups;
	}

	/**
	 * Matches the given term against all {@link InputGroup}s of this
	 * {@link InputAnalyzer}.
	 * 
	 * @param term
	 *            The term to match against; might be null.
	 * @return True if any of the {@link InputAnalyzer}'s {@link InputGroup}s match
	 *         the given term, false otherwise
	 */
	public boolean matches(String term) {
		return this.inputGroups.values().parallelStream().anyMatch(group -> group.matches(term));
	}

	/**
	 * Matches the given term against a specific {@link InputGroup}s of this
	 * {@link InputAnalyzer}, identified by the given identifier.
	 * <p>
	 * This effectively equals taking the identified group and calling
	 * {@link InputGroup#matches(String)} on it using the given term.
	 * 
	 * @param term
	 *            The term to match against; might be null.
	 * @param identifier
	 *            The identifier that identifies the {@link InputGroup} of this
	 *            {@link InputAnalyzer} to match the term against; might <b>not</b>
	 *            be null.
	 * @return True if the identified {@link InputGroup} matches the given term,
	 *         false otherwise
	 */
	public boolean matchesGroup(String term, G identifier) {
		if (identifier == null) {
			throw new IllegalArgumentException("Cannot identify a group by a null identifier.");
		}
		if (!this.inputGroups.containsKey(identifier)) {
			throw new IllegalArgumentException(
					"The group identifier '" + identifier.name() + "' is unknown to this analyzer.");
		}
		return this.inputGroups.get(identifier).matches(term);
	}

	/**
	 * Finds the {@link InputGroup}s of this {@link InputAnalyzer} that match the
	 * given term.
	 * 
	 * @param term
	 *            The term to match against; might be null.
	 * @return A {@link Set} of identifiers that identify the matching
	 *         {@link InputGroup}s; never null, might be empty
	 */
	public Set<G> matching(String term) {
		return this.inputGroups.entrySet().parallelStream().filter(entry -> entry.getValue().matches(term))
				.map(entry -> entry.getKey()).collect(Collectors.toSet());
	}

	/**
	 * Returns a {@link Map} of groups and their possibilities on how the given term
	 * can be split by the {@link InputGroup}'s separators to match these
	 * {@link InputGroup}'s {@link InputPart}s.
	 * <p>
	 * For example, on an {@link InputAnalyzer} containing the two
	 * {@link InputGroup}s and their respective non-optional
	 * {@link InputPart}s...<br>
	 * - GROUP_STREET_AND_CITY(<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;PART_STREET("[A-Z]{1}[A-Za-z]*(
	 * [A-Z]{1}[A-Za-z]*)*"),<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;PART_CITY("[A-Z]{1}[A-Za-z]*(
	 * [A-Z]{1}[A-Za-z]*)*"),<br>
	 * &nbsp;&nbsp;)<br>
	 * - GROUP_FULLNAME(<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;PART_FORENAME("[A-Z]{1}[A-Za-z]*(
	 * [A-Z]{1}[A-Za-z]*)*"),<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;PART_LASTNAME("[A-Z]{1}[A-Za-z]*"),<br>
	 * &nbsp;&nbsp;)<br>
	 * ... that partially allow multiple words per part and both use the separator "
	 * ", the term "Harry James Potter" would cause the following output:
	 * <p>
	 * <code>
	 * Map[<br>
	 * &nbsp;&nbsp;GROUP_STREET_AND_CITY=List[<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;Map[<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;PART_STREET="Harry James",<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;PART_CITY="Potter"<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;],<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;Map[<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;PART_STREET="Harry",<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;PART_CITY="James Potter"<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;]<br>
	 * &nbsp;&nbsp;],<br>
	 * &nbsp;&nbsp;GROUP_FULLNAME=List[<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;Map[<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;PART_FORENAME="Harry James",<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;PART_LASTNAME="Potter"<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;]<br>
	 * &nbsp;&nbsp;]<br>
	 * ]
	 * </code>
	 * 
	 * @param term
	 *            The term to match against; might be null.
	 * @return A {@link Map} of {@link InputGroup} identifiers and those group's
	 *         possibilities to match the given input term; never null, might be
	 *         empty if no group matches the term at all (in this case,
	 *         {@link #matches(String)} would return <code>false</code> for the same
	 *         term)
	 */
	public Map<G, List<LinkedHashMap<P, String>>> analyze(String term) {
		Map<G, List<LinkedHashMap<P, String>>> resultMap = this.inputGroups.entrySet().parallelStream()
				.collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue().analyze(term)));
		return resultMap.entrySet().parallelStream().filter(entry -> !entry.getValue().isEmpty())
				.collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
	}

	/**
	 * Analyzes the given term using a specific {@link InputGroup}s of this
	 * {@link InputAnalyzer}, identified by the given identifier.
	 * <p>
	 * This effectively equals taking the identified group and calling
	 * {@link InputGroup#analyze(String)} on it using the given term.
	 * 
	 * @param term
	 *            The term to match against; might be null.
	 * @param identifier
	 *            The identifier that identifies the {@link InputGroup} of this
	 *            {@link InputAnalyzer} to match the term against; might <b>not</b>
	 *            be null.
	 * @return A {@link List} of possibilities how the identified group's parts
	 *         match the given term; never null, might be empty if the group does
	 *         not match the term at all (in this case,
	 *         {@link #matchesGroup(String, Enum)}} would return <code>false</code>
	 *         for the same identifier/term combination)
	 */
	public List<LinkedHashMap<P, String>> analyzeForGroup(String term, G identifier) {
		if (!this.inputGroups.containsKey(identifier)) {
			throw new IllegalArgumentException(
					"The group identifier '" + identifier.name() + "' is unknown to this analyzer.");
		}
		return this.inputGroups.get(identifier).analyze(term);
	}

	/**
	 * Begins a new {@link InputAnalyzerBuilder}.
	 * 
	 * @param <G>
	 *            The {@link Enum} type whose entries identify an
	 *            {@link InputGroup}.
	 * @param <P>
	 *            The {@link Enum} type implementing {@link InputPart} for the
	 *            group's parts.
	 * @param identifier
	 *            The identifier the given group will be identified by the
	 *            {@link InputAnalyzer}; might <b>not</b> be null.
	 * @param group
	 *            The group to add to the {@link InputAnalyzer}; might <b>not</b> be
	 *            null.
	 * @return A new {@link InputAnalyzerBuilder}; never null
	 */
	public static <G extends Enum<G>, P extends Enum<P> & InputPart> InputAnalyzerBuilder<G, P> forGroup(G identifier,
			InputGroup<P> group) {
		InputAnalyzerBuilder<G, P> builder = new InputAnalyzerBuilder<>();
		builder.andGroup(identifier, group);
		return builder;
	}
}
