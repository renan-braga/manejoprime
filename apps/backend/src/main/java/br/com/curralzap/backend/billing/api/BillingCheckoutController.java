package br.com.curralzap.backend.billing.api;

import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.billing.application.BillingCheckoutService;
import br.com.curralzap.backend.billing.application.CheckoutRequest;
import br.com.curralzap.backend.billing.application.CheckoutResult;
import br.com.curralzap.backend.shared.api.ControllerSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing")
class BillingCheckoutController {

    private final BillingCheckoutService checkoutService;

    BillingCheckoutController(BillingCheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/checkout")
    ResponseEntity<CheckoutApiResponse> checkout(
            HttpServletRequest request,
            @RequestBody CheckoutApiRequest body
    ) {
        AuthTokenClaims claims = ControllerSupport.requireClaims(request);
        String clientIp = extractClientIp(request);
        CheckoutResult result = checkoutService.checkout(claims.farmId(), body.toCommand(clientIp));
        return ResponseEntity.ok(new CheckoutApiResponse(
                result.paymentMethod(),
                result.invoiceUrl(),
                result.bankSlipUrl(),
                result.boletoCode(),
                result.pixQrCodeImage(),
                result.pixCopyPaste()
        ));
    }

    record CheckoutApiRequest(
            String plan,
            String paymentMethod,
            String billingCycle,
            String customerType,
            String name,
            String document,
            String email,
            String phone,
            String postalCode,
            String address,
            String addressNumber,
            String city,
            String state,
            String cardHolderName,
            String cardNumber,
            String cardExpiryMonth,
            String cardExpiryYear,
            String cardCcv
    ) {
        CheckoutRequest toCommand(String clientIp) {
            return new CheckoutRequest(
                    plan, paymentMethod, billingCycle, customerType,
                    name, document, email, phone,
                    postalCode, address, addressNumber,
                    city, state,
                    cardHolderName, cardNumber, cardExpiryMonth, cardExpiryYear, cardCcv,
                    clientIp
            );
        }
    }

    private static String extractClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    record CheckoutApiResponse(
            String paymentMethod,
            String paymentUrl,
            String bankSlipUrl,
            String boletoCode,
            String pixQrCodeImage,
            String pixCopyPaste
    ) {}
}
