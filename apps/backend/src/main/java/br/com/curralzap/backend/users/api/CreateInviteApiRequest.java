package br.com.curralzap.backend.users.api;

import br.com.curralzap.backend.users.application.CreateInviteInput;

record CreateInviteApiRequest(
        String email,
        String phone,
        String role
) {
    CreateInviteInput toInput() {
        return CreateInviteInput.fromApi(email, phone, role);
    }
}
