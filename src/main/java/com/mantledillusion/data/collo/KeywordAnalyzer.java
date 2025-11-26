package com.mantledillusion.data.collo;

import java.util.*;
import java.util.stream.Collectors;

/**
 * An analyzer that might be used in cases where an input has to be checked against a set of {@link Keyword}s.
 * <p>
 * The analyzer's keywords have to be separated by a specifiable separator, so they can be matched individually.
 * 
 * @param <K> The {@link Keyword} type that defines the term's keywords.
 */
public final class KeywordAnalyzer<K extends Keyword> {

	/**
	 * The separator " " used by default {@link KeywordAnalyzer}s.
	 */
	public static final String DEFAULT_SPLITERATOR = " ";

	private class WeightedKeywords {

		private final LinkedHashMap<K, String> keywords;
		private final double weight;

		private WeightedKeywords(String input, LinkedHashMap<K, String> keywords) {
			this.keywords = keywords;
			this.weight = keywords.entrySet().parallelStream()
					.mapToDouble(entry -> entry.getKey().weight(input, entry.getValue()))
					.sum();
		}

		public LinkedHashMap<K, String> getKeywords() {
			return this.keywords;
		}

		public double getWeight() {
			return this.weight;
		}
	}

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
	 * A listener to {@link KeywordAnalyzer}s.
	 *
	 * @param <K> The {@link Keyword} type that defines the term's keywords.
	 */
	public interface KeywordUpdateListener<K extends Keyword> {

		/**
		 * Invoked when a keyword is added/removed to/from a {@link KeywordAnalyzer}.
		 *
		 * @param keyword The keyword that was updated; might <b>not</b> be null.
		 * @param occurrence The occurrence of the keyword that was updated; might <b>not</b> be null.
		 * @param added True if the keyword was added, false if it was removed
		 */
		void keywordUpdated(K keyword, KeywordOccurrence occurrence, boolean added);
	}

	private final List<AnalyzerKeyword<K>> keywords;
	private final boolean matchesAny;
	private final String spliterator;
	private final List<KeywordUpdateListener<K>> listeners = new ArrayList<>();

	/**
	 * Instantiates a {@link KeywordAnalyzer}.
	 * <p>
	 * Defines <code>matchesAny</code> as <code>true</code>; even if all the analyzer's keywords are optional, at
	 * least one has to match in order for the whole analyzer to match.
	 * <p>
	 * Defines <code>spliterator</code> as <code>" "</code>; the analyzer's keywords are expected to be separated
	 * by a space.
	 */
	public KeywordAnalyzer() {
		this(true, DEFAULT_SPLITERATOR);
	}

	/**
	 * Instantiates a {@link KeywordAnalyzer}.
	 * <p>
	 * Defines <code>spliterator</code> as <code>" "</code>; the analyzer's keywords are expected to be separated
	 * by a space.
	 *
	 * @param matchesAny Whether any of the analyzer's keywords has to match for the whole analyzer to match. Only
	 *                   has effect in analyzers whose keywords are all optional; in that case, using
	 *                   <code>false</code> will allow an empty input to match the analyzer.
	 */
	public KeywordAnalyzer(boolean matchesAny) {
		this(matchesAny, DEFAULT_SPLITERATOR);
	}

	/**
	 * Instantiates a {@link KeywordAnalyzer}.
	 * <p>
	 * Defines <code>matchesAny</code> as <code>true</code>; even if all the analyzer's keywords are optional, at
	 * least one has to match in order for the whole analyzer to match.
	 *
	 * @param spliterator The separator to split an input by to gain separated keywords; might <b>not</b> be null.
	 */
	public KeywordAnalyzer(String spliterator) {
		this(true, spliterator);
	}

	/**
	 * Instantiates a {@link KeywordAnalyzer}.
	 *
	 * @param matchesAny Whether any of the analyzer's keywords has to match for the whole analyzer to match. Only
	 *                   has effect in analyzers whose keywords are all optional; in that case, using
	 *                   <code>false</code> will allow an empty input to match the analyzer.
	 * @param spliterator The separator to split an input by to gain separated keywords; might <b>not</b> be null.
	 */
	public KeywordAnalyzer(boolean matchesAny, String spliterator) {
		this.keywords = new ArrayList<>();
		this.matchesAny = matchesAny;
		this.spliterator = spliterator;
	}

	/**
	 * Adds the given {@link KeywordUpdateListener} to the analyzer.
	 *
	 * @param listener The listener to add; might <b>not</b> be null.
	 */
	public void addListener(KeywordUpdateListener<K> listener) {
		this.listeners.add(listener);
	}

	/**
	 * Removed the given {@link KeywordUpdateListener} from the analyzer.
	 *
	 * @param listener The listener to remove; might <b>not</b> be null.
	 */
	public void removeListener(KeywordUpdateListener<K> listener) {
		this.listeners.remove(listener);
	}

	/**
	 * Adds the given {@link Keyword} to this {@link KeywordAnalyzer}.
	 * <p>
	 * Defines the keyword's occurrence in its analyzer as {@link KeywordOccurrence#FIX}.
	 *
	 * @param keyword The keyword to add; might <b>not</b> be null.
	 * @return this
	 */
	public KeywordAnalyzer<K> addKeyword(K keyword) {
		return addKeyword(keyword, KeywordOccurrence.FIX);
	}

	/**
	 * Adds the given {@link Keyword} to this {@link KeywordAnalyzer}.
	 *
	 * @param keyword The keyword to add; might <b>not</b> be null.
	 * @param occurrence Describes how the keyword can occur in its analyzer; might <b>not</b> be null.
	 * @return this
	 */
	public KeywordAnalyzer<K> addKeyword(K keyword, KeywordOccurrence occurrence) {
		if (keyword == null) {
			throw new IllegalArgumentException("Cannot add null keyword.");
		} else if (occurrence == null) {
			throw new IllegalArgumentException("Cannot keyword using a null occurrence.");
		} else if (this.keywords.stream().anyMatch(next -> next.keyword.equals(keyword))) {
			throw new IllegalArgumentException("Cannot add keyword '" + keyword + "' twice.");
		}
		this.keywords.add(new AnalyzerKeyword<>(keyword, occurrence));
		this.listeners.forEach(listener -> listener.keywordUpdated(keyword, occurrence, true));
		return this;
	}

	/**
	 * Removes the given {@link Keyword} from this {@link KeywordAnalyzer}.
	 *
	 * @param keyword The keyword to remove; might <b>not</b> be null.
	 * @return this
	 */
	public KeywordAnalyzer<K> removeKeyword(K keyword) {
		Iterator<AnalyzerKeyword<K>> iter = this.keywords.iterator();
		while (iter.hasNext()) {
			AnalyzerKeyword<K> next = iter.next();
			if (next.keyword.equals(keyword)) {
				iter.remove();
				this.listeners.forEach(listener -> listener.keywordUpdated(keyword, next.occurrence, false));
			}
		}
		return this;
	}

	/**
	 * Returns an unmodifiable view of the analyzer's keywords.
	 *
	 * @return The keywords, never null, might be empty
	 */
	public List<K> getKeywords() {
		return Collections.unmodifiableList(this.keywords.stream()
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
	 * Returns a {@link List} sorted by weight of possibilities on how the given input can be split by this
	 * {@link KeywordAnalyzer}'s separator to match this {@link KeywordAnalyzer}'s {@link Keyword}s.
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
		boolean[] useMatcher = new boolean[this.keywords.size()];
		addTerms(terms, termKeywords, useMatcher, 0);
		return terms.parallelStream()
				.map(keywords -> new WeightedKeywords(input, keywords))
				.sorted(Comparator.comparing(WeightedKeywords::getWeight).reversed())
				.map(WeightedKeywords::getKeywords)
				.collect(Collectors.toList());
	}

	private void addTerms(List<LinkedHashMap<K, String>> terms, String[] termKeywords,
						  boolean[] useMatcher, int currentMatcherIndex) {
		if (this.keywords.get(currentMatcherIndex).getOccurrence() == KeywordOccurrence.EXCLUSIVE) {
			// ADD TERMS WITHOUT THE EXCLUSIVE KEYWORD
			addTerms(terms, termKeywords, useMatcher, currentMatcherIndex, false);

			// BUILD TERMS OF JUST THE EXCLUSIVE KEYWORD
			useMatcher = new boolean[useMatcher.length];
			useMatcher[currentMatcherIndex] = true;
			buildTerm(terms, new LinkedHashMap<>(), termKeywords, useMatcher, 0, 0);
		} else {
			// ADD TERMS WITHOUT THE OPTIONAL KEYWORD
			if (this.keywords.get(currentMatcherIndex).getOccurrence() == KeywordOccurrence.OPTIONAL) {
				addTerms(terms, termKeywords, useMatcher, currentMatcherIndex, false);
			}

			// ADD TERMS WITH THE OPTIONAL KEYWORD
			addTerms(terms, termKeywords, useMatcher, currentMatcherIndex, true);
		}
	}

	private void addTerms(List<LinkedHashMap<K, String>> terms, String[] termKeywords,
						  boolean[] useMatcher, int currentMatcherIndex, boolean useCurrentMatcher) {
		useMatcher[currentMatcherIndex] = useCurrentMatcher;
		if (currentMatcherIndex + 1 < this.keywords.size()) {
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

			String segment;
			for (int incrementalTermIndex = currentTermIndex; incrementalTermIndex < termKeywords.length
					- aheadMatcherCount; incrementalTermIndex++) {
				segment = join(termKeywords, currentTermIndex, incrementalTermIndex + 1);
				K keyword = this.keywords.get(currentMatcherIndex).getKeyword();
				if (segment.matches(keyword.getMatcher()) && keyword.verify(segment)) {
					LinkedHashMap<K, String> newTerm = new LinkedHashMap<>(currentTerm);
					newTerm.put(keyword, segment);
					if (incrementalTermIndex + 1 == termKeywords.length) {
						terms.add(newTerm);
					} else if (currentMatcherIndex + 1 < this.keywords.size()) {
						buildTerm(terms, newTerm, termKeywords, useMatcher, incrementalTermIndex + 1,
								currentMatcherIndex + 1);
					}
				}
			}
		} else if (currentMatcherIndex + 1 < this.keywords.size()) {
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
}
