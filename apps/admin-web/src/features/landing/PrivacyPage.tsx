import { useNavigate } from "react-router-dom";
import { ArrowLeft } from "lucide-react";
import { Button } from "@/components/ui/button";
import { BrandLogo } from "@/components/BrandLogo";

export function PrivacyPage() {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-background">
      <nav className="border-b border-border">
        <div className="max-w-3xl mx-auto px-6 h-14 flex items-center justify-between">
          <div className="flex items-center gap-2 cursor-pointer" onClick={() => navigate("/")}>
            <BrandLogo className="w-6 h-6" />
            <span className="font-heading font-bold text-base">ManejoPrime</span>
          </div>
          <Button variant="ghost" size="sm" onClick={() => navigate(-1)} className="gap-1.5">
            <ArrowLeft className="w-4 h-4" /> Voltar
          </Button>
        </div>
      </nav>

      <main className="max-w-3xl mx-auto px-6 py-12">
        <h1 className="font-heading text-3xl font-bold mb-2">Política de Privacidade</h1>
        <p className="text-sm text-muted-foreground mb-10">Última atualização: janeiro de 2025</p>

        <div className="prose prose-sm max-w-none space-y-8 text-foreground">

          <section>
            <h2 className="font-heading text-xl font-semibold mb-3">1. Quem somos</h2>
            <p className="text-muted-foreground leading-relaxed">
              O ManejoPrime é uma plataforma de gestão pecuária operada por <strong>[Razão Social Ltda.]</strong>,
              inscrita no CNPJ sob o nº <strong>XX.XXX.XXX/0001-XX</strong>, com sede em <strong>[Cidade/UF]</strong>.
              Para dúvidas sobre esta política, entre em contato pelo e-mail <strong>privacidade@bovfort.com.br</strong>.
            </p>
          </section>

          <section>
            <h2 className="font-heading text-xl font-semibold mb-3">2. Dados que coletamos</h2>
            <p className="text-muted-foreground leading-relaxed mb-3">Coletamos apenas os dados necessários para a prestação do serviço:</p>
            <ul className="list-disc pl-5 space-y-1 text-muted-foreground">
              <li><strong>Dados de conta:</strong> nome, e-mail e foto de perfil fornecidos via login social (Google, Microsoft ou Facebook).</li>
              <li><strong>Dados da fazenda:</strong> nome da propriedade, animais, lotes, pesagens, vacinas, medicamentos e ocorrências cadastrados por você.</li>
              <li><strong>Dados de uso:</strong> logs de acesso, endereço IP e informações do dispositivo para fins de segurança e depuração.</li>
              <li><strong>Mensagens WhatsApp:</strong> transcrições de áudios enviados ao bot para registro de eventos — processadas por IA e descartadas após extração.</li>
            </ul>
          </section>

          <section>
            <h2 className="font-heading text-xl font-semibold mb-3">3. Como usamos seus dados</h2>
            <ul className="list-disc pl-5 space-y-1 text-muted-foreground">
              <li>Operar e melhorar a plataforma.</li>
              <li>Autenticar usuários e controlar acesso por papel (admin, veterinário, funcionário).</li>
              <li>Enviar notificações relacionadas ao serviço (alertas de ocorrência, carências, etc.).</li>
              <li>Cumprir obrigações legais e regulatórias.</li>
            </ul>
            <p className="text-muted-foreground leading-relaxed mt-3">
              Não vendemos, alugamos nem compartilhamos seus dados com terceiros para fins publicitários.
            </p>
          </section>

          <section>
            <h2 className="font-heading text-xl font-semibold mb-3">4. Armazenamento e segurança</h2>
            <p className="text-muted-foreground leading-relaxed">
              Os dados são armazenados em servidores na nuvem com criptografia em trânsito (TLS) e em repouso (AES-256).
              O acesso é restrito a colaboradores com necessidade operacional e protegido por autenticação multifator.
            </p>
          </section>

          <section id="lgpd">
            <h2 className="font-heading text-xl font-semibold mb-3">5. Seus direitos (LGPD)</h2>
            <p className="text-muted-foreground leading-relaxed mb-3">
              Nos termos da Lei Geral de Proteção de Dados (Lei nº 13.709/2018), você tem direito a:
            </p>
            <ul className="list-disc pl-5 space-y-1 text-muted-foreground">
              <li>Confirmar a existência de tratamento dos seus dados.</li>
              <li>Acessar, corrigir ou atualizar seus dados.</li>
              <li>Solicitar a anonimização, bloqueio ou eliminação de dados desnecessários.</li>
              <li>Revogar o consentimento a qualquer momento.</li>
              <li>Solicitar a portabilidade dos dados para outro fornecedor.</li>
            </ul>
            <p className="text-muted-foreground leading-relaxed mt-3">
              Para exercer qualquer um desses direitos, envie e-mail para <strong>privacidade@bovfort.com.br</strong>.
              Responderemos em até 15 dias úteis.
            </p>
          </section>

          <section>
            <h2 className="font-heading text-xl font-semibold mb-3">6. Cookies</h2>
            <p className="text-muted-foreground leading-relaxed">
              Utilizamos cookies estritamente necessários para autenticação e preferências de sessão.
              Não utilizamos cookies de rastreamento ou publicidade.
            </p>
          </section>

          <section>
            <h2 className="font-heading text-xl font-semibold mb-3">7. Alterações nesta política</h2>
            <p className="text-muted-foreground leading-relaxed">
              Podemos atualizar esta política periodicamente. Notificaremos usuários ativos por e-mail com
              pelo menos 15 dias de antecedência em caso de mudanças materiais.
            </p>
          </section>

          <section>
            <h2 className="font-heading text-xl font-semibold mb-3">8. Contato</h2>
            <p className="text-muted-foreground leading-relaxed">
              Encarregado de Proteção de Dados (DPO): <strong>[Nome do DPO]</strong><br />
              E-mail: <strong>privacidade@bovfort.com.br</strong>
            </p>
          </section>
        </div>
      </main>
    </div>
  );
}
