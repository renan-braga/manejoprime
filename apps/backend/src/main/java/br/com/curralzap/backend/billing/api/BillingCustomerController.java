package br.com.curralzap.backend.billing.api;

import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.billing.application.BillingCustomerData;
import br.com.curralzap.backend.billing.application.BillingCustomerRepository;
import br.com.curralzap.backend.shared.api.ControllerSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing/customer")
class BillingCustomerController {

    private final BillingCustomerRepository customerRepository;

    BillingCustomerController(BillingCustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @GetMapping
    ResponseEntity<BillingCustomerApiResponse> get(HttpServletRequest request) {
        AuthTokenClaims claims = ControllerSupport.requireClaims(request);
        return customerRepository.findByFarmId(claims.farmId())
                .map(d -> ResponseEntity.ok(BillingCustomerApiResponse.from(d)))
                .orElse(ResponseEntity.noContent().build());
    }

    @PutMapping
    ResponseEntity<Void> put(HttpServletRequest request, @RequestBody BillingCustomerApiRequest body) {
        AuthTokenClaims claims = ControllerSupport.requireClaims(request);
        customerRepository.upsert(new BillingCustomerData(
                claims.farmId(),
                body.customerType(),
                body.name(),
                body.document(),
                body.email(),
                body.phone(),
                body.postalCode(),
                body.address(),
                body.addressNumber(),
                body.city(),
                body.state()
        ));
        return ResponseEntity.noContent().build();
    }

    record BillingCustomerApiRequest(
            String customerType,
            String name,
            String document,
            String email,
            String phone,
            String postalCode,
            String address,
            String addressNumber,
            String city,
            String state
    ) {}

    record BillingCustomerApiResponse(
            String customerType,
            String name,
            String document,
            String email,
            String phone,
            String postalCode,
            String address,
            String addressNumber,
            String city,
            String state
    ) {
        static BillingCustomerApiResponse from(BillingCustomerData d) {
            return new BillingCustomerApiResponse(
                    d.customerType(), d.name(), d.document(), d.email(), d.phone(),
                    d.postalCode(), d.address(), d.addressNumber(), d.city(), d.state()
            );
        }
    }
}
