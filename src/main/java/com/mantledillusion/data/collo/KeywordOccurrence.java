package com.mantledillusion.data.collo;

/**
 * Describes the occurrence of a keyword in a term.
 */
public enum KeywordOccurrence {

    /**
     * The keyword has to occur exactly once.
     */
    FIX,

    /**
     * The keyword has to occur once or not at all.
     */
    OPTIONAL,

    /**
     * The keyword has to occur once without any other keywords occurring, or not at all.
     */
    EXCLUSIVE
}
