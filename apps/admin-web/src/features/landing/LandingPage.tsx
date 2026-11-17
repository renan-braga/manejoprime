import { useEffect, useRef, useState } from "react";
import { Navigate, useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import {
  Beef, Scale, Wheat, AlertTriangle, Syringe, Pill, HandCoins,
  MessageCircle, ArrowRight, Users, BarChart3, Mic,
  Timer, CheckCircle2, Activity, Coins, Heart, Check, Zap
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { useAuth } from "../auth/AuthContext";
import { BrandLogo } from "@/components/BrandLogo";

const HERO_URL = "https://d2xsxph8kpxj0f.cloudfront.net/310419663031713835/EGxCXdZBgvzjEM88qnvyAz/curralzap-hero-Bnmso3zTKCKvPneL6ofRYr.webp";
const RANCHER_URL = "https://d2xsxph8kpxj0f.cloudfront.net/310419663031713835/EGxCXdZBgvzjEM88qnvyAz/curralzap-whatsapp-field-bH5yYmVqVhdJNX7KRUWiud.webp";

const waFeatures = [
  {
    icon: Scale,
    title: "Pesagem",
    example: '"O boi 347 pesou 520 quilos"',
    benefit: "Peso registrado, ganho diário calculado na hora",
  },
  {
    icon: Wheat,
    title: "Alimentação",
    example: '"Lote engorda tá recebendo 5 kg de ração por cabeça"',
    benefit: "Custo de alimentação atualizado automaticamente",
  },
  {
    icon: AlertTriangle,
    title: "Ocorrência",
    example: '"O boi 347 tá mancando, parece grave"',
    benefit: "Gerente e veterinário recebem alerta na hora",
  },
  {
    icon: Syringe,
    title: "Vacina",
    example: '"Vacinei o lote engorda contra aftosa, segunda dose"',
    benefit: "Calendário vacinal atualizado, próxima dose agendada",
  },
  {
    icon: Pill,
    title: "Medicamento",
    example: '"Apliquei ivermectina no boi 347, carência 35 dias"',
    benefit: "Sistema bloqueia venda do animal até liberar a carência",
  },
];

const modules = [
  { icon: Beef, title: "Rebanho", desc: "Cadastro completo dos seus animais. Brinco, raça, peso, lote — tudo num lugar só.", roles: "Gerente" },
  { icon: Scale, title: "Pesagem", desc: "Registre o peso e veja o ganho diário de cada animal. Saiba quem tá rendendo e quem não tá.", roles: "Todos", wa: true },
  { icon: Wheat, title: "Alimentação", desc: "Controle o que cada lote come e quanto custa. Descubra qual ração dá mais resultado por real investido.", roles: "Gerente", wa: true },
  { icon: AlertTriangle, title: "Ocorrências", desc: "Funcionário viu algo errado? Registra na hora. Gerente e veterinário ficam sabendo antes que piore.", roles: "Todos", wa: true },
  { icon: Syringe, title: "Vacinas", desc: "Calendário vacinal completo. Nunca mais perca prazo de vacinação ou dose de reforço.", roles: "Gerente, Vet.", wa: true },
  { icon: Pill, title: "Medicamentos", desc: "Registre tratamentos e o sistema calcula a carência. Nenhum animal vai pro abate antes da hora.", roles: "Gerente, Vet.", wa: true },
  { icon: HandCoins, title: "Ofertas", desc: "Recebeu proposta de compra ou venda? Simule frete, comissão e Funrural. Veja se vale a pena antes de fechar.", roles: "Gerente" },
  { icon: BarChart3, title: "Relatórios", desc: "Números da fazenda na palma da mão. GMD, custo por arroba, evolução de peso — sem precisar de planilha.", roles: "Gerente, Vet." },
];

const roles = [
  {
    name: "Gerente",
    color: "bg-emerald-600",
    desc: "Visão completa da fazenda. Cadastra animais, gerencia lotes, analisa ofertas de compra e venda, acompanha relatórios e controla quem acessa o quê.",
    icon: Users,
  },
  {
    name: "Veterinário",
    color: "bg-blue-600",
    desc: "Foco na saúde do rebanho. Registra vacinas, aplica medicamentos, resolve ocorrências e acompanha a carência de cada animal tratado.",
    icon: Heart,
  },
  {
    name: "Funcionário",
    color: "bg-orange-500",
    desc: "Mão na massa no campo. Registra pesagem e observa o gado no dia a dia. Se viu algo errado, registra a ocorrência direto pelo WhatsApp.",
    icon: Mic,
  },
];

const plans = [
  {
    name: "Starter",
    price: "R$89,90",
    period: "/mês",
    description: "Para fazendas que estão começando",
    highlight: null,
    features: [
      "Até 200 animais",
      "2 usuários",
      "Registro via WhatsApp",
      "Todos os módulos",
      "Relatórios básicos",
    ],
    cta: "Testar grátis por 30 dias",
    featured: false,
  },
  {
    name: "Pro",
    price: "R$169,90",
    period: "/mês",
    description: "Para fazendas em crescimento",
    highlight: "Mais popular",
    features: [
      "Até 500 animais",
      "5 usuários",
      "Até 2 fazendas",
      "Registro via WhatsApp",
      "Todos os módulos",
      "Relatórios avançados",
    ],
    cta: "Testar grátis por 30 dias",
    featured: true,
  },
  {
    name: "Business",
    price: "R$319,90",
    period: "/mês",
    description: "Para grandes operações e integradores",
    highlight: null,
    features: [
      "Até 2.000 animais",
      "10 usuários",
      "Até 5 fazendas",
      "Registro via WhatsApp",
      "Todos os módulos",
      "Suporte prioritário",
    ],
    cta: "Testar grátis por 30 dias",
    featured: false,
  },
];

const fadeUp = {
  hidden: { opacity: 0, y: 24 },
  visible: (i: number) => ({
    opacity: 1, y: 0,
    transition: { delay: i * 0.08, duration: 0.5, ease: "easeOut" as const }
  }),
};

export function LandingPage() {
  const { status } = useAuth();
  const navigate = useNavigate();
  const scrollRef = useRef<HTMLDivElement>(null);
  const [scrolled, setScrolled] = useState(false);

  function scrollToSection(id: string) {
    const el = document.getElementById(id);
    if (el && scrollRef.current) {
      scrollRef.current.scrollTo({ top: el.offsetTop, behavior: "smooth" });
    }
  }

  useEffect(() => {
    const el = scrollRef.current;
    if (!el) return;
    const onScroll = () => setScrolled(el.scrollTop > 60);
    el.addEventListener("scroll", onScroll, { passive: true });
    return () => el.removeEventListener("scroll", onScroll);
  }, []);

  if (status === "authenticated") {
    return <Navigate replace to="/app" />;
  }

  return (
    <div
      ref={scrollRef}
      className="min-h-screen lg:h-screen lg:overflow-y-scroll lg:snap-y lg:snap-mandatory bg-background"
      style={{ scrollBehavior: "smooth" }}
    >
      {/* Nav */}
      <nav className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
        scrolled
          ? "bg-background/90 backdrop-blur-md border-b border-border"
          : "bg-transparent border-transparent"
      }`}>
        <div className={`max-w-6xl mx-auto px-4 md:px-6 flex items-center justify-between transition-all duration-300 ${
          scrolled ? "h-16" : "h-20 md:h-32"
        }`}>
          <div className={`flex items-center gap-2 md:gap-4 transition-all duration-300 ${
            scrolled ? "" : "[filter:drop-shadow(0_1px_3px_rgba(0,0,0,0.55))]"
          }`}>
            <BrandLogo className={`transition-all duration-300 ${
              scrolled
                ? "w-8 h-8 md:w-12 md:h-12 text-emerald-700"
                : "w-10 h-10 md:w-[5rem] md:h-[5rem] text-white"
            }`} />
            <span className={`font-heading font-bold tracking-tight transition-all duration-300 ${
              scrolled ? "text-xl md:text-3xl text-foreground" : "text-2xl md:text-6xl text-white"
            }`}>ManejoPrime</span>
          </div>
          <Button size="sm" className="gap-1.5 bg-emerald-600 hover:bg-emerald-700" onClick={() => navigate("/login")}>
            Acessar App <ArrowRight className="w-3.5 h-3.5" />
          </Button>
        </div>
      </nav>

      {/* Hero */}
      <section className="relative min-h-screen lg:h-screen lg:snap-start overflow-hidden">
        <img
          src={HERO_URL}
          alt="Fazenda"
          className="absolute inset-0 w-full h-full object-cover animate-kenburns-hero"
        />
        <div className="absolute inset-0 bg-gradient-to-r from-black/75 via-black/45 to-transparent" />
        <div className="relative z-10 max-w-6xl mx-auto px-6 h-full flex items-center pt-14">
            <motion.div
              className="max-w-lg"
              initial={{ opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.7 }}
            >
              <h1 className="font-heading text-4xl lg:text-5xl font-bold text-white leading-tight">
                Seu rebanho na palma da mão.
              </h1>
              <p className="text-white/80 mt-4 text-lg leading-relaxed">
                Gravou um áudio no WhatsApp? Pronto. Pesagem registrada, vacina anotada, ocorrência comunicada. Sem planilha, sem caderninho, sem complicação.
              </p>
              <div className="flex flex-col sm:flex-row gap-3 mt-8">
                <Button size="lg" className="bg-emerald-600 hover:bg-emerald-700 gap-2 text-base" onClick={() => navigate("/login")}>
                  <MessageCircle className="w-5 h-5" /> Testar grátis por 30 dias
                </Button>
                <Button size="lg" variant="outline" className="bg-white/10 border-white/30 text-white hover:bg-white/20 gap-2 text-base" onClick={() => scrollToSection("planos")}>
                  Ver planos
                </Button>
              </div>
              <div className="flex items-center gap-6 mt-6">
                <div className="flex items-center gap-1.5">
                  <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                  <span className="text-white/60 text-sm">30 dias grátis, sem cartão</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <Timer className="w-4 h-4 text-emerald-400" />
                  <span className="text-white/60 text-sm">10 segundos por registro</span>
                </div>
              </div>
            </motion.div>
          </div>
      </section>

      {/* WhatsApp Section */}
      <section id="como-funciona" className="min-h-screen flex items-center bg-emerald-50/50 overflow-hidden lg:snap-start">
        <div className="max-w-6xl mx-auto px-6 w-full py-16">
          <div className="grid lg:grid-cols-2 gap-16 items-center">
            <motion.div
              className="relative"
              initial={{ opacity: 0, x: -40 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.6 }}
            >
              <div className="rounded-2xl overflow-hidden shadow-2xl">
                <img
                  src={RANCHER_URL}
                  alt="Produtor usando WhatsApp no campo"
                  className="w-full h-auto object-cover animate-kenburns-photo"
                />
              </div>
              <motion.div
                className="absolute -bottom-6 -right-4 lg:-right-8 w-56 bg-white rounded-xl shadow-xl border border-emerald-100 p-3"
                initial={{ opacity: 0, y: 20, scale: 0.9 }}
                whileInView={{ opacity: 1, y: 0, scale: 1 }}
                viewport={{ once: true }}
                transition={{ delay: 0.4, duration: 0.5 }}
              >
                <div className="flex items-center gap-2 mb-2">
                  <div className="w-6 h-6 rounded-full bg-emerald-600 flex items-center justify-center">
                    <MessageCircle className="w-3.5 h-3.5 text-white" />
                  </div>
                  <span className="text-xs font-bold text-emerald-700">ManejoPrime</span>
                </div>
                <div className="bg-emerald-50 rounded-lg p-2 mb-1.5">
                  <div className="flex items-center gap-1.5">
                    <Mic className="w-3 h-3 text-emerald-600" />
                    <div className="flex-1 h-1.5 bg-emerald-200 rounded-full">
                      <div className="h-full w-3/4 bg-emerald-500 rounded-full" />
                    </div>
                    <span className="text-[10px] text-emerald-600">0:12</span>
                  </div>
                </div>
                <p className="text-[11px] text-foreground leading-snug">
                  Entendi! <strong>Boi 347</strong> pesou <strong>520 kg</strong>.<br />
                  GMD: +1,2 kg/dia. Confirma?
                </p>
                <div className="flex gap-1.5 mt-2">
                  <div className="flex-1 bg-emerald-600 text-white text-[10px] font-semibold rounded py-1 text-center">Confirmar</div>
                  <div className="flex-1 bg-gray-100 text-gray-500 text-[10px] font-semibold rounded py-1 text-center">Corrigir</div>
                </div>
              </motion.div>
            </motion.div>

            <div>
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ duration: 0.5 }}
              >
                <span className="text-xs font-semibold text-emerald-600 uppercase tracking-wider">O jeito mais fácil</span>
                <h2 className="font-heading text-3xl font-bold mt-2 text-foreground">
                  Fala no WhatsApp, o sistema anota.
                </h2>
                <p className="text-muted-foreground mt-3 leading-relaxed">
                  Seu funcionário não precisa abrir app, digitar nada ou aprender sistema novo. É só gravar um áudio como já faz todo dia. O ManejoPrime entende, organiza e registra.
                </p>
              </motion.div>

              <div className="space-y-4 mt-8">
                {waFeatures.map((f, i) => (
                  <motion.div
                    key={f.title}
                    className="group flex gap-4 p-3 rounded-xl hover:bg-white hover:shadow-sm transition-all"
                    custom={i}
                    initial="hidden"
                    whileInView="visible"
                    viewport={{ once: true }}
                    variants={fadeUp}
                  >
                    <div className="w-10 h-10 rounded-lg bg-emerald-100 flex items-center justify-center shrink-0 group-hover:bg-emerald-600 transition-colors">
                      <f.icon className="w-5 h-5 text-emerald-600 group-hover:text-white transition-colors" />
                    </div>
                    <div>
                      <h4 className="font-heading font-semibold text-sm">{f.title}</h4>
                      <p className="text-xs text-emerald-700 italic mt-0.5">{f.example}</p>
                      <p className="text-xs text-muted-foreground mt-1">{f.benefit}</p>
                    </div>
                  </motion.div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Value Props */}
      <section className="min-h-screen flex items-center border-b border-border lg:snap-start">
        <div className="max-w-6xl mx-auto px-6 w-full py-16">
          <div className="grid sm:grid-cols-3 gap-8">
            {[
              { icon: Timer, value: "10s", label: "por registro", desc: "Áudio enviado, sistema registrado. Sem digitação, sem treinamento, sem demora." },
              { icon: Activity, value: "GMD", label: "automático", desc: "Depois de cada pesagem, você sabe quais animais estão rendendo e quais não estão." },
              { icon: Coins, value: "R$", label: "por arroba", desc: "Veja o custo de produção antes de fechar qualquer negócio. Sem chute, sem planilha." },
            ].map((v, i) => (
              <motion.div
                key={v.label}
                className="text-center"
                custom={i}
                initial="hidden"
                whileInView="visible"
                viewport={{ once: true }}
                variants={fadeUp}
              >
                <div className="w-16 h-16 rounded-2xl bg-emerald-100 flex items-center justify-center mx-auto mb-4">
                  <v.icon className="w-8 h-8 text-emerald-600" />
                </div>
                <div className="font-heading text-2xl font-bold text-emerald-700">{v.value}</div>
                <div className="text-sm font-medium text-foreground">{v.label}</div>
                <p className="text-base text-muted-foreground mt-1">{v.desc}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* Modules */}
      <section id="modulos" className="min-h-screen flex items-center lg:snap-start">
        <div className="max-w-6xl mx-auto px-6 w-full py-16">
          <div className="text-center mb-12">
            <h2 className="font-heading text-3xl font-bold">Tudo que a fazenda precisa</h2>
            <p className="text-muted-foreground mt-2 max-w-lg mx-auto">
              Do registro do bezerro até a simulação de venda. Cada módulo resolve um problema real do dia a dia.
            </p>
          </div>
          <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {modules.map((m, i) => (
              <motion.div
                key={m.title}
                custom={i}
                initial="hidden"
                whileInView="visible"
                viewport={{ once: true }}
                variants={fadeUp}
              >
                <Card className="border border-border hover:shadow-md hover:border-emerald-200 transition-all h-full group">
                  <CardContent className="p-5">
                    <div className="flex items-center justify-between mb-3">
                      <div className="w-9 h-9 rounded-lg bg-emerald-100 flex items-center justify-center group-hover:bg-emerald-600 transition-colors">
                        <m.icon className="w-5 h-5 text-emerald-600 group-hover:text-white transition-colors" />
                      </div>
                      {m.wa && (
                        <span className="text-[10px] font-medium text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-full flex items-center gap-1">
                          <MessageCircle className="w-2.5 h-2.5" /> WhatsApp
                        </span>
                      )}
                    </div>
                    <h3 className="font-heading font-bold text-sm">{m.title}</h3>
                    <p className="text-sm text-muted-foreground mt-1.5 leading-relaxed">{m.desc}</p>
                    <div className="mt-3 pt-3 border-t border-border/50">
                      <span className="text-[10px] text-muted-foreground">Acesso: {m.roles}</span>
                    </div>
                  </CardContent>
                </Card>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* Roles */}
      <section className="min-h-screen flex items-center bg-muted/30 lg:snap-start">
        <div className="max-w-6xl mx-auto px-6 w-full py-16">
          <div className="text-center mb-12">
            <h2 className="font-heading text-3xl font-bold">Cada um no seu papel</h2>
            <p className="text-muted-foreground mt-2 max-w-lg mx-auto">
              O gerente vê tudo. O veterinário cuida da saúde. O funcionário registra o que acontece no campo. Sem confusão.
            </p>
          </div>
          <div className="grid md:grid-cols-3 gap-6">
            {roles.map((r, i) => (
              <motion.div
                key={r.name}
                custom={i}
                initial="hidden"
                whileInView="visible"
                viewport={{ once: true }}
                variants={fadeUp}
              >
                <Card className="border border-border hover:shadow-md transition-all h-full">
                  <CardContent className="p-6">
                    <div className="flex items-center gap-3 mb-4">
                      <div className={`w-10 h-10 rounded-lg ${r.color} flex items-center justify-center`}>
                        <r.icon className="w-5 h-5 text-white" />
                      </div>
                      <h3 className="font-heading font-bold text-lg">{r.name}</h3>
                    </div>
                    <p className="text-sm text-muted-foreground leading-relaxed">{r.desc}</p>
                  </CardContent>
                </Card>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* Pricing */}
      <section id="planos" className="min-h-screen flex items-center bg-emerald-50/50 lg:snap-start">
        <div className="max-w-6xl mx-auto px-6 w-full py-16">
          <div className="text-center mb-12">
            <span className="text-xs font-semibold text-emerald-600 uppercase tracking-wider">Preços</span>
            <h2 className="font-heading text-3xl font-bold mt-2">Planos que cabem na fazenda</h2>
            <p className="text-muted-foreground mt-2 max-w-lg mx-auto">
              30 dias grátis para testar tudo, sem cartão de crédito. Cancele quando quiser.
            </p>
          </div>
          <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5 max-w-4xl mx-auto">
            {plans.map((plan, i) => (
              <motion.div
                key={plan.name}
                custom={i}
                initial="hidden"
                whileInView="visible"
                viewport={{ once: true }}
                variants={fadeUp}
                className="relative h-full"
              >
                {plan.highlight && (
                  <div className="absolute -top-3 left-1/2 -translate-x-1/2 z-10">
                    <span className={`flex items-center gap-1 text-[10px] font-bold px-3 py-1 rounded-full ${plan.featured ? "bg-emerald-600 text-white" : "bg-amber-400 text-amber-900"}`}>
                      {plan.featured && <Zap className="w-2.5 h-2.5" />}
                      {plan.highlight}
                    </span>
                  </div>
                )}
                <Card className={`border h-full transition-all ${plan.featured ? "border-emerald-500 shadow-lg shadow-emerald-100" : "border-border hover:shadow-md hover:border-emerald-200"}`}>
                  <CardContent className="p-6 flex flex-col h-full">
                    <div>
                      <h3 className="font-heading font-bold text-lg">{plan.name}</h3>
                      <div className="flex items-end gap-1 mt-2">
                        <span className="font-heading text-3xl font-bold text-foreground">{plan.price}</span>
                        <span className="text-sm text-muted-foreground mb-1">{plan.period}</span>
                      </div>
                      <p className="text-xs text-muted-foreground mt-1.5">{plan.description}</p>
                    </div>
                    <ul className="space-y-2.5 mt-5 flex-1">
                      {plan.features.map((f) => (
                        <li key={f} className="flex items-start gap-2 text-sm">
                          <Check className="w-3.5 h-3.5 text-emerald-600 shrink-0 mt-0.5" />
                          <span className="text-foreground/80">{f}</span>
                        </li>
                      ))}
                    </ul>
                    <Button
                      className={`w-full mt-6 ${plan.featured ? "bg-emerald-600 hover:bg-emerald-700" : "variant-outline"}`}
                      variant={plan.featured ? "default" : "outline"}
                      onClick={() => navigate("/login")}
                    >
                      {plan.cta}
                    </Button>
                  </CardContent>
                </Card>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA + Footer */}
      <section className="min-h-screen flex flex-col lg:snap-start">
        <div className="flex-1 flex items-center justify-center">
          <div className="max-w-6xl mx-auto px-6 text-center">
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.5 }}
            >
              <h2 className="font-heading text-3xl font-bold">Comece seu teste grátis hoje</h2>
              <p className="text-muted-foreground mt-3 mb-2 max-w-md mx-auto">
                30 dias para explorar tudo. Sem cartão de crédito, sem compromisso.
              </p>
              <p className="text-sm text-emerald-600 font-medium mb-8">
                Após o teste, escolha o plano a partir de R$89,90/mês.
              </p>
              <Button size="lg" className="bg-emerald-600 hover:bg-emerald-700 gap-2 text-base" onClick={() => navigate("/login")}>
                Testar grátis por 30 dias <ArrowRight className="w-5 h-5" />
              </Button>
            </motion.div>
          </div>
        </div>
        <footer className="border-t border-border pt-12 pb-8">
          <div className="max-w-6xl mx-auto px-6">
            {/* Top row */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-8 mb-10">
              {/* Brand */}
              <div className="col-span-2 md:col-span-1">
                <div className="flex items-center gap-2 mb-3">
                  <BrandLogo className="w-6 h-6 text-foreground" />
                  <span className="font-heading font-bold text-base">ManejoPrime</span>
                </div>
                <p className="text-xs text-muted-foreground leading-relaxed">
                  Gestão pecuária simples e inteligente. Do campo ao relatório, sem complicação.
                </p>
              </div>

              {/* Produto */}
              <div>
                <p className="text-xs font-semibold uppercase tracking-wider text-foreground mb-3">Produto</p>
                <ul className="space-y-2">
                  {[
                    { label: "Como funciona", id: "como-funciona" },
                    { label: "Módulos", id: "modulos" },
                    { label: "Planos", id: "planos" },
                    { label: "WhatsApp", id: "como-funciona" },
                  ].map((item) => (
                    <li key={item.label}>
                      <button
                        onClick={() => scrollToSection(item.id)}
                        className="text-xs text-muted-foreground hover:text-foreground transition-colors"
                      >
                        {item.label}
                      </button>
                    </li>
                  ))}
                </ul>
              </div>

              {/* Legal */}
              <div>
                <p className="text-xs font-semibold uppercase tracking-wider text-foreground mb-3">Legal</p>
                <ul className="space-y-2">
                  {[
                    { label: "Política de Privacidade", href: "/privacy" },
                    { label: "Termos de Uso", href: "/terms" },
                    { label: "LGPD", href: "/privacy#lgpd" },
                  ].map((item) => (
                    <li key={item.label}>
                      <a
                        href={item.href}
                        className="text-xs text-muted-foreground hover:text-foreground transition-colors"
                      >
                        {item.label}
                      </a>
                    </li>
                  ))}
                </ul>
              </div>

              {/* Contato */}
              <div>
                <p className="text-xs font-semibold uppercase tracking-wider text-foreground mb-3">Contato</p>
                <ul className="space-y-2">
                  <li className="text-xs text-muted-foreground">contato@bovfort.com.br</li>
                  <li className="text-xs text-muted-foreground">Suporte via WhatsApp</li>
                  <li className="text-xs text-muted-foreground">Seg–Sex, 8h–18h</li>
                </ul>
              </div>
            </div>

            {/* Bottom row */}
            <div className="border-t border-border pt-6 flex flex-col sm:flex-row items-center justify-between gap-3">
              <p className="text-xs text-muted-foreground">
                © {new Date().getFullYear()} ManejoPrime · CNPJ XX.XXX.XXX/0001-XX · Todos os direitos reservados
              </p>
              <div className="flex items-center gap-4">
                <span className="text-xs text-muted-foreground flex items-center gap-1">
                  🔒 SSL Seguro
                </span>
                <span className="text-xs text-muted-foreground flex items-center gap-1">
                  🛡️ LGPD Compliant
                </span>
              </div>
            </div>
          </div>
        </footer>
      </section>
    </div>
  );
}
