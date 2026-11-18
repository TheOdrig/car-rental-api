package com.akif.auth.api;

public interface UserLookup {

    UserDto getUserByUsername(String username);
}
