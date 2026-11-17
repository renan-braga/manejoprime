import { useNavigate } from "react-router-dom";
import { ArrowLeft } from "lucide-react";
import { Button } from "@/components/ui/button";
import { BrandLogo } from "@/components/BrandLogo";

export function TermsPage() {
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
        <h1 className="font-heading text-3xl font-bold mb-2">Termos de Uso</h1>
        <p className="text-sm text-muted-foreground mb-10">Última atualização: janeiro de 2025</p>

        <div className="prose prose-sm max-w-none space-y-8 text-foreground">

          <section>
            <h2 className="font-heading text-xl font-semibold mb-3">1. Aceitação</h2>
            <p className="text-muted-foreground leading-relaxed">
              Ao acessar ou utilizar o ManejoPrime, você concorda com estes Termos de Uso e com nossa
              Política de Privacidade. Se não concordar, não utilize o serviço.
            </p>
          </section>

          <section>
            <h2 className="font-heading text-xl font-semibold mb-3">2. O serviço</h2>
            <p className="text-muted-foreground leading-relaxed">
              O ManejoPrime é uma plataforma SaaS de gestão pecuária que permite registrar animais, lotes,
              pesagens, vacinas, medicamentos, ocorrências e ofertas, incluindo registro via mensagem de
              áudio no WhatsApp. O serviço é disponibilizado "como está" e pode ser atualizado a qualquer momento.
            </p>
          </section>

          <section>
            <h2 className="font-heading text-xl font-semibold mb-3">3. Conta e acesso</h2>
            <ul className="list-disc pl-5 space-y-1 text-muted-foreground">
              <li>O acesso ao ManejoPrime é mediante convite do administrador da fazenda.</li>
              <li>Você é responsável pela confidencialidade da sua conta.</li>
              <li>É proibido compartilhar credenciais de acesso com terceiros.</li>
              <li>O administrador pode remover usuários ou alterar permissões a qualquer momento.</li>
            </ul>
          </section>

          <section>
            <h2 className="font-heading text-xl font-semibold mb-3">4. Uso aceitável</h2>
            <p className="text-muted-foreground leading-relaxed mb-3">É proibido utilizar o ManejoPrime para:</p>
            <ul className="list-disc pl-5 space-y-1 text-muted-foreground">
              <li>Cadastrar informações falsas ou fraudulentas.</li>
              <li>Tentar acessar dados de outras fazendas.</li>
              <li>Realizar engenharia reversa, scraping ou uso automatizado não autorizado.</li>
              <li>Qualquer atividade ilegal conforme a legislação brasileira.</li>
            </ul>
          </section>

          <section>
            <h2 className="font-heading text-xl font-semibold mb-3">5. Planos e pagamentos</h2>
            <p className="text-muted-foreground leading-relaxed">
              O ManejoPrime oferece planos gratuitos e pagos. Os valores, limites e condições de cada plano
              estão descritos na página de Planos e podem ser alterados com aviso prévio de 30 dias.
              Planos pagos são cobrados mensalmente e podem ser cancelados a qualquer momento, sem multa.
            </p>
          </section>

          <section>
            <h2 className="font-heading text-xl font-semibold mb-3">6. Propriedade dos dados</h2>
            <p className="text-muted-foreground leading-relaxed">
              Todos os dados cadastrados por você na plataforma são de sua propriedade.
              O ManejoPrime não reivindica nenhum direito sobre eles. Você pode exportar ou solicitar
              a exclusão dos seus dados a qualquer momento.
            </p>
          </section>

          <section>
            <h2 className="font-heading text-xl font-semibold mb-3">7. Disponibilidade</h2>
            <p className="text-muted-foreground leading-relaxed">
              Nos esforçamos para manter o ManejoPrime disponível 24/7, mas não garantimos disponibilidade
              ininterrupta. Manutenções programadas serão comunicadas com antecedência.
            </p>
          </section>

          <section>
            <h2 className="font-heading text-xl font-semibold mb-3">8. Limitação de responsabilidade</h2>
            <p className="text-muted-foreground leading-relaxed">
              O ManejoPrime não se responsabiliza por decisões tomadas com base nas informações registradas
              na plataforma. As informações são de responsabilidade exclusiva do usuário que as cadastrou.
            </p>
          </section>

          <section>
            <h2 className="font-heading text-xl font-semibold mb-3">9. Rescisão</h2>
            <p className="text-muted-foreground leading-relaxed">
              Podemos suspender ou encerrar sua conta em caso de violação destes termos, sem aviso prévio.
              Você pode encerrar sua conta a qualquer momento através das configurações ou por e-mail.
            </p>
          </section>

          <section>
            <h2 className="font-heading text-xl font-semibold mb-3">10. Lei aplicável</h2>
            <p className="text-muted-foreground leading-relaxed">
              Estes termos são regidos pelas leis da República Federativa do Brasil.
              Fica eleito o foro da comarca de <strong>[Cidade/UF]</strong> para dirimir quaisquer controvérsias.
            </p>
          </section>

          <section>
            <h2 className="font-heading text-xl font-semibold mb-3">11. Contato</h2>
            <p className="text-muted-foreground leading-relaxed">
              Dúvidas sobre estes termos: <strong>contato@bovfort.com.br</strong>
            </p>
          </section>
        </div>
      </main>
    </div>
  );
}
