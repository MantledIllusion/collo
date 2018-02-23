package com.mantledillusion.data.collo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public final class InputAnalyzer<G extends Enum<G>, P extends Enum<P> & InputPart> {

	public static final class InputAnalyzerBuilder<G extends Enum<G>, P extends Enum<P> & InputPart> {

		private final Map<G, InputGroup<P>> inputGroups = new HashMap<>();

		private InputAnalyzerBuilder() {
		}

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

		public InputAnalyzer<G, P> build() {
			return new InputAnalyzer<>(new HashMap<>(this.inputGroups));
		}
	}

	private final Map<G, InputGroup<P>> inputGroups;

	private InputAnalyzer(Map<G, InputGroup<P>> inputGroups) {
		this.inputGroups = inputGroups;
	}
	
	public boolean matches(String input) {
		for (InputGroup<P> group : this.inputGroups.values()) {
			if (group.matches(input)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean matchesGroup(String input, G identifier) {
		if (!this.inputGroups.containsKey(identifier)) {
			throw new IllegalArgumentException(
					"The group identifier '" + identifier.name() + "' is unknown to this analyzer.");
		}
		return this.inputGroups.get(identifier).matches(input);
	}
	
	public Set<G> matching(String input) {
		Set<G> result = new HashSet<>();
		for (Entry<G, InputGroup<P>> group : this.inputGroups.entrySet()) {
			if (group.getValue().matches(input)) {
				result.add(group.getKey());
			}
		}
		return result;
	}

	public Map<G, List<LinkedHashMap<P, String>>> analyze(String input) {
		Map<G, List<LinkedHashMap<P, String>>> result = new HashMap<>();
		for (Entry<G, InputGroup<P>> group : this.inputGroups.entrySet()) {
			List<LinkedHashMap<P, String>> list = group.getValue().analyze(input);
			if (!list.isEmpty()) {
				result.put(group.getKey(), list);
			}
		}
		return result;
	}

	public List<LinkedHashMap<P, String>> analyzeForGroup(String input, G identifier) {
		if (!this.inputGroups.containsKey(identifier)) {
			throw new IllegalArgumentException(
					"The group identifier '" + identifier.name() + "' is unknown to this analyzer.");
		}
		return this.inputGroups.get(identifier).analyze(input);
	}

	public static <G extends Enum<G>, P extends Enum<P> & InputPart> InputAnalyzerBuilder<G, P> forGroup(G identifier,
			InputGroup<P> group) {
		InputAnalyzerBuilder<G, P> builder = new InputAnalyzerBuilder<>();
		builder.andGroup(identifier, group);
		return builder;
	}
}
