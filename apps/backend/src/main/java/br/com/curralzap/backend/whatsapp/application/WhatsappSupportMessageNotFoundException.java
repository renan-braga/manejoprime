package br.com.curralzap.backend.whatsapp.application;

public class WhatsappSupportMessageNotFoundException extends RuntimeException {

    public WhatsappSupportMessageNotFoundException() {
        super("Mensagem do WhatsApp nao encontrada.");
    }
}
