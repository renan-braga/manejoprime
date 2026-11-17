import { MessageCircle, Mail } from "lucide-react";
import { SUPPORT_EMAIL, SUPPORT_WHATSAPP_URL } from "../config/support";

export function SupportLinks() {
  return (
    <div className="flex items-center gap-3 text-sm text-muted-foreground">
      <a
        href={SUPPORT_WHATSAPP_URL}
        target="_blank"
        rel="noopener noreferrer"
        className="flex items-center gap-1.5 hover:text-green-600 transition-colors"
      >
        <MessageCircle className="w-4 h-4 text-green-500" />
        <span>WhatsApp</span>
      </a>
      <span className="text-border">|</span>
      <a
        href={`mailto:${SUPPORT_EMAIL}`}
        className="flex items-center gap-1.5 hover:text-foreground transition-colors"
      >
        <Mail className="w-4 h-4" />
        <span>Dúvidas? Fale conosco</span>
      </a>
    </div>
  );
}
