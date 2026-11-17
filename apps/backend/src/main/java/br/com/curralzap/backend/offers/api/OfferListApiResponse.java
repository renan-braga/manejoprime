package br.com.curralzap.backend.offers.api;

import br.com.curralzap.backend.offers.application.OfferListItemView;

import java.util.List;

record OfferListApiResponse(List<OfferListItemApiResponse> items) {

    static OfferListApiResponse from(List<OfferListItemView> views) {
        return new OfferListApiResponse(views.stream().map(OfferListItemApiResponse::from).toList());
    }
}
