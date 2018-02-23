package com.mantledillusion.data.collo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public final class InputGroup<P extends Enum<P> & InputPart> {

	private static final class InputPartOccurrence<P extends Enum<P> & InputPart> {

		private final P part;
		private final boolean isOptional;

		private InputPartOccurrence(P part) {
			this(part, false);
		}

		private InputPartOccurrence(P part, boolean isOptional) {
			this.part = part;
			this.isOptional = isOptional;
		}

		P getPart() {
			return part;
		}

		boolean isOptional() {
			return isOptional;
		}
	}

	public static final class InputGroupBuilder<P extends Enum<P> & InputPart> {

		private final List<InputPartOccurrence<P>> inputParts = new ArrayList<>();

		private InputGroupBuilder() {
		}

		public InputGroupBuilder<P> andPart(P inputPart) {
			return andPart(inputPart, false);
		}

		public InputGroupBuilder<P> andPart(P inputPart, boolean isOptional) {
			if (inputPart == null) {
				throw new IllegalArgumentException("Cannot add null input part.");
			}
			this.inputParts.add(new InputPartOccurrence<>(inputPart, isOptional));
			return this;
		}
		
		public InputGroup<P> build() {
			return build(true);
		}

		@SuppressWarnings("unchecked")
		public InputGroup<P> build(boolean matchesAny) {
			return new InputGroup<>((InputPartOccurrence<P>[]) this.inputParts
					.toArray(new InputPartOccurrence[this.inputParts.size()]), matchesAny);
		}
	}

	private final InputPartOccurrence<P>[] inputParts;
	private final boolean matchesAny;

	private InputGroup(InputPartOccurrence<P>[] inputParts, boolean matchesAny) {
		this.inputParts = inputParts;
		this.matchesAny = matchesAny;
	}
	
	public boolean matches(String term) {
		return !analyze(term).isEmpty();
	}

	public List<LinkedHashMap<P, String>> analyze(String term) {
		String[] termParts = (term == null ? "" : term).split(" ");
		List<LinkedHashMap<P, String>> partitions = new ArrayList<>();
		boolean[] useMatcher = new boolean[this.inputParts.length];
		addPartitions(partitions, termParts, useMatcher, 0);
		return partitions;
	}

	private void addPartitions(List<LinkedHashMap<P, String>> partitions, String[] termParts, boolean[] useMatcher,
			int currentMatcherIndex) {
		if (this.inputParts[currentMatcherIndex].isOptional()) {
			addPartitions(partitions, termParts, useMatcher, currentMatcherIndex, false);
		}
		addPartitions(partitions, termParts, useMatcher, currentMatcherIndex, true);
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
		for (boolean b: useMatcher) {
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
				currentTermPart = join(termParts, currentTermIndex, incrementalTermIndex+1);
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
		for (int i=from; i<to; i++) {
			sb.append(strings[i]).append(' ');
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	public static <P extends Enum<P> & InputPart> InputGroupBuilder<P> forPart(P inputPart) {
		return forPart(inputPart, false);
	}

	public static <P extends Enum<P> & InputPart> InputGroupBuilder<P> forPart(P inputPart, boolean isOptional) {
		InputGroupBuilder<P> builder = new InputGroupBuilder<>();
		builder.andPart(inputPart, isOptional);
		return builder;
	}
}
