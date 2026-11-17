package br.com.curralzap.backend.users.api;

import br.com.curralzap.backend.users.application.UserMutationInput;

record UserMutationApiRequest(
        String name,
        String phone,
        String role
) {
    UserMutationInput toInput() {
        return UserMutationInput.fromApi(phone, name, role);
    }
}
