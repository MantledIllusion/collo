# collo
Collo is a lightweight library for string input partitioning and grouping.

Collospermum Hastatum, also known as Perching Lily, is a New Zealand plant that uses its arching flax-like leaves for filtering water out of thin air, detouring it into channels to its base for consumption.

## 1. Shaping String Input

Many forms of string data has a certain format; for example, names have the first name before the last name and addresses usually start with the street, followed by the name of the city.

Such simple formats can easily be recognized using regular expressions. But if there is a certain variation possible in the format, the regular expression complexity will completely explode:
- If there are a lot of optional parts ("Harry Potter" is valid, as is "Harry James Potter")
- If there are multiple overall formats for the data ("Harry Potter" is a possibility as well as "Potter, Harry")
- If the same entity can be referred to completely differently ("Harry Potter" and "Undesirable No 1" both refer to the same person)

In addition, if the input cannot be pre-categorized at all (not knowing a string will refer to a person, but instead it can contain a name, an address, ...), an endless if-else cascade of regular expression match checks will be the result.

Collo was made to counter all these problems at once.

### 1.1 The InputPart

The input part is a technical regular expression based representation of string segments.

Collo requires an **_Enum_** implementing the interface **_InputPart_** to represent the parts, so analysis results can be returned as PartEnum->Substring pairs.

```java
private enum InputParts implements InputPart {
    FIRSTNAME("[A-Z]{1}[A-Za-z]*"),
    LASTNAME("[A-Z]{1}[A-Za-z]*"),
    UNDESIRABLE_NUMBER("Undesirable No \\d+"),

    HOUSENR("\\d+"),
    STREET("[A-Z]{1}[A-Za-z]*( [A-Z]{1}[A-Za-z]*)*"),
    CITY("[A-Z]{1}[A-Za-z]*( [A-Z]{1}[A-Za-z]*)*");
}
```

### 1.2 The InputGroup

The input group is a functional sequence of input parts that describe the same entity.

Collo requires an **_Enum_** to represent the groups, so analysis results can be returned as GroupEnum->Map<PartEnum, Substring> pairs.

```java
    private enum InputGroups {
        FULLNAME, 
        FULLADDRESS;
    }
```

## 2. The InputAnalyzer

The **_InputAnalyzer_** class offers a static builder which allows adding an arbitrary number of **_InputGroup_** instances the analyzer will be able to recognize.

The **_InputGroup_** class offers a static builder which allows adding an arbitrary number of **_InputPart_** instances that make up the group.

In combination, both builders can be used to set up an analyzer that can recognize completely different shapes of strings:

```java
InputAnalyzer<InputGroups, InputParts> analyzer = InputAnalyzer
    .forGroup(InputGroups.FULLNAME, InputGroup.
        andPart(InputParts.UNDESIRABLE_NUMBER, PartOccurrenceMode.EXCLUSIVE).
        forPart(InputParts.FORENAME).
        andPart(InputParts.LASTNAME).build())
    .andGroup(InputGroups.FULLADDRESS, InputGroup.
        forPart(InputParts.HOUSENR, PartOccurrenceMode.OPTIONAL).
        andPart(InputParts.STREET).
        andPart(InputParts.CITY).build())
    .build();
```

Setting a **_PartOccurrenceMode_** can help to cover cases when a part can occur but does not have to (OPTIONAL), or when such an optional part might be the only part when it occurs (EXCLUSIVE).

The analyzer has several methods to analyze input strings, but all of them base on _**InputAnalyzer**.analyze(String term)_, which can split a term into the possible groups it matches:

```inputAnalyzer.analyze("Harry Potter")``` :
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
