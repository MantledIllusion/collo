package com.mantledillusion.data.collo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Defines a group of {@link InputPart} implementing {@link Enum} entries that form a syntactic group.
 * <p>
 * The group's parts have to be separated by a specifiable separator, so they can be matched individually.
 * <p>
 * To form an {@link InputGroup}, use {@link #forPart(Enum)} or {@link #forPart(Enum, PartOccurrenceMode)} to start
 * an {@link InputGroupBuilder}.
 * 
 * @param <P> The {@link InputPart} implementing {@link Enum} type that defines the group's parts.
 */
public final class InputGroup<P extends Enum<P> & InputPart> {

	private static final String DEFAULT_SPLITERATOR = " ";

	private static final class InputPartOccurrence<P extends Enum<P> & InputPart> {

		private final P part;
		private final PartOccurrenceMode partOccurrenceMode;

		private InputPartOccurrence(P part, PartOccurrenceMode partOccurrenceMode) {
			this.part = part;
			this.partOccurrenceMode = partOccurrenceMode;
		}

		private P getPart() {
			return part;
		}

		public PartOccurrenceMode getPartOccurrenceMode() {
			return partOccurrenceMode;
		}
	}

	/**
	 * A builder for {@link InputGroup}s.
	 * <p>
	 * All {@link InputPart}s given to the builder will be contained and checked by the finally build group in the
	 * exact order they where given to the builder.
	 * 
	 * @param <P> The {@link InputPart} implementing {@link Enum} type that defines the group's parts.
	 */
	public static final class InputGroupBuilder<P extends Enum<P> & InputPart> {

		private final List<InputPartOccurrence<P>> inputParts = new ArrayList<>();

		private InputGroupBuilder() {
		}

		/**
		 * Adds the given {@link InputPart} to the {@link InputGroup} to build.
		 * 
		 * @param inputPart The part to add; might <b>not</b> be null.
		 * @return this
		 */
		public InputGroupBuilder<P> andPart(P inputPart) {
			return andPart(inputPart, PartOccurrenceMode.FIX);
		}

		/**
		 * Adds the given {@link InputPart} to the {@link InputGroup} to build.
		 *
		 * @param inputPart The part to add; might <b>not</b> be null.
		 * @param partOccurrenceMode The mode describing how the part can occur in its group; might <b>not</b> be null.
		 * @return this
		 */
		public InputGroupBuilder<P> andPart(P inputPart, PartOccurrenceMode partOccurrenceMode) {
			if (inputPart == null) {
				throw new IllegalArgumentException("Cannot add null input part.");
			} else if (partOccurrenceMode == null) {
				throw new IllegalArgumentException("Cannot input part using a null part mode.");
			}
			this.inputParts.add(new InputPartOccurrence<>(inputPart, partOccurrenceMode));
			return this;
		}

		/**
		 * Builds an {@link InputGroup} out of the {@link InputPart}s currently contained by this {@link InputGroupBuilder}.
		 * <p>
		 * <code>matchesAny</code> will be set to <code>true</code>; Even if all the group's parts are optional, at
		 * least one has to match a given term in order for the group to match.
		 * <p>
		 * <code>spliterator</code> will be set to <code>" "</code>; the group's parts are expected to be separated by
		 * a space.
		 * 
		 * @return A new {@link InputGroup}; never null
		 */
		public InputGroup<P> build() {
			return build(true, DEFAULT_SPLITERATOR);
		}

		/**
		 * Builds an {@link InputGroup} out of the {@link InputPart}s currently contained by this
		 * {@link InputGroupBuilder}.
		 * <p>
		 * <code>spliterator</code> will be set to <code>" "</code>; the group's parts are expected to be separated by
		 * a space.
		 * 
		 * @param matchesAny Whether or not any of the group's parts has to match in a given input term. This has only
		 *                   effect in groups whose parts are all optional; in that case, using <code>false</code> will
		 *                   allow an empty {@link String} to match the group.
		 * @return A new {@link InputGroup}; never null
		 */
		public InputGroup<P> build(boolean matchesAny) {
			return build(matchesAny, DEFAULT_SPLITERATOR);
		}

		/**
		 * Builds an {@link InputGroup} out of the {@link InputPart}s currently contained by this
		 * {@link InputGroupBuilder}.
		 * <p>
		 * <code>matchesAny</code> will be set to <code>true</code>; Even if all the group's parts are optional, at
		 * least one has to match a given term in order for the group to match.
		 * 
		 * @param spliterator The separator to split an input term by to gain separated {@link InputPart}s;
		 *                    might <b>not</b> be null.
		 * @return A new {@link InputGroup}; never null
		 */
		public InputGroup<P> build(String spliterator) {
			return build(true, spliterator);
		}

		/**
		 * Builds an {@link InputGroup} out of the {@link InputPart}s currently contained by this
		 * {@link InputGroupBuilder}.
		 * 
		 * @param matchesAny Whether or not any of the group's parts has to match in a given input term. This has only
		 *                   effect in groups whose parts are all optional; in that case, using <code>false</code> will
		 *                   allow an empty {@link String} to match the group.
		 * @param spliterator The separator to split an input term by to gain separated
		 * @return A new {@link InputGroup}; never null
		 */
		@SuppressWarnings("unchecked")
		public InputGroup<P> build(boolean matchesAny, String spliterator) {
			if (spliterator == null || spliterator.isEmpty()) {
				throw new IllegalArgumentException("The spliterator may never be null or empty.");
			}
			return new InputGroup<>(this.inputParts.toArray(new InputPartOccurrence[0]), matchesAny, spliterator);
		}
	}

	private final InputPartOccurrence<P>[] inputParts;
	private final boolean matchesAny;
	private final String spliterator;

	private InputGroup(InputPartOccurrence<P>[] inputParts, boolean matchesAny, String spliterator) {
		this.inputParts = inputParts;
		this.matchesAny = matchesAny;
		this.spliterator = spliterator;
	}

	/**
	 * Returns whether this {@link InputGroup} matches the given input term.
	 * 
	 * @param term The term to match against; might be null.
	 * @return True if the given term matches this {@link InputGroup}, false otherwise
	 */
	public boolean matches(String term) {
		return !analyze(term).isEmpty();
	}

	/**
	 * Returns a {@link List} of possibilities on how the given term can be split by this {@link InputGroup}'s
	 * separator to match this {@link InputGroup}'s {@link InputPart}s.
	 * <p>
	 * For example, for a street-city matching group with the non-optional parts...<br>
	 * - STREET("[A-Z]{1}[A-Za-z]*( [A-Z]{1}[A-Za-z]*)*") and<br>
	 * - CITY("[A-Z]{1}[A-Za-z]*( [A-Z]{1}[A-Za-z]*)*")<br>
	 * ... that allow multiple words per part and the separator " ", the term "Diagon Alley London" would cause the
	 * following output:
	 * <p>
	 * <code>
	 * List[<br>
	 * &nbsp;&nbsp;Map[<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;STREET="Diagon",<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;CITY="Alley London"<br>
	 * &nbsp;&nbsp;],<br>
	 * &nbsp;&nbsp;Map[<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;STREET="Diagon Alley",<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;CITY="London"<br>
	 * &nbsp;&nbsp;]<br>
	 * ]<br>
	 * </code>
	 * 
	 * @param term The term to analyze; might be null
	 * @return A {@link List} of possibilities how this group's parts match the given term; never null, might be empty
	 * if this group does not match the term at all (in this case, {@link #matches(String)} would return
	 * <code>false</code> for the same term)
	 */
	public List<LinkedHashMap<P, String>> analyze(String term) {
		String[] termParts = (term == null ? "" : term).split(spliterator);
		List<LinkedHashMap<P, String>> partitions = new ArrayList<>();
		boolean[] useMatcher = new boolean[this.inputParts.length];
		addPartitions(partitions, termParts, useMatcher, 0);
		return partitions;
	}

	private void addPartitions(List<LinkedHashMap<P, String>> partitions, String[] termParts, boolean[] useMatcher,
			int currentMatcherIndex) {
		if (this.inputParts[currentMatcherIndex].getPartOccurrenceMode() == PartOccurrenceMode.EXCLUSIVE) {
			// ADD PARTITIONS WITHOUT THE EXCLUSIVE PART
			addPartitions(partitions, termParts, useMatcher, currentMatcherIndex, false);

			// BUILD PARTITIONS OF JUST THE EXCLUSIVE PART
			useMatcher = new boolean[useMatcher.length];
			useMatcher[currentMatcherIndex] = true;
			buildPartition(partitions, new LinkedHashMap<>(), termParts, useMatcher, 0, 0);
		} else {
			// ADD PARTITIONS WITHOUT THE OPTIONAL PART
			if (this.inputParts[currentMatcherIndex].getPartOccurrenceMode() == PartOccurrenceMode.OPTIONAL) {
				addPartitions(partitions, termParts, useMatcher, currentMatcherIndex, false);
			}

			// ADD PARTITIONS WITH THE OPTIONAL PART
			addPartitions(partitions, termParts, useMatcher, currentMatcherIndex, true);
		}
	}

	private void addPartitions(List<LinkedHashMap<P, String>> partitions, String[] termParts, boolean[] useMatcher,
			int currentMatcherIndex, boolean useCurrentMatcher) {
		useMatcher[currentMatcherIndex] = useCurrentMatcher;
		if (currentMatcherIndex + 1 < this.inputParts.length) {
			addPartitions(partitions, termParts, useMatcher, currentMatcherIndex + 1);
		} else if (!this.matchesAny || matchesAny(useMatcher)) {
			buildPartition(partitions, new LinkedHashMap<>(), termParts, useMatcher, 0, 0);
		}
	}

	private boolean matchesAny(boolean[] useMatcher) {
		for (boolean b : useMatcher) {
			if (b) {
				return true;
			}
		}
		return false;
	}

	private void buildPartition(List<LinkedHashMap<P, String>> partitions, LinkedHashMap<P, String> currentPartition,
			String[] termParts, boolean[] useMatcher, int currentTermIndex, int currentMatcherIndex) {
		if (useMatcher[currentMatcherIndex]) {
			int aheadMatcherCount = 0;
			for (int aheadMatcherIndex = currentMatcherIndex
					+ 1; aheadMatcherIndex < useMatcher.length; aheadMatcherIndex++) {
				if (useMatcher[aheadMatcherIndex]) {
					aheadMatcherCount++;
				}
			}

			String currentTermPart;
			for (int incrementalTermIndex = currentTermIndex; incrementalTermIndex < termParts.length
					- aheadMatcherCount; incrementalTermIndex++) {
				currentTermPart = join(termParts, currentTermIndex, incrementalTermIndex + 1);
				if (currentTermPart.matches(this.inputParts[currentMatcherIndex].getPart().getMatcher())) {
					LinkedHashMap<P, String> newPartition = new LinkedHashMap<>(currentPartition);
					newPartition.put(this.inputParts[currentMatcherIndex].getPart(), currentTermPart);
					if (incrementalTermIndex + 1 == termParts.length) {
						partitions.add(newPartition);
					} else if (currentMatcherIndex + 1 < this.inputParts.length) {
						buildPartition(partitions, newPartition, termParts, useMatcher, incrementalTermIndex + 1,
								currentMatcherIndex + 1);
					}
				}
			}
		} else if (currentMatcherIndex + 1 < this.inputParts.length) {
			buildPartition(partitions, currentPartition, termParts, useMatcher, currentTermIndex,
					currentMatcherIndex + 1);
		}
	}

	private String join(String[] strings, int from, int to) {
		StringBuilder sb = new StringBuilder();
		for (int i = from; i < to; i++) {
			sb.append(strings[i]).append(this.spliterator);
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	/**
	 * Begins a new {@link InputGroupBuilder}.
	 * 
	 * @param <P> The {@link InputPart} implementing {@link Enum} type that defines the group's parts.
	 * @param inputPart The part to add; might <b>not</b> be null.
	 * @return A new {@link InputGroupBuilder}; never null
	 */
	public static <P extends Enum<P> & InputPart> InputGroupBuilder<P> forPart(P inputPart) {
		return forPart(inputPart, PartOccurrenceMode.FIX);
	}

	/**
	 * Begins a new {@link InputGroupBuilder}.
	 * 
	 * @param <P> The {@link InputPart} implementing {@link Enum} type that defines the group's parts.
	 * @param inputPart The part to add; might <b>not</b> be null.
	 * @param partOccurrenceMode The mode describing how the part can occur in its group; might <b>not</b> be null.
	 * @return A new {@link InputGroupBuilder}; never null
	 */
	public static <P extends Enum<P> & InputPart> InputGroupBuilder<P> forPart(P inputPart, PartOccurrenceMode partOccurrenceMode) {
		InputGroupBuilder<P> builder = new InputGroupBuilder<>();
		builder.andPart(inputPart, partOccurrenceMode);
		return builder;
	}
}
