package br.com.curralzap.backend.offers.api;

import br.com.curralzap.backend.shared.api.ControllerSupport;
import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.offers.application.OfferService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/offers")
class OffersController {

    private final OfferService offerService;

    OffersController(OfferService offerService) {
        this.offerService = offerService;
    }

    @GetMapping
    OfferListApiResponse listOffers(
            HttpServletRequest request,
            @RequestParam(required = false) String status
    ) {
        return OfferListApiResponse.from(offerService.listOffers(requireClaims(request), status));
    }

    @PostMapping
    OfferApiResponse createOffer(
            HttpServletRequest request,
            @RequestBody OfferMutationApiRequest body
    ) {
        return OfferApiResponse.from(offerService.createOffer(requireClaims(request), body.toInput()));
    }

    @PatchMapping("/{offerId}")
    OfferApiResponse updateOffer(
            HttpServletRequest request,
            @PathVariable UUID offerId,
            @RequestBody OfferMutationApiRequest body
    ) {
        return OfferApiResponse.from(offerService.updateOffer(requireClaims(request), offerId, body.toInput()));
    }

    @PostMapping("/{offerId}/activate")
    OfferApiResponse activateOffer(
            HttpServletRequest request,
            @PathVariable UUID offerId
    ) {
        return OfferApiResponse.from(offerService.activateOffer(requireClaims(request), offerId));
    }

    @PostMapping("/{offerId}/cancel")
    OfferApiResponse cancelOffer(
            HttpServletRequest request,
            @PathVariable UUID offerId
    ) {
        return OfferApiResponse.from(offerService.cancelOffer(requireClaims(request), offerId));
    }

    @PostMapping("/{offerId}/mark-sold")
    OfferApiResponse markOfferSold(
            HttpServletRequest request,
            @PathVariable UUID offerId
    ) {
        return OfferApiResponse.from(offerService.markOfferSold(requireClaims(request), offerId));
    }

    private AuthTokenClaims requireClaims(HttpServletRequest request) {
        return ControllerSupport.requireClaims(request);
    }
}
