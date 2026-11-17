package br.com.curralzap.backend.users.api;

import br.com.curralzap.backend.users.application.InviteDetailView;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

record InviteApiResponse(
        UUID id,
        UUID farmId,
        String email,
        String phone,
        String role,
        String status,
        UUID invitedBy,
        OffsetDateTime createdAt
) {
    static InviteApiResponse from(InviteDetailView view) {
        return new InviteApiResponse(
                view.id(),
                view.farmId(),
                view.email(),
                view.phone(),
                view.role(),
                view.status(),
                view.invitedBy(),
                view.createdAt()
        );
    }

    static List<InviteApiResponse> fromList(List<InviteDetailView> views) {
        return views.stream().map(InviteApiResponse::from).toList();
    }
}
