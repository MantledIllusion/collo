package com.mantledillusion.data.collo;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An analyzer that might be used in cases where an input has to be checked against a set of {@link KeywordAnalyzer}s.
 *
 * @param <T> The {@link Term} type identifying the analyzers terms.
 * @param <K> The {@link Keyword} type identifying the analyzer term's keywords.
 */
public final class TermAnalyzer<T extends Term<K>, K extends Keyword> {

	private class WeightedTerm {

		private final T term;
		private final List<LinkedHashMap<K, String>> keywords;
		private final double weight;

        private WeightedTerm(String input, T term, List<LinkedHashMap<K, String>> keywords) {
            this.term = term;
            this.keywords = keywords;
			this.weight = this.term.weight(input, this.keywords);
        }

		private T getTerm() {
			return this.term;
		}

		private List<LinkedHashMap<K, String>> getKeywords() {
			return this.keywords;
		}

		private double getWeight() {
			return this.weight;
		}

		private boolean matches() {
			return !this.keywords.isEmpty();
		}
    }

	/**
	 * A listener to {@link TermAnalyzer}s.
	 *
	 * @param <T> The {@link Term} type identifying the analyzers terms.
	 * @param <K> The {@link Keyword} type identifying the analyzer term's keywords.
	 */
	public interface TermUpdateListener<T extends Term<K>, K extends Keyword> {

		/**
		 * Invoked when a term is added/removed to/from a {@link TermAnalyzer}.
		 *
		 * @param term The term that was updated; might <b>not</b> be null.
		 * @param keywordAnalyzer The keyword analyzer that was updated; might <b>not</b> be null.
		 * @param added True if the term was added, false if it was removed
		 */
		void termUpdated(T term, KeywordAnalyzer<K> keywordAnalyzer, boolean added);
	}

	private final Map<T, KeywordAnalyzer<K>> terms;
	private final List<TermUpdateListener<T, K>> listeners = new ArrayList<>();

	/**
	 * Instantiates a {@link TermAnalyzer}.
	 */
	public TermAnalyzer() {
		this.terms = new HashMap<>();
	}

	/**
	 * Adds the given {@link TermUpdateListener} to the analyzer.
	 *
	 * @param listener The listener to add; might <b>not</b> be null.
	 */
	public void addListener(TermUpdateListener<T, K> listener) {
		this.listeners.add(listener);
	}

	/**
	 * Removed the given {@link TermUpdateListener} from the analyzer.
	 *
	 * @param listener The listener to remove; might <b>not</b> be null.
	 */
	public void removeListener(TermUpdateListener<T, K> listener) {
		this.listeners.remove(listener);
	}

	/**
	 * Returns an unmodifiable view of the analyzer's terms.
	 *
	 * @return The terms, never null, might be empty
	 */
	public Set<T> getTerms() {
		return this.terms.keySet();
	}

	/**
	 * Adds the given {@link KeywordAnalyzer} identified by the given identifier to this {@link TermAnalyzer}.
	 *
	 * @param term The definition the given term will be identified by the {@link TermAnalyzer}; might <b>not</b> be null.
	 * @param analyzer The term to add to the {@link TermAnalyzer}; might <b>not</b> be null.
	 * @return this
	 */
	public TermAnalyzer<T, K> addTerm(T term, KeywordAnalyzer<K> analyzer) {
		if (term == null) {
			throw new IllegalArgumentException("Cannot add an analyzer for a null term.");
		} else if (analyzer == null) {
			throw new IllegalArgumentException("Cannot add a null analyzer.");
		} else if (this.terms.containsKey(term)) {
			throw new IllegalArgumentException("Cannot add an analyzer for the term '" + term + "' twice.");
		}
		this.terms.put(term, analyzer);
		this.listeners.forEach(listener -> listener.termUpdated(term, analyzer, true));
		return this;
	}

	/**
	 * Removes the {@link KeywordAnalyzer} identified by the given identifier from this {@link TermAnalyzer}.
	 *
	 * @param term The definition the given term is identified by the {@link TermAnalyzer}; might <b>not</b> be null.
	 * @return this
	 */
	public TermAnalyzer<T, K> removeTerm(T term) {
		if (this.terms.containsKey(term)) {
			KeywordAnalyzer<K> analyzer = this.terms.remove(term);
			this.listeners.forEach(listener -> listener.termUpdated(term, analyzer, false));
		}
		return this;
	}

	/**
	 * Matches the given input against all {@link KeywordAnalyzer}s of this {@link TermAnalyzer}.
	 * 
	 * @param input The input to match against; might be null.
	 * @return True if any of the {@link TermAnalyzer}'s {@link KeywordAnalyzer}s match the given input, false otherwise
	 */
	public boolean matches(String input) {
		return this.terms.values().parallelStream().anyMatch(term -> term.matches(input));
	}

	/**
	 * Matches the given input against a specific {@link KeywordAnalyzer} of this {@link TermAnalyzer}, identified by the
	 * given {@link Term}.
	 * <p>
	 * This effectively equals taking the {@link KeywordAnalyzer} of the given {@link Term} and calling
	 * {@link KeywordAnalyzer#matches(String)} on it using the given input.
	 * 
	 * @param input The input to match against; might be null.
	 * @param term The term that identifies the {@link KeywordAnalyzer} of this {@link TermAnalyzer} to match the input
	 *             against; might <b>not</b> be null.
	 * @return True if the identified {@link KeywordAnalyzer} matches the given input,  false otherwise
	 */
	public boolean matchesTerm(String input, T term) {
		if (term == null) {
			throw new IllegalArgumentException("Cannot identify a term by a null definition.");
		}
		if (!this.terms.containsKey(term)) {
			throw new IllegalArgumentException(
					"The term '" + term + "' is unknown to this analyzer.");
		}
		return this.terms.get(term).matches(input);
	}

	/**
	 * Finds the {@link KeywordAnalyzer}s of this {@link TermAnalyzer} that match the given input.
	 * 
	 * @param input The input to match against; might be null.
	 * @return A {@link Set} of {@link Term}s that identify the matching {@link KeywordAnalyzer}s; never null, might be empty
	 */
	public Set<T> matching(String input) {
		return this.terms.entrySet().parallelStream()
				.filter(entry -> entry.getValue().matches(input))
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
	}

	/**
	 * Returns a {@link LinkedHashMap} sorted by weight of terms and their possibilities on how the given input can be
	 * split by the {@link KeywordAnalyzer}'s separators to match the {@link KeywordAnalyzer}'s {@link Keyword}s.
	 * <p>
	 * For example, on an {@link TermAnalyzer} containing the two {@link Term}s and their respective non-optional
	 * {@link Keyword}s...<br>
	 * - STREET_AND_CITY(<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;STREET("[A-Z]{1}[A-Za-z]*(
	 * [A-Z]{1}[A-Za-z]*)*"),<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;CITY("[A-Z]{1}[A-Za-z]*(
	 * [A-Z]{1}[A-Za-z]*)*"),<br>
	 * &nbsp;&nbsp;)<br>
	 * - FULLNAME(<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;FIRSTNAME("[A-Z]{1}[A-Za-z]*(
	 * [A-Z]{1}[A-Za-z]*)*"),<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;LASTNAME("[A-Z]{1}[A-Za-z]*"),<br>
	 * &nbsp;&nbsp;)<br>
	 * ... that allow multiple words per keyword and both use the separator " ", the term "Harry James Potter" would
	 * cause the following output:
	 * <p>
	 * <code>
	 * Map[<br>
	 * &nbsp;&nbsp;STREET_AND_CITY=List[<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;Map[<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;STREET="Harry James",<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;CITY="Potter"<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;],<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;Map[<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;STREET="Harry",<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;CITY="James Potter"<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;]<br>
	 * &nbsp;&nbsp;],<br>
	 * &nbsp;&nbsp;FULLNAME=List[<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;Map[<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;FIRSTNAME="Harry James",<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;LASTNAME="Potter"<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;]<br>
	 * &nbsp;&nbsp;]<br>
	 * ]
	 * </code>
	 * 
	 * @param input The input to match against; might be null.
	 * @return A {@link Map} of {@link Term}s and its possibilities to match the given input; never null, might be
	 * empty if no {@link KeywordAnalyzer} matches the input at all (in this case, {@link #matches(String)} would
	 * return <code>false</code> for the same input)
	 */
	public LinkedHashMap<T, List<LinkedHashMap<K, String>>> analyze(String input) {
		return this.terms.entrySet().parallelStream()
				.map(entry -> new WeightedTerm(input, entry.getKey(), entry.getValue().analyze(input)))
				.filter(WeightedTerm::matches)
				.sorted(Comparator.comparing(WeightedTerm::getWeight).reversed())
				.collect(Collectors.toMap(
						WeightedTerm::getTerm,
                        WeightedTerm::getKeywords,
						(k1, k2) -> Stream.concat(k1.stream(), k2.stream())
								.collect(Collectors.toList()),
						LinkedHashMap::new
				));
	}

	/**
	 * Analyzes the given input using a specific {@link KeywordAnalyzer}s of this {@link TermAnalyzer}, identified by
	 * the given {@link Term}.
	 * <p>
	 * This effectively equals taking the {@link KeywordAnalyzer} of the given {@link Term} and calling
	 * {@link KeywordAnalyzer#analyze(String)} on it using the given input.
	 * 
	 * @param input The input to match against; might be null.
	 * @param term The {@link Term} that identifies the {@link KeywordAnalyzer} of this {@link TermAnalyzer} to match
	 *             the given input against; might <b>not</b> be null.
	 * @return A {@link Map} of {@link Term}s and its possibilities to match the given input; never null, might be
	 * empty if no {@link KeywordAnalyzer} matches the input at all (in this case, {@link #matches(String)} would
	 * return <code>false</code> for the same input)
	 */
	public List<LinkedHashMap<K, String>> analyze(String input, T term) {
		if (!this.terms.containsKey(term)) {
			throw new IllegalArgumentException(
					"The term '" + term + "' is unknown to this analyzer.");
		}
		return this.terms.get(term).analyze(input);
	}
}
