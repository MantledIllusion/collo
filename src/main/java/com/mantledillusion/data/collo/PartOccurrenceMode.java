package com.mantledillusion.data.collo;

/**
 * Describes the occurrence of a part in a group.
 */
public enum PartOccurrenceMode {

    /**
     * The part has to occur exactly once.
     */
    FIX,

    /**
     * The part has to occur once or not at all.
     */
    OPTIONAL,

    /**
     * The part has to occur once without any other parts occurring, or not at all.
     */
    EXCLUSIVE
}
