package com.emin.roomsplit.group;

public class AlreadyGroupMemberException extends RuntimeException {

    public AlreadyGroupMemberException() {
        super("Već si član ove grupe");
    }
}
