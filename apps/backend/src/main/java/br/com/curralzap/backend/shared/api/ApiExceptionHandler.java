package br.com.curralzap.backend.shared.api;

import br.com.curralzap.backend.ops.application.InvalidOpsPeriodException;
import br.com.curralzap.backend.ops.application.OpsAuthenticationRequiredException;
import br.com.curralzap.backend.animals.application.AnimalHasRecordsException;
import br.com.curralzap.backend.animals.application.AnimalNotFoundException;
import br.com.curralzap.backend.animals.application.InvalidAnimalQueryException;
import br.com.curralzap.backend.animals.application.InvalidAnimalMutationException;
import br.com.curralzap.backend.animals.application.AnimalWriteForbiddenException;
import br.com.curralzap.backend.auth.application.InactiveUserException;
import br.com.curralzap.backend.auth.application.AuthenticationRequiredException;
import br.com.curralzap.backend.billing.application.AnimalQuotaExceededException;
import br.com.curralzap.backend.billing.application.BillingAccessDeniedException;
import br.com.curralzap.backend.billing.application.BillingGatewayException;
import br.com.curralzap.backend.auth.application.InvalidRefreshTokenException;
import br.com.curralzap.backend.auth.application.ExpiredAuthTokenException;
import br.com.curralzap.backend.auth.application.ExpiredRefreshTokenException;
import br.com.curralzap.backend.auth.application.InvalidAuthTokenException;
import br.com.curralzap.backend.auth.application.NoInvitationException;
import br.com.curralzap.backend.auth.application.OAuthTokenVerificationException;
import br.com.curralzap.backend.farms.application.FarmQuotaExceededException;
import br.com.curralzap.backend.farms.application.FarmSwitchForbiddenException;
import br.com.curralzap.backend.farms.application.InvalidFarmRegistrationException;
import br.com.curralzap.backend.auth.application.RevokedRefreshTokenException;
import br.com.curralzap.backend.auth.application.SessionReplacedException;
import br.com.curralzap.backend.feeding.application.FeedingAccessForbiddenException;
import br.com.curralzap.backend.reports.application.DashboardReadForbiddenException;
import br.com.curralzap.backend.feeding.application.InvalidFeedingMutationException;
import br.com.curralzap.backend.feeding.application.InvalidFeedingQueryException;
import br.com.curralzap.backend.farms.application.FarmSettingsForbiddenException;
import br.com.curralzap.backend.farms.application.FarmSettingsNotFoundException;
import br.com.curralzap.backend.farms.application.InvalidFarmSettingsMutationException;
import br.com.curralzap.backend.lots.application.InvalidLotQueryException;
import br.com.curralzap.backend.lots.application.InvalidLotUpsertException;
import br.com.curralzap.backend.lots.application.LotNotFoundException;
import br.com.curralzap.backend.lots.application.LotReadForbiddenException;
import br.com.curralzap.backend.lots.application.LotWriteForbiddenException;
import br.com.curralzap.backend.medications.application.InvalidMedicationMutationException;
import br.com.curralzap.backend.medications.application.InvalidMedicationQueryException;
import br.com.curralzap.backend.medications.application.MedicationAccessForbiddenException;
import br.com.curralzap.backend.medications.application.MedicationNotFoundException;
import br.com.curralzap.backend.users.application.DuplicateUserPhoneException;
import br.com.curralzap.backend.users.application.InvalidInviteInputException;
import br.com.curralzap.backend.users.application.InvalidUserMutationException;
import br.com.curralzap.backend.users.application.InvalidUserQueryException;
import br.com.curralzap.backend.users.application.InviteForbiddenException;
import br.com.curralzap.backend.users.application.InviteNotFoundException;
import br.com.curralzap.backend.users.application.UserManagementForbiddenException;
import br.com.curralzap.backend.users.application.UserNotFoundException;
import br.com.curralzap.backend.vaccines.application.InvalidVaccineMutationException;
import br.com.curralzap.backend.vaccines.application.InvalidVaccineQueryException;
import br.com.curralzap.backend.vaccines.application.VaccineAccessForbiddenException;
import br.com.curralzap.backend.vaccines.application.VaccineNotFoundException;
import br.com.curralzap.backend.occurrences.application.InvalidOccurrenceQueryException;
import br.com.curralzap.backend.offers.application.InvalidOfferMutationException;
import br.com.curralzap.backend.offers.application.OfferAccessForbiddenException;
import br.com.curralzap.backend.offers.application.OfferNotFoundException;
import br.com.curralzap.backend.offers.application.InvalidOfferStateTransitionException;
import br.com.curralzap.backend.occurrences.application.InvalidOccurrenceStateTransitionException;
import br.com.curralzap.backend.occurrences.application.OccurrenceActionForbiddenException;
import br.com.curralzap.backend.occurrences.application.OccurrenceNotFoundException;
import br.com.curralzap.backend.occurrences.application.OccurrenceQueryForbiddenException;
import br.com.curralzap.backend.weighings.application.InvalidWeighingQueryException;
import br.com.curralzap.backend.weighings.application.InvalidWeighingMutationException;
import br.com.curralzap.backend.weighings.application.WeighingAnimalNotFoundException;
import br.com.curralzap.backend.weighings.application.WeighingWriteForbiddenException;
import br.com.curralzap.backend.whatsapp.application.InvalidWhatsappWebhookAuthenticationException;
import br.com.curralzap.backend.whatsapp.application.InvalidWhatsappSupportMessageQueryException;
import br.com.curralzap.backend.whatsapp.application.InvalidWhatsappWebhookPayloadException;
import br.com.curralzap.backend.whatsapp.application.WhatsappMessagePublishFailedException;
import br.com.curralzap.backend.whatsapp.application.WhatsappSupportForbiddenException;
import br.com.curralzap.backend.whatsapp.application.WhatsappSupportMessageNotFoundException;
import br.com.curralzap.backend.whatsapp.application.WhatsappSupportRetryNotAllowedException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(InvalidOpsPeriodException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidOpsPeriod(
            InvalidOpsPeriodException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "validation_failed", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(OpsAuthenticationRequiredException.class)
    ResponseEntity<ApiErrorResponse> handleOpsAuthenticationRequired(
            OpsAuthenticationRequiredException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "ops_authentication_required", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(AuthenticationRequiredException.class)
    ResponseEntity<ApiErrorResponse> handleAuthenticationRequired(
            AuthenticationRequiredException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "authentication_required", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "validation_failed", "Dados inválidos.", fieldErrors, request);
    }

    @ExceptionHandler(OAuthTokenVerificationException.class)
    ResponseEntity<ApiErrorResponse> handleOAuthTokenVerification(
            OAuthTokenVerificationException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "oauth_verification_failed", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(NoInvitationException.class)
    ResponseEntity<ApiErrorResponse> handleNoInvitation(
            NoInvitationException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "no_invitation", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(FarmQuotaExceededException.class)
    ResponseEntity<ApiErrorResponse> handleFarmQuotaExceeded(
            FarmQuotaExceededException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.PAYMENT_REQUIRED,
                "farm_quota_exceeded",
                "O seu plano não permite adicionar mais fazendas.",
                Map.of(),
                Map.of("limit", exception.getLimit()),
                request
        );
    }

    @ExceptionHandler(FarmSwitchForbiddenException.class)
    ResponseEntity<ApiErrorResponse> handleFarmSwitchForbidden(
            FarmSwitchForbiddenException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "forbidden", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(InvalidFarmRegistrationException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidFarmRegistration(
            InvalidFarmRegistrationException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "validation_failed", "Dados inválidos.", exception.fieldErrors(), request);
    }

    @ExceptionHandler(InactiveUserException.class)
    ResponseEntity<ApiErrorResponse> handleInactiveUser(
            InactiveUserException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "access_revoked", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(InvalidAuthTokenException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidAuthToken(
            InvalidAuthTokenException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "authentication_failed", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(ExpiredAuthTokenException.class)
    ResponseEntity<ApiErrorResponse> handleExpiredAuthToken(
            ExpiredAuthTokenException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "token_expired", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidRefreshToken(
            InvalidRefreshTokenException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "invalid_refresh_token", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(ExpiredRefreshTokenException.class)
    ResponseEntity<ApiErrorResponse> handleExpiredRefreshToken(
            ExpiredRefreshTokenException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "expired_refresh_token", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(RevokedRefreshTokenException.class)
    ResponseEntity<ApiErrorResponse> handleRevokedRefreshToken(
            RevokedRefreshTokenException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.CONFLICT, "revoked_refresh_token", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(SessionReplacedException.class)
    ResponseEntity<ApiErrorResponse> handleSessionReplaced(
            SessionReplacedException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.CONFLICT, "session_replaced", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(BillingGatewayException.class)
    ResponseEntity<ApiErrorResponse> handleBillingGateway(
            BillingGatewayException exception,
            HttpServletRequest request
    ) {
        log.warn("Billing gateway error (kind={}): {}", exception.kind(), exception.getMessage(), exception);
        return switch (exception.kind()) {
            case NOT_CONFIGURED -> buildErrorResponse(
                    HttpStatus.SERVICE_UNAVAILABLE, "billing_not_configured",
                    "O serviço de pagamento está sendo configurado. Tente mais tarde ou contate o suporte.",
                    Map.of(), request);
            case CLIENT_ERROR -> buildErrorResponse(
                    HttpStatus.BAD_REQUEST, "billing_gateway_client_error",
                    "Não foi possível processar os dados de pagamento. Verifique os dados e tente novamente.",
                    Map.of(), request);
            case SERVER_ERROR -> buildErrorResponse(
                    HttpStatus.BAD_GATEWAY, "billing_gateway_error",
                    "Erro ao processar pagamento. Tente novamente.",
                    Map.of(), request);
        };
    }

    @ExceptionHandler(AnimalQuotaExceededException.class)
    ResponseEntity<ApiErrorResponse> handleAnimalQuotaExceeded(
            AnimalQuotaExceededException exception,
            HttpServletRequest request
    ) {
        Map<String, Object> details = Map.of(
                "plan", exception.plan(),
                "currentCount", exception.currentCount(),
                "planLimit", exception.planLimit(),
                "importCount", exception.importCount()
        );
        return buildErrorResponse(
                HttpStatus.PAYMENT_REQUIRED,
                "animal_quota_exceeded",
                exception.getMessage(),
                Map.of(),
                details,
                request
        );
    }

    @ExceptionHandler(BillingAccessDeniedException.class)
    ResponseEntity<ApiErrorResponse> handleBillingAccessDenied(
            BillingAccessDeniedException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.PAYMENT_REQUIRED,
                "payment_required",
                exception.getMessage(),
                Map.of(),
                exception.details(),
                request
        );
    }

    @ExceptionHandler(InvalidAnimalQueryException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidAnimalQuery(
            InvalidAnimalQueryException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "validation_failed", "Dados inválidos.", exception.fieldErrors(), request);
    }

    @ExceptionHandler(InvalidAnimalMutationException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidAnimalMutation(
            InvalidAnimalMutationException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "validation_failed", "Dados inválidos.", exception.fieldErrors(), request);
    }

    @ExceptionHandler(InvalidLotQueryException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidLotQuery(
            InvalidLotQueryException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "validation_failed", "Dados inválidos.", exception.fieldErrors(), request);
    }

    @ExceptionHandler(InvalidLotUpsertException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidLotUpsert(
            InvalidLotUpsertException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "validation_failed", "Dados inválidos.", exception.fieldErrors(), request);
    }

    @ExceptionHandler(InvalidUserQueryException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidUserQuery(
            InvalidUserQueryException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "validation_failed", "Dados inválidos.", exception.fieldErrors(), request);
    }

    @ExceptionHandler(InvalidUserMutationException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidUserMutation(
            InvalidUserMutationException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "validation_failed", "Dados inválidos.", exception.fieldErrors(), request);
    }

    @ExceptionHandler(InvalidInviteInputException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidInviteInput(
            InvalidInviteInputException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "validation_failed", "Dados inválidos.", exception.fieldErrors(), request);
    }

    @ExceptionHandler(InviteNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleInviteNotFound(
            InviteNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "not_found", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(InviteForbiddenException.class)
    ResponseEntity<ApiErrorResponse> handleInviteForbidden(
            InviteForbiddenException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "forbidden", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(InvalidFarmSettingsMutationException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidFarmSettingsMutation(
            InvalidFarmSettingsMutationException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "validation_failed", "Dados inválidos.", exception.fieldErrors(), request);
    }

    @ExceptionHandler(InvalidFeedingMutationException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidFeedingMutation(
            InvalidFeedingMutationException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "validation_failed", "Dados inválidos.", exception.fieldErrors(), request);
    }

    @ExceptionHandler(InvalidFeedingQueryException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidFeedingQuery(
            InvalidFeedingQueryException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "validation_failed", "Dados inválidos.", exception.fieldErrors(), request);
    }

    @ExceptionHandler(InvalidWeighingMutationException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidWeighingMutation(
            InvalidWeighingMutationException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "validation_failed", "Dados inválidos.", exception.fieldErrors(), request);
    }

    @ExceptionHandler(InvalidWeighingQueryException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidWeighingQuery(
            InvalidWeighingQueryException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "validation_failed", "Dados inválidos.", exception.fieldErrors(), request);
    }

    @ExceptionHandler(InvalidVaccineQueryException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidVaccineQuery(
            InvalidVaccineQueryException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "validation_failed", "Dados inválidos.", exception.fieldErrors(), request);
    }

    @ExceptionHandler(InvalidVaccineMutationException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidVaccineMutation(
            InvalidVaccineMutationException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "validation_failed", "Dados inválidos.", exception.fieldErrors(), request);
    }

    @ExceptionHandler(InvalidMedicationQueryException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidMedicationQuery(
            InvalidMedicationQueryException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "validation_failed", "Dados inválidos.", exception.fieldErrors(), request);
    }

    @ExceptionHandler(InvalidMedicationMutationException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidMedicationMutation(
            InvalidMedicationMutationException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "validation_failed", "Dados inválidos.", exception.fieldErrors(), request);
    }

    @ExceptionHandler(InvalidOccurrenceQueryException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidOccurrenceQuery(
            InvalidOccurrenceQueryException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "validation_failed", "Dados inválidos.", exception.fieldErrors(), request);
    }

    @ExceptionHandler(InvalidWhatsappWebhookPayloadException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidWhatsappWebhookPayload(
            InvalidWhatsappWebhookPayloadException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "validation_failed", "Dados inválidos.", exception.fieldErrors(), request);
    }

    @ExceptionHandler(InvalidWhatsappSupportMessageQueryException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidWhatsappSupportQuery(
            InvalidWhatsappSupportMessageQueryException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "validation_failed", "Dados inválidos.", exception.fieldErrors(), request);
    }

    @ExceptionHandler(InvalidWhatsappWebhookAuthenticationException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidWhatsappWebhookAuthentication(
            InvalidWhatsappWebhookAuthenticationException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "authentication_failed", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(WhatsappMessagePublishFailedException.class)
    ResponseEntity<ApiErrorResponse> handleWhatsappMessagePublishFailure(
            WhatsappMessagePublishFailedException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, "dependency_failed", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(DuplicateUserPhoneException.class)
    ResponseEntity<ApiErrorResponse> handleDuplicateUserPhone(
            DuplicateUserPhoneException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.CONFLICT, "duplicate_phone", exception.getMessage(), exception.fieldErrors(), request);
    }

    @ExceptionHandler(AnimalHasRecordsException.class)
    ResponseEntity<ApiErrorResponse> handleAnimalHasRecords(
            AnimalHasRecordsException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.CONFLICT, "animal_has_records", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(AnimalNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleAnimalNotFound(
            AnimalNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "not_found", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(LotNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleLotNotFound(
            LotNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "not_found", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleUserNotFound(
            UserNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "not_found", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(FarmSettingsNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleFarmSettingsNotFound(
            FarmSettingsNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "not_found", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(OccurrenceNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleOccurrenceNotFound(
            OccurrenceNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "not_found", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(VaccineNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleVaccineNotFound(
            VaccineNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "not_found", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(MedicationNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleMedicationNotFound(
            MedicationNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "not_found", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(WeighingAnimalNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleWeighingAnimalNotFound(
            WeighingAnimalNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "not_found", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(WhatsappSupportMessageNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleWhatsappSupportMessageNotFound(
            WhatsappSupportMessageNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "not_found", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(OfferNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleOfferNotFound(
            OfferNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "not_found", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(AnimalWriteForbiddenException.class)
    ResponseEntity<ApiErrorResponse> handleAnimalWriteForbidden(
            AnimalWriteForbiddenException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "forbidden", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(LotReadForbiddenException.class)
    ResponseEntity<ApiErrorResponse> handleLotReadForbidden(
            LotReadForbiddenException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "forbidden", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(LotWriteForbiddenException.class)
    ResponseEntity<ApiErrorResponse> handleLotWriteForbidden(
            LotWriteForbiddenException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "forbidden", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(UserManagementForbiddenException.class)
    ResponseEntity<ApiErrorResponse> handleUserManagementForbidden(
            UserManagementForbiddenException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "forbidden", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(FarmSettingsForbiddenException.class)
    ResponseEntity<ApiErrorResponse> handleFarmSettingsForbidden(
            FarmSettingsForbiddenException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "forbidden", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(OccurrenceActionForbiddenException.class)
    ResponseEntity<ApiErrorResponse> handleOccurrenceActionForbidden(
            OccurrenceActionForbiddenException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "forbidden", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(OccurrenceQueryForbiddenException.class)
    ResponseEntity<ApiErrorResponse> handleOccurrenceQueryForbidden(
            OccurrenceQueryForbiddenException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "forbidden", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(WeighingWriteForbiddenException.class)
    ResponseEntity<ApiErrorResponse> handleWeighingWriteForbidden(
            WeighingWriteForbiddenException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "forbidden", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(VaccineAccessForbiddenException.class)
    ResponseEntity<ApiErrorResponse> handleVaccineAccessForbidden(
            VaccineAccessForbiddenException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "forbidden", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(MedicationAccessForbiddenException.class)
    ResponseEntity<ApiErrorResponse> handleMedicationAccessForbidden(
            MedicationAccessForbiddenException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "forbidden", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(FeedingAccessForbiddenException.class)
    ResponseEntity<ApiErrorResponse> handleFeedingAccessForbidden(
            FeedingAccessForbiddenException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "forbidden", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(DashboardReadForbiddenException.class)
    ResponseEntity<ApiErrorResponse> handleDashboardReadForbidden(
            DashboardReadForbiddenException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "forbidden", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(OfferAccessForbiddenException.class)
    ResponseEntity<ApiErrorResponse> handleOfferAccessForbidden(
            OfferAccessForbiddenException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "forbidden", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(WhatsappSupportForbiddenException.class)
    ResponseEntity<ApiErrorResponse> handleWhatsappSupportForbidden(
            WhatsappSupportForbiddenException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "forbidden", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(InvalidOccurrenceStateTransitionException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidOccurrenceStateTransition(
            InvalidOccurrenceStateTransitionException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.CONFLICT, "invalid_state_transition", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(WhatsappSupportRetryNotAllowedException.class)
    ResponseEntity<ApiErrorResponse> handleWhatsappSupportRetryNotAllowed(
            WhatsappSupportRetryNotAllowedException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.CONFLICT, "invalid_state_transition", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(InvalidOfferMutationException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidOfferMutation(
            InvalidOfferMutationException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "validation_failed", "Dados inválidos.", exception.fieldErrors(), request);
    }

    @ExceptionHandler(InvalidOfferStateTransitionException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidOfferStateTransition(
            InvalidOfferStateTransitionException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.CONFLICT, "invalid_state_transition", exception.getMessage(), Map.of(), request);
    }

    private ResponseEntity<ApiErrorResponse> buildErrorResponse(
            HttpStatus status,
            String code,
            String message,
            Map<String, String> fieldErrors,
            HttpServletRequest request
    ) {
        String requestId = (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        ApiErrorResponse response = new ApiErrorResponse(code, message, fieldErrors, Map.of(), requestId);
        return ResponseEntity.status(status).body(response);
    }

    private ResponseEntity<ApiErrorResponse> buildErrorResponse(
            HttpStatus status,
            String code,
            String message,
            Map<String, String> fieldErrors,
            Map<String, Object> details,
            HttpServletRequest request
    ) {
        String requestId = (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        ApiErrorResponse response = new ApiErrorResponse(code, message, fieldErrors, details, requestId);
        return ResponseEntity.status(status).body(response);
    }
}
