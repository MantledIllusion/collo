package com.mantledillusion.data.collo;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TermAnalyzerTest {

	private static final String TEST_FIRSTNAME = "Harry";
	private static final String TEST_SECONDNAME = "James";
	private static final String TEST_FORENAME = join(TEST_FIRSTNAME, TEST_SECONDNAME);
	private static final String TEST_LASTNAME = "Potter";
	private static final String TEST_FULLNAME = join(TEST_FORENAME, TEST_LASTNAME);
	private static final String TEST_NICKNAME = "Undesirable No 1";

	private static final String TEST_HOUSENR = "4";
	private static final String TEST_STREET_1 = "Privet";
	private static final String TEST_STREET_2 = "Drive";
	private static final String TEST_CITY_1 = "Little";
	private static final String TEST_CITY_2 = "Whinging";
	private static final String TEST_ADDRESS = join(TEST_HOUSENR, TEST_STREET_1, TEST_STREET_2, TEST_CITY_1,
			TEST_CITY_2);
	private static final String TEST_BIRTHDAY = "1980-07-30";

	private static String join(String... strings) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < strings.length; i++) {
			sb.append(strings[i]).append(' ');
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	private enum Terms implements Term<Keywords> {
		FULLNAME, FULLADDRESS, BIRTHDAY;
	}

	private enum Keywords implements Keyword {
		FORENAME("[A-Z]{1}[A-Za-z]*( [A-Z]{1}[A-Za-z]*)*"),
		LASTNAME("[A-Z]{1}[A-Za-z]*"),
		UNDESIRABLE_NUMBER("Undesirable No \\d+"),

		HOUSENR("\\d+"),
		STREET("[A-Z]{1}[A-Za-z]*( [A-Z]{1}[A-Za-z]*)*"),
		CITY("[A-Z]{1}[A-Za-z]*( [A-Z]{1}[A-Za-z]*)*"),

		ISOLOCALDATE("[0-9]{4}-[0-9]{2}-[0-9]{2}", segment -> {
			try {
				DateTimeFormatter.ISO_LOCAL_DATE.parse(segment);
				return true;
			} catch (DateTimeParseException e) {
				return false;
			}
		});

		private final String matcher;
		private final Predicate<String> verifier;

		Keywords(String matcher) {
			this(matcher, segment -> true);
		}

		Keywords(String matcher, Predicate<String> verifier) {
			this.matcher = matcher;
            this.verifier = verifier;
        }

		@Override
		public String getMatcher() {
			return this.matcher;
		}

		@Override
		public boolean verify(String segment) {
			return this.verifier.test(segment);
		}
	}

	private TermAnalyzer<Terms, Keywords> analyzer;

	@Before
	public void before() {
		this.analyzer = new TermAnalyzer<Terms, Keywords>()
				.addTerm(Terms.FULLNAME, new KeywordAnalyzer<Keywords>()
						.addKeyword(Keywords.FORENAME)
						.addKeyword(Keywords.UNDESIRABLE_NUMBER, KeywordOccurrence.EXCLUSIVE)
						.addKeyword(Keywords.LASTNAME))
				.addTerm(Terms.FULLADDRESS, new KeywordAnalyzer<Keywords>()
						.addKeyword(Keywords.HOUSENR, KeywordOccurrence.OPTIONAL)
						.addKeyword(Keywords.STREET)
						.addKeyword(Keywords.CITY))
				.addTerm(Terms.BIRTHDAY, new KeywordAnalyzer<Keywords>()
						.addKeyword(Keywords.ISOLOCALDATE));
	}

	@Test
	public void testMandatoryMatching() {
		assertTrue(this.analyzer.matchesTerm(TEST_FULLNAME, Terms.FULLNAME));
	}

	@Test
	public void testExclusiveMatching() {
		assertTrue(this.analyzer.matchesTerm(TEST_NICKNAME, Terms.FULLNAME));
	}

	@Test
	public void testOptionalMatching() {
		assertTrue(this.analyzer.matchesTerm(TEST_ADDRESS, Terms.FULLADDRESS));
	}

	@Test
	public void testAnyMatching() {
		assertTrue(this.analyzer.matches(TEST_FULLNAME));
		assertTrue(this.analyzer.matches(TEST_NICKNAME));
		assertTrue(this.analyzer.matches(TEST_ADDRESS));
	}

	@Test
	public void testMatchFinding() {
		assertEquals(EnumSet.of(Terms.FULLNAME, Terms.FULLADDRESS), this.analyzer.matching(TEST_FULLNAME));
		assertEquals(EnumSet.of(Terms.FULLNAME), this.analyzer.matching(TEST_NICKNAME));
		assertEquals(EnumSet.of(Terms.FULLADDRESS), this.analyzer.matching(TEST_ADDRESS));
	}

	@Test
	public void testMandatoryAnalyzing() {
		assertEquals(getNameSplitByNameGroupPossibilities(), this.analyzer.analyze(TEST_FULLNAME, Terms.FULLNAME));
		assertEquals(getAddressSplitByNameGroupPossibilities(), this.analyzer.analyze(TEST_ADDRESS, Terms.FULLNAME));
	}

	private List<Map<Keywords, String>> getNameSplitByNameGroupPossibilities() {
		Map<Keywords, String> possibility = new HashMap<>();
		possibility.put(Keywords.FORENAME, TEST_FORENAME);
		possibility.put(Keywords.LASTNAME, TEST_LASTNAME);
		return Arrays.asList(possibility);
	}

	private List<Map<Keywords, String>> getAddressSplitByNameGroupPossibilities() {
		return Arrays.asList();
	}

	@Test
	public void testExclusiveAnalyzing() {
		assertEquals(getNickSplitByNameGroupPossibilities(), this.analyzer.analyze(TEST_NICKNAME, Terms.FULLNAME));
	}

	private List<Map<Keywords, String>> getNickSplitByNameGroupPossibilities() {
		Map<Keywords, String> possibility = new HashMap<>();
		possibility.put(Keywords.UNDESIRABLE_NUMBER, TEST_NICKNAME);
		return Arrays.asList(possibility);
	}

	@Test
	public void testOptionalAnalyzing() {
		assertEquals(getNameSplitByAddressGroupPossibilities(), this.analyzer.analyze(TEST_FULLNAME, Terms.FULLADDRESS));
		assertEquals(getAddressSplitByAddressGroupPossibilities(), this.analyzer.analyze(TEST_ADDRESS, Terms.FULLADDRESS));
	}

	private List<Map<Keywords, String>> getNameSplitByAddressGroupPossibilities() {
		Map<Keywords, String> possibility1 = new HashMap<>();
		possibility1.put(Keywords.STREET, TEST_FIRSTNAME);
		possibility1.put(Keywords.CITY, join(TEST_SECONDNAME, TEST_LASTNAME));

		Map<Keywords, String> possibility2 = new HashMap<>();
		possibility2.put(Keywords.STREET, join(TEST_FIRSTNAME, TEST_SECONDNAME));
		possibility2.put(Keywords.CITY, TEST_LASTNAME);

		return Arrays.asList(possibility1, possibility2);
	}

	private List<Map<Keywords, String>> getAddressSplitByAddressGroupPossibilities() {
		Map<Keywords, String> possibility1 = new HashMap<>();
		possibility1.put(Keywords.HOUSENR, TEST_HOUSENR);
		possibility1.put(Keywords.STREET, TEST_STREET_1);
		possibility1.put(Keywords.CITY, join(TEST_STREET_2, TEST_CITY_1, TEST_CITY_2));

		Map<Keywords, String> possibility2 = new HashMap<>();
		possibility2.put(Keywords.HOUSENR, TEST_HOUSENR);
		possibility2.put(Keywords.STREET, join(TEST_STREET_1, TEST_STREET_2));
		possibility2.put(Keywords.CITY, join(TEST_CITY_1, TEST_CITY_2));

		Map<Keywords, String> possibility3 = new HashMap<>();
		possibility3.put(Keywords.HOUSENR, TEST_HOUSENR);
		possibility3.put(Keywords.STREET, join(TEST_STREET_1, TEST_STREET_2, TEST_CITY_1));
		possibility3.put(Keywords.CITY, TEST_CITY_2);

		return Arrays.asList(possibility1, possibility2, possibility3);
	}

	@Test
	public void testAnyAnalyzing() {
		Map<Terms, List<Map<Keywords, String>>> nameSplitPossibilities = new HashMap<>();
		nameSplitPossibilities.put(Terms.FULLNAME, getNameSplitByNameGroupPossibilities());
		nameSplitPossibilities.put(Terms.FULLADDRESS, getNameSplitByAddressGroupPossibilities());
		
		assertEquals(nameSplitPossibilities, this.analyzer.analyze(TEST_FULLNAME));

		Map<Terms, List<Map<Keywords, String>>> nickSplitPossibilities = new HashMap<>();
		nickSplitPossibilities.put(Terms.FULLNAME, getNickSplitByNameGroupPossibilities());

		assertEquals(nickSplitPossibilities, this.analyzer.analyze(TEST_NICKNAME));
		
		Map<Terms, List<Map<Keywords, String>>> addressSplitPossibilities = new HashMap<>();
		addressSplitPossibilities.put(Terms.FULLADDRESS, getAddressSplitByAddressGroupPossibilities());
		
		assertEquals(addressSplitPossibilities, this.analyzer.analyze(TEST_ADDRESS));
	}

	@Test
	public void testVerifiedAnalyzing() {
		Map<Keywords, String> birthdayPossibility = new HashMap<>();
		birthdayPossibility.put(Keywords.ISOLOCALDATE, TEST_BIRTHDAY);

		Map<Terms, List<Map<Keywords, String>>> birthdayPossibilities = new HashMap<>();
		birthdayPossibilities.put(Terms.BIRTHDAY, Arrays.asList(birthdayPossibility));

		assertEquals(birthdayPossibilities, this.analyzer.analyze(TEST_BIRTHDAY));

		assertEquals(new HashMap<>(), this.analyzer.analyze("1980-13-30"));
	}
}
