package br.com.curralzap.backend.users.application;

public class UserManagementForbiddenException extends RuntimeException {

    public UserManagementForbiddenException() {
        super("User does not have permission to manage users.");
    }
}
