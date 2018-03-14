package com.mantledillusion.data.collo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class InputAnalyzingTest {

	private static final String TEST_FIRSTNAME = "Harry";
	private static final String TEST_SECONDNAME = "James";
	private static final String TEST_FORENAME = join(TEST_FIRSTNAME, TEST_SECONDNAME);
	private static final String TEST_LASTNAME = "Potter";
	private static final String TEST_NAME = join(TEST_FORENAME, TEST_LASTNAME);

	private static final String TEST_HOUSENR = "4";
	private static final String TEST_STREET_1 = "Privet";
	private static final String TEST_STREET_2 = "Drive";
	private static final String TEST_CITY_1 = "Little";
	private static final String TEST_CITY_2 = "Whinging";
	private static final String TEST_ADDRESS = join(TEST_HOUSENR, TEST_STREET_1, TEST_STREET_2, TEST_CITY_1,
			TEST_CITY_2);

	private static String join(String... strings) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < strings.length; i++) {
			sb.append(strings[i]).append(' ');
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	private static enum InputGroups {
		FULLNAME, FULLADDRESS;
	}

	private static enum InputParts implements InputPart {
		FORENAME("[A-Z]{1}[A-Za-z]*( [A-Z]{1}[A-Za-z]*)*"),
		LASTNAME("[A-Z]{1}[A-Za-z]*"),

		HOUSENR("\\d+"),
		STREET("[A-Z]{1}[A-Za-z]*( [A-Z]{1}[A-Za-z]*)*"),
		CITY("[A-Z]{1}[A-Za-z]*( [A-Z]{1}[A-Za-z]*)*");

		private final String matcher;

		private InputParts(String matcher) {
			this.matcher = matcher;
		}

		@Override
		public String getMatcher() {
			return this.matcher;
		}
	}

	private InputAnalyzer<InputGroups, InputParts> analyzer;

	@Before
	public void before() {
		this.analyzer = InputAnalyzer
				.forGroup(InputGroups.FULLNAME,
						InputGroup.forPart(InputParts.FORENAME).andPart(InputParts.LASTNAME).build())
				.andGroup(InputGroups.FULLADDRESS, InputGroup.forPart(InputParts.HOUSENR, true)
						.andPart(InputParts.STREET).andPart(InputParts.CITY).build())
				.build();
	}

	@Test
	public void testMandatoryMatching() {
		assertTrue(this.analyzer.matchesGroup(TEST_NAME, InputGroups.FULLNAME));
		assertFalse(this.analyzer.matchesGroup(TEST_ADDRESS, InputGroups.FULLNAME));
	}

	@Test
	public void testOptionalMatching() {
		assertTrue(this.analyzer.matchesGroup(TEST_NAME, InputGroups.FULLADDRESS));
		assertTrue(this.analyzer.matchesGroup(TEST_ADDRESS, InputGroups.FULLADDRESS));
	}

	@Test
	public void testAnyMatching() {
		assertTrue(this.analyzer.matches(TEST_NAME));
		assertTrue(this.analyzer.matches(TEST_ADDRESS));
	}

	@Test
	public void testMatchFinding() {
		assertEquals(EnumSet.of(InputGroups.FULLNAME, InputGroups.FULLADDRESS), this.analyzer.matching(TEST_NAME));
		assertEquals(EnumSet.of(InputGroups.FULLADDRESS), this.analyzer.matching(TEST_ADDRESS));
	}

	@Test
	public void testMandatoryAnalyzing() {
		assertEquals(getNameSplitByNameGroupPossibilities(), this.analyzer.analyzeForGroup(TEST_NAME, InputGroups.FULLNAME));
		assertEquals(getAddressSplitByNameGroupPossibilities(), this.analyzer.analyzeForGroup(TEST_ADDRESS, InputGroups.FULLNAME));
	}

	private List<Map<InputParts, String>> getNameSplitByNameGroupPossibilities() {
		Map<InputParts, String> possibility = new HashMap<>();
		possibility.put(InputParts.FORENAME, TEST_FORENAME);
		possibility.put(InputParts.LASTNAME, TEST_LASTNAME);
		return Arrays.asList(possibility);
	}

	private List<Map<InputParts, String>> getAddressSplitByNameGroupPossibilities() {
		return Arrays.asList();
	}

	@Test
	public void testOptionalAnalyzing() {
		assertEquals(getNameSplitByAddressGroupPossibilities(), this.analyzer.analyzeForGroup(TEST_NAME, InputGroups.FULLADDRESS));
		assertEquals(getAddressSplitByAddressGroupPossibilities(), this.analyzer.analyzeForGroup(TEST_ADDRESS, InputGroups.FULLADDRESS));
	}

	private List<Map<InputParts, String>> getNameSplitByAddressGroupPossibilities() {
		Map<InputParts, String> possibility1 = new HashMap<>();
		possibility1.put(InputParts.STREET, TEST_FIRSTNAME);
		possibility1.put(InputParts.CITY, join(TEST_SECONDNAME, TEST_LASTNAME));

		Map<InputParts, String> possibility2 = new HashMap<>();
		possibility2.put(InputParts.STREET, join(TEST_FIRSTNAME, TEST_SECONDNAME));
		possibility2.put(InputParts.CITY, TEST_LASTNAME);

		return Arrays.asList(possibility1, possibility2);
	}

	private List<Map<InputParts, String>> getAddressSplitByAddressGroupPossibilities() {
		Map<InputParts, String> possibility1 = new HashMap<>();
		possibility1.put(InputParts.HOUSENR, TEST_HOUSENR);
		possibility1.put(InputParts.STREET, TEST_STREET_1);
		possibility1.put(InputParts.CITY, join(TEST_STREET_2, TEST_CITY_1, TEST_CITY_2));

		Map<InputParts, String> possibility2 = new HashMap<>();
		possibility2.put(InputParts.HOUSENR, TEST_HOUSENR);
		possibility2.put(InputParts.STREET, join(TEST_STREET_1, TEST_STREET_2));
		possibility2.put(InputParts.CITY, join(TEST_CITY_1, TEST_CITY_2));

		Map<InputParts, String> possibility3 = new HashMap<>();
		possibility3.put(InputParts.HOUSENR, TEST_HOUSENR);
		possibility3.put(InputParts.STREET, join(TEST_STREET_1, TEST_STREET_2, TEST_CITY_1));
		possibility3.put(InputParts.CITY, TEST_CITY_2);

		return Arrays.asList(possibility1, possibility2, possibility3);
	}

	@Test
	public void testAnyAnalyzing() {
		Map<InputGroups, List<Map<InputParts, String>>> nameSplitPossibilities = new HashMap<>();
		nameSplitPossibilities.put(InputGroups.FULLNAME, getNameSplitByNameGroupPossibilities());
		nameSplitPossibilities.put(InputGroups.FULLADDRESS, getNameSplitByAddressGroupPossibilities());
		
		assertEquals(nameSplitPossibilities, this.analyzer.analyze(TEST_NAME));
		
		Map<InputGroups, List<Map<InputParts, String>>> addressSplitPossibilities = new HashMap<>();
		addressSplitPossibilities.put(InputGroups.FULLADDRESS, getAddressSplitByAddressGroupPossibilities());
		
		assertEquals(addressSplitPossibilities, this.analyzer.analyze(TEST_ADDRESS));
	}
}
