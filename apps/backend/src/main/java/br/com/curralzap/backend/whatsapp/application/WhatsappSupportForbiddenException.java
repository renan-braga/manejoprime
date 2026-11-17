package br.com.curralzap.backend.whatsapp.application;

public class WhatsappSupportForbiddenException extends RuntimeException {

    public WhatsappSupportForbiddenException() {
        super("Usuario sem permissao para acessar suporte do WhatsApp.");
    }
}
