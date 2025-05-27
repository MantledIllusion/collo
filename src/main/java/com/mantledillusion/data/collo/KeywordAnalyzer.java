package com.mantledillusion.data.collo;

import java.util.*;
import java.util.stream.Collectors;

/**
 * An analyzer that might be used in cases where an input has to be checked against a set of {@link Keyword}s.
 * <p>
 * The analyzer's keywords have to be separated by a specifiable separator, so they can be matched individually.
 * <p>
 * To form a {@link KeywordAnalyzer}, use {@link #forKeyword(Keyword)}
 * or {@link #forKeyword(Keyword, KeywordOccurrence)} to start a {@link KeywordAnalyzerBuilder}.
 * 
 * @param <K> The {@link Keyword} type that defines the term's keywords.
 */
public final class KeywordAnalyzer<K extends Keyword> {

	private static final String DEFAULT_SPLITERATOR = " ";

	private static final class AnalyzerKeyword<K extends Keyword> {

		private final K keyword;
		private final KeywordOccurrence occurrence;

		private AnalyzerKeyword(K keyword, KeywordOccurrence occurrence) {
			this.keyword = keyword;
			this.occurrence = occurrence;
		}

		private K getKeyword() {
			return keyword;
		}

		private KeywordOccurrence getOccurrence() {
			return occurrence;
		}
	}

	/**
	 * A builder for {@link KeywordAnalyzer}s.
	 * <p>
	 * All keywords given to the builder will be contained and checked by the build analyzer in the exact order they
	 * were given to the builder.
	 * 
	 * @param <K> The {@link Keyword} type that defines the {@link KeywordAnalyzer}'s keywords.
	 */
	public static final class KeywordAnalyzerBuilder<K extends Keyword> {

		private final List<AnalyzerKeyword<K>> keywords = new ArrayList<>();

		private KeywordAnalyzerBuilder() {
		}

		/**
		 * Adds the given {@link Keyword} to the {@link KeywordAnalyzer} to build.
		 * <p>
		 * Defines the keyword's occurrence in its analyzer as {@link KeywordOccurrence#FIX}.
		 * 
		 * @param keyword The keyword to add; might <b>not</b> be null.
		 * @return this
		 */
		public KeywordAnalyzerBuilder<K> andKeyword(K keyword) {
			return andKeyword(keyword, KeywordOccurrence.FIX);
		}

		/**
		 * Adds the given {@link Keyword} to the {@link KeywordAnalyzer} to build.
		 *
		 * @param keyword The keyword to add; might <b>not</b> be null.
		 * @param occurrence Describes how the keyword can occur in its analyzer; might <b>not</b> be null.
		 * @return this
		 */
		public KeywordAnalyzerBuilder<K> andKeyword(K keyword, KeywordOccurrence occurrence) {
			if (keyword == null) {
				throw new IllegalArgumentException("Cannot add null keyword.");
			} else if (occurrence == null) {
				throw new IllegalArgumentException("Cannot keyword using a null occurrence.");
			}
			this.keywords.add(new AnalyzerKeyword<>(keyword, occurrence));
			return this;
		}

		/**
		 * Builds an {@link KeywordAnalyzer} out of the {@link Keyword}s currently contained by this {@link KeywordAnalyzerBuilder}.
		 * <p>
		 * Defines <code>matchesAny</code> as <code>true</code>; even if all the analyzer's keywords are optional, at
		 * least one has to match in order for the whole analyzer to match.
		 * <p>
		 * Defines <code>spliterator</code> as <code>" "</code>; the analyzer's keywords are expected to be separated
		 * by a space.
		 * 
		 * @return A new {@link KeywordAnalyzer}; never null
		 */
		public KeywordAnalyzer<K> build() {
			return build(true, DEFAULT_SPLITERATOR);
		}

		/**
		 * Builds an {@link KeywordAnalyzer} out of the {@link Keyword}s currently contained by this {@link KeywordAnalyzerBuilder}.
		 * <p>
		 * Defines <code>spliterator</code> as <code>" "</code>; the analyzer's keywords are expected to be separated
		 * by a space.
		 * 
		 * @param matchesAny Whether any of the analyzer's keywords has to match for the whole analyzer to match. Only
		 *                   has effect in analyzers whose keywords are all optional; in that case, using
		 *                   <code>false</code> will allow an empty input to match the analyzer.
		 * @return A new {@link KeywordAnalyzer}; never null
		 */
		public KeywordAnalyzer<K> build(boolean matchesAny) {
			return build(matchesAny, DEFAULT_SPLITERATOR);
		}

		/**
		 * Builds an {@link KeywordAnalyzer} out of the {@link Keyword}s currently contained by this {@link KeywordAnalyzerBuilder}.
		 * <p>
		 * Defines <code>matchesAny</code> as <code>true</code>; even if all the analyzer's keywords are optional, at
		 * least one has to match in order for the whole analyzer to match.
		 * 
		 * @param spliterator The separator to split an input by to gain separated keywords; might <b>not</b> be null.
		 * @return A new {@link KeywordAnalyzer}; never null
		 */
		public KeywordAnalyzer<K> build(String spliterator) {
			return build(true, spliterator);
		}

		/**
		 * Builds an {@link KeywordAnalyzer} out of the {@link Keyword}s currently contained by this
		 * {@link KeywordAnalyzerBuilder}.
		 *
		 * @param matchesAny Whether any of the analyzer's keywords has to match for the whole analyzer to match. Only
		 *                   has effect in analyzers whose keywords are all optional; in that case, using
		 *                   <code>false</code> will allow an empty input to match the analyzer.
		 * @param spliterator The separator to split an input by to gain separated keywords; might <b>not</b> be null.
		 * @return A new {@link KeywordAnalyzer}; never null
		 */
		@SuppressWarnings("unchecked")
		public KeywordAnalyzer<K> build(boolean matchesAny, String spliterator) {
			if (spliterator == null || spliterator.isEmpty()) {
				throw new IllegalArgumentException("The spliterator may never be null or empty.");
			}
			return new KeywordAnalyzer<>(this.keywords.toArray(new AnalyzerKeyword[0]), matchesAny, spliterator);
		}
	}

	private final AnalyzerKeyword<K>[] keywords;
	private final boolean matchesAny;
	private final String spliterator;

	private KeywordAnalyzer(AnalyzerKeyword<K>[] keywords, boolean matchesAny, String spliterator) {
		this.keywords = keywords;
		this.matchesAny = matchesAny;
		this.spliterator = spliterator;
	}

	/**
	 * Returns an unmodifiable view of the analyzer's keywords.
	 *
	 * @return The keywords, never null, might be empty
	 */
	public List<K> getKeywords() {
		return Collections.unmodifiableList(Arrays.stream(this.keywords)
				.map(AnalyzerKeyword::getKeyword)
				.collect(Collectors.toList()));
	}

	/**
	 * Returns whether this {@link KeywordAnalyzer} matches the given input.
	 * 
	 * @param input The input to match against; might be null.
	 * @return True if the given input matches this {@link KeywordAnalyzer}, false otherwise
	 */
	public boolean matches(String input) {
		return !analyze(input).isEmpty();
	}

	/**
	 * Returns a {@link List} of possibilities on how the given input can be split by this {@link KeywordAnalyzer}'s
	 * separator to match this {@link KeywordAnalyzer}'s {@link Keyword}s.
	 * <p>
	 * For example, for a street-city matching input with the non-optional keywords...<br>
	 * - STREET("[A-Z]{1}[A-Za-z]*( [A-Z]{1}[A-Za-z]*)*") and<br>
	 * - CITY("[A-Z]{1}[A-Za-z]*( [A-Z]{1}[A-Za-z]*)*")<br>
	 * ... that allow multiple words per keyword and the separator " ", the input "Diagon Alley London" would cause the
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
	 * @param input The input to analyze; might be null
	 * @return A {@link List} of possibilities how this analyzer's keywords match the given input; never null, might be
	 * empty if this input does not match the analyzer at all (in this case, {@link #matches(String)} would return
	 * <code>false</code> for the same term)
	 */
	public List<LinkedHashMap<K, String>> analyze(String input) {
		String[] termKeywords = (input == null ? "" : input).split(spliterator);
		List<LinkedHashMap<K, String>> terms = new ArrayList<>();
		boolean[] useMatcher = new boolean[this.keywords.length];
		addTerms(terms, termKeywords, useMatcher, 0);
		return terms;
	}

	private void addTerms(List<LinkedHashMap<K, String>> terms, String[] termKeywords,
						  boolean[] useMatcher, int currentMatcherIndex) {
		if (this.keywords[currentMatcherIndex].getOccurrence() == KeywordOccurrence.EXCLUSIVE) {
			// ADD TERMS WITHOUT THE EXCLUSIVE KEYWORD
			addTerms(terms, termKeywords, useMatcher, currentMatcherIndex, false);

			// BUILD TERMS OF JUST THE EXCLUSIVE KEYWORD
			useMatcher = new boolean[useMatcher.length];
			useMatcher[currentMatcherIndex] = true;
			buildTerm(terms, new LinkedHashMap<>(), termKeywords, useMatcher, 0, 0);
		} else {
			// ADD TERMS WITHOUT THE OPTIONAL KEYWORD
			if (this.keywords[currentMatcherIndex].getOccurrence() == KeywordOccurrence.OPTIONAL) {
				addTerms(terms, termKeywords, useMatcher, currentMatcherIndex, false);
			}

			// ADD TERMS WITH THE OPTIONAL KEYWORD
			addTerms(terms, termKeywords, useMatcher, currentMatcherIndex, true);
		}
	}

	private void addTerms(List<LinkedHashMap<K, String>> terms, String[] termKeywords,
						  boolean[] useMatcher, int currentMatcherIndex, boolean useCurrentMatcher) {
		useMatcher[currentMatcherIndex] = useCurrentMatcher;
		if (currentMatcherIndex + 1 < this.keywords.length) {
			addTerms(terms, termKeywords, useMatcher, currentMatcherIndex + 1);
		} else if (!this.matchesAny || matchesAny(useMatcher)) {
			buildTerm(terms, new LinkedHashMap<>(), termKeywords, useMatcher, 0, 0);
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

	private void buildTerm(List<LinkedHashMap<K, String>> terms, LinkedHashMap<K, String> currentTerm, String[] termKeywords,
						   boolean[] useMatcher, int currentTermIndex, int currentMatcherIndex) {
		if (useMatcher[currentMatcherIndex]) {
			int aheadMatcherCount = 0;
			for (int aheadMatcherIndex = currentMatcherIndex
					+ 1; aheadMatcherIndex < useMatcher.length; aheadMatcherIndex++) {
				if (useMatcher[aheadMatcherIndex]) {
					aheadMatcherCount++;
				}
			}

			String currentTermKeyword;
			for (int incrementalTermIndex = currentTermIndex; incrementalTermIndex < termKeywords.length
					- aheadMatcherCount; incrementalTermIndex++) {
				currentTermKeyword = join(termKeywords, currentTermIndex, incrementalTermIndex + 1);
				if (currentTermKeyword.matches(this.keywords[currentMatcherIndex].getKeyword().getMatcher())) {
					LinkedHashMap<K, String> newTerm = new LinkedHashMap<>(currentTerm);
					newTerm.put(this.keywords[currentMatcherIndex].getKeyword(), currentTermKeyword);
					if (incrementalTermIndex + 1 == termKeywords.length) {
						terms.add(newTerm);
					} else if (currentMatcherIndex + 1 < this.keywords.length) {
						buildTerm(terms, newTerm, termKeywords, useMatcher, incrementalTermIndex + 1,
								currentMatcherIndex + 1);
					}
				}
			}
		} else if (currentMatcherIndex + 1 < this.keywords.length) {
			buildTerm(terms, currentTerm, termKeywords, useMatcher, currentTermIndex,
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
	 * Begins a new {@link KeywordAnalyzerBuilder}.
	 * <p>
	 * Defines the keyword's occurrence in its term as {@link KeywordOccurrence#FIX}.
	 * 
	 * @param <K> The {@link Keyword} type that defines the analyzer's keywords.
	 * @param keyword The keyword to add; might <b>not</b> be null.
	 * @return A new {@link KeywordAnalyzerBuilder}; never null
	 */
	public static <K extends Keyword> KeywordAnalyzerBuilder<K> forKeyword(K keyword) {
		return forKeyword(keyword, KeywordOccurrence.FIX);
	}

	/**
	 * Begins a new {@link KeywordAnalyzerBuilder}.
	 * 
	 * @param <K> The {@link Keyword} type that defines the analyzer's keywords.
	 * @param keyword The keyword to add; might <b>not</b> be null.
	 * @param occurrence Describes how the keyword can occur in its analyzer; might <b>not</b> be null.
	 * @return A new {@link KeywordAnalyzerBuilder}; never null
	 */
	public static <K extends Keyword> KeywordAnalyzerBuilder<K> forKeyword(K keyword, KeywordOccurrence occurrence) {
		KeywordAnalyzerBuilder<K> builder = new KeywordAnalyzerBuilder<>();
		builder.andKeyword(keyword, occurrence);
		return builder;
	}
}
