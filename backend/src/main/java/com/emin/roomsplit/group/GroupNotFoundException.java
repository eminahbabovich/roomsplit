package com.emin.roomsplit.group;

// Namjerno ista poruka kad grupa ne postoji i kad korisnik nije član, da ne otkrijemo koje grupe postoje
public class GroupNotFoundException extends RuntimeException {

    public GroupNotFoundException() {
        super("Grupa ne postoji ili nisi njen član");
    }
}
