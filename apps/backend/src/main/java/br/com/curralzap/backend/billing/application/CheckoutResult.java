package br.com.curralzap.backend.billing.application;

public record CheckoutResult(
        String paymentMethod,
        String invoiceUrl,
        String bankSlipUrl,
        String boletoCode,
        String pixQrCodeImage,
        String pixCopyPaste
) {}
