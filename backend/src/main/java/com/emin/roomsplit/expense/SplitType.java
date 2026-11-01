package com.emin.roomsplit.expense;

public enum SplitType {
    /** Iznos se dijeli jednako na sve učesnike. */
    EQUAL,
    /** Svaki učesnik ima tačno zadan iznos, zbir mora biti jednak ukupnom iznosu. */
    EXACT
}
