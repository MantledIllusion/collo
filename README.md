# collo
Collo is a lightweight library for string input partitioning and grouping.

Collospermum Hastatum, also known as Perching Lily, is a New Zealand plant that uses its arching flax-like leaves for filtering water out of thin air, detouring it into channels to its base for consumption.

```xml
<dependency>
    <groupId>com.mantledillusion.data</groupId>
    <artifactId>collo</artifactId>
</dependency>
```

Get the newest version at [mvnrepository.com/collo](https://mvnrepository.com/artifact/com.mantledillusion.data/collo)

## 1. Shaping String Input

Many forms of string data has a certain format; for example, names have the first name before the last name and addresses usually start with the street and house number, followed by the name of the city.

Such simple formats can easily be recognized using regular expressions. But if there is a certain variation possible in the format, the regular expression complexity will completely explode:
- If there are a lot of optional parts ("Harry Potter" is valid, as is "Harry James Potter")
- If there are multiple overall formats for the data ("Harry Potter" is a possibility as well as "Potter, Harry")
- If the same entity can be referred to completely differently ("Harry Potter" and "Undesirable No 1" both refer to the same person)

In addition, if the input cannot be pre-categorized at all (not knowing a string will refer to a person, but instead it can contain a name, an address, ...), an endless if-else cascade of regular expression match checks will be the result.

Collo was made to counter all these problems at once.

### 1.1 The Keyword

The keyword is a technical regular expression based representation of string segments.

Collo requires an implementation of the interface **_Keyword_** to represent the keywords, so analysis results can be returned as Keyword->substring pairs.

This is an example of a simple **_Keyword_** using an **_Enum_**:

```java
private enum Keywords implements Keyword {
    FIRSTNAME("[A-Z]{1}[A-Za-z]*"),
    LASTNAME("[A-Z]{1}[A-Za-z]*"),
    UNDESIRABLE_NUMBER("Undesirable No \\d+"),

    HOUSENR("\\d+"),
    STREET("[A-Z]{1}[A-Za-z]*( [A-Z]{1}[A-Za-z]*)*"),
    CITY("[A-Z]{1}[A-Za-z]*( [A-Z]{1}[A-Za-z]*)*");
}
```

### 1.2 The Term

The term is a functional sequence of keywords that describe the same entity.

Collo requires an implementation of the interface **_Term_** to represent the terms, so analysis results can be returned as Term->Map<PartEnum, Substring> pairs.

This is an example of a simple **_Term_** using an **_Enum_**:

```java
    private enum Terms implements Term {
        FULLNAME, 
        FULLADDRESS;
    }
```

## 2. Term- and KeywordAnalyzers

The **_TermAnalyzer_** class offers a static builder which allows adding an arbitrary number of **_Term_** instances the analyzer will be able to recognize.

The **_KeywordAnalyzer_** class offers a static builder which allows adding an arbitrary number of **_Keyword_** instances that make up the term.

In combination, both builders can be used to set up an analyzer that can recognize completely different shapes of strings.

This is an example of building an analyzer using the example terms and keywords above:

```java
import com.mantledillusion.data.collo.TermAnalyzer;
import com.mantledillusion.data.collo.KeywordAnalyzer;
import com.mantledillusion.data.collo.KeywordOccurrence;

TermAnalyzer<Terms, InputParts> termAnalyzer = TermAnalyzer
        .forTerm(Terms.FULLNAME, KeywordAnalyzer
                .forKeyword(Keywords.UNDESIRABLE_NUMBER, KeywordOccurrence.EXCLUSIVE)
                .andKeyword(Keywords.FORENAME)
                .andKeyword(Keywords.LASTNAME)
                .build())
        .andTerm(Terms.FULLADDRESS, KeywordAnalyzer
                .forKeyword(Keywords.HOUSENR, KeywordOccurrence.OPTIONAL)
                .andKeyword(Keywords.STREET)
                .andKeyword(Keywords.CITY)
                .build())
        .build();
```

Setting a **_KeywordOccurrence_** can help to cover cases when a part can occur but does not have to (OPTIONAL), or when such an optional part might be the only part when it occurs (EXCLUSIVE).

The analyzer has several methods to analyze input strings, but all of them base on _**TermAnalyzer**.analyze(String input)_, which can split an input into the possible terms and keywords it matches:

Calling ```termAnalyzer.analyze("Harry Potter")``` on the example above would return a result map looking like this when converted to JSON:
```json
[
  {
    "key": "FULLNAME",
    "value": [
      {
        "key": "FIRSTNAME",
        "value": "Harry"
      },
      {
        "key": "LASTNAME",
        "value": "Potter"
      }
    ]
  }
]
```
