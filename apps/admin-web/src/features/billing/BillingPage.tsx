import { FormEvent, useState, useCallback, useEffect, useRef } from "react";
import {
  AlertCircle,
  Building2,
  CheckCircle2,
  CreditCard,
  RotateCcw,
  ShieldCheck,
} from "lucide-react";
import { useAuth, type BillingContext } from "../auth/AuthContext";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

type BillingFormState = {
  customerType: "individual" | "company";
  name: string;
  document: string;
  email: string;
  phone: string;
  postalCode: string;
  address: string;
  number: string;
  city: string;
  state: string;
};

type BillingCycle = "monthly" | "annual";

type CardFormState = {
  holderName: string;
  number: string;
  expiryMonth: string;
  expiryYear: string;
  ccv: string;
};

const INITIAL_CARD: CardFormState = {
  holderName: "",
  number: "",
  expiryMonth: "",
  expiryYear: "",
  ccv: "",
};

const PLAN_DETAILS: Record<string, {
  label: string;
  monthlyPrice: string;
  annualPrice: string;
  animals: string;
  users: string;
  farms: string;
  whatsapp: string;
}> = {
  free:       { label: "Free",       monthlyPrice: "R$ 0",             annualPrice: "R$ 0",              animals: "50",        users: "1",         farms: "1",         whatsapp: "10/dia" },
  trialing:   { label: "Teste grátis", monthlyPrice: "R$ 0",           annualPrice: "R$ 0",              animals: "Ilimitado", users: "Ilimitado", farms: "2",         whatsapp: "Ilimitado" },
  starter:    { label: "Starter",    monthlyPrice: "R$ 89,90/mês",     annualPrice: "R$ 899,00/ano",     animals: "200",       users: "2",         farms: "1",         whatsapp: "Ilimitado" },
  pro:        { label: "Pro",        monthlyPrice: "R$ 169,90/mês",    annualPrice: "R$ 1.699,00/ano",   animals: "500",       users: "5",         farms: "2",         whatsapp: "Ilimitado" },
  business:   { label: "Business",   monthlyPrice: "R$ 319,90/mês",    annualPrice: "R$ 3.199,00/ano",   animals: "2.000",     users: "10",        farms: "5",         whatsapp: "Ilimitado" },
  enterprise: { label: "Enterprise", monthlyPrice: "R$ 499,90/mês",    annualPrice: "R$ 4.999,00/ano",   animals: "Ilimitado", users: "Ilimitado", farms: "Ilimitado", whatsapp: "Ilimitado" },
  internal:   { label: "Interno",    monthlyPrice: "Isento",           annualPrice: "Isento",            animals: "Ilimitado", users: "Ilimitado", farms: "Ilimitado", whatsapp: "Ilimitado" },
};


const INITIAL_FORM: BillingFormState = {
  customerType: "company",
  name: "",
  document: "",
  email: "",
  phone: "",
  postalCode: "",
  address: "",
  number: "",
  city: "",
  state: "",
};

function getPriceLabel(plan: (typeof PLAN_DETAILS)[string], cycle: BillingCycle) {
  return cycle === "annual" ? plan.annualPrice : plan.monthlyPrice;
}

export function BillingPage() {
  const { session, fetchProtectedJson, refreshBillingStatus } = useAuth();
  const checkoutRef = useRef<HTMLElement>(null);

  useEffect(() => {
    if (window.location.hash === "#checkout") {
      checkoutRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
    }
  }, []);

  const billing = normalizeBilling(session?.billing);
  const plan = PLAN_DETAILS[billing.plan] ?? PLAN_DETAILS.free;
  const [form, setForm] = useState<BillingFormState>({
    ...INITIAL_FORM,
    name: session?.farmName ?? "",
    email: session?.email ?? "",
    phone: formatPhone(session?.phone ?? ""),
  });
  const [savedForm, setSavedForm] = useState<BillingFormState | null>(null);
  const [saveState, setSaveState] = useState<"idle" | "saving" | "saved" | "error">("idle");

  useEffect(() => {
    fetchProtectedJson<{
      customerType?: string; name?: string; document?: string; email?: string;
      phone?: string; postalCode?: string; address?: string; addressNumber?: string;
      city?: string; state?: string;
    }>("/api/v1/billing/customer").then((data) => {
      if (data == null) return;
      const loaded: BillingFormState = {
        customerType: (data.customerType as BillingFormState["customerType"]) ?? "company",
        name: data.name ?? session?.farmName ?? "",
        document: data.document ?? "",
        email: data.email ?? session?.email ?? "",
        phone: data.phone ?? session?.phone ?? "",
        postalCode: data.postalCode ?? "",
        address: data.address ?? "",
        number: data.addressNumber ?? "",
        city: data.city ?? "",
        state: data.state ?? "",
      };
      setForm(loaded);
      setSavedForm(loaded);
    }).catch(() => {});
  }, []);

  const [selectedPlan, setSelectedPlan] = useState(["starter", "pro", "business"].includes(billing.plan) ? billing.plan : "starter");
  const [billingCycle, setBillingCycle] = useState<BillingCycle>("monthly");
  const [card, setCard] = useState<CardFormState>(INITIAL_CARD);
  const [checkoutState, setCheckoutState] = useState<"idle" | "loading" | "waiting">("idle");
  const [checkoutError, setCheckoutError] = useState<string | null>(null);
  const [isCheckingStatus, setIsCheckingStatus] = useState(false);
  const [paymentSuccess, setPaymentSuccess] = useState(false);

  const selectedPlanDetails = PLAN_DETAILS[selectedPlan] ?? PLAN_DETAILS.starter;
  const selectedPriceLabel = getPriceLabel(selectedPlanDetails, billingCycle);
  const documentTyped = form.document.replace(/\D/g, "").length > 0;
  const documentValid = isDocumentValid(form);
  const billingComplete = isBillingFormComplete(form);
  const formComplete = billingComplete && isCardFormComplete(card);
  const status = billingStatusPresentation(billing);
  const billingBlocked = !billing.canWrite;
  const billingWarns = billing.status === "past_due" || billingBlocked;

  function resetCheckout() {
    setCheckoutState("idle");
    setCheckoutError(null);
    setPaymentSuccess(false);
  }

  function updateCard<K extends keyof CardFormState>(key: K, value: CardFormState[K]) {
    setCard((c) => ({ ...c, [key]: value }));
    resetCheckout();
  }

  function updateForm<K extends keyof BillingFormState>(key: K, value: BillingFormState[K]) {
    setForm((current) => ({ ...current, [key]: value }));
    resetCheckout();
  }

  async function handleSaveBillingData(event: FormEvent) {
    event.preventDefault();
    if (!formComplete) return;
    setSaveState("saving");
    try {
      await fetchProtectedJson("/api/v1/billing/customer", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          customerType: form.customerType,
          name: form.name,
          document: form.document,
          email: form.email,
          phone: form.phone.replace(/\D/g, ""),
          postalCode: form.postalCode,
          address: form.address,
          addressNumber: form.number,
          city: form.city,
          state: form.state,
        }),
      });
      setSavedForm(form);
      setSaveState("saved");
    } catch {
      setSaveState("error");
    }
  }

  const handleRealizarPagamento = useCallback(async () => {
    if (!formComplete) return;
    setCheckoutState("loading");
    setCheckoutError(null);
    setPaymentSuccess(false);

    try {
      await fetchProtectedJson<unknown>("/api/v1/billing/checkout", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          plan: selectedPlan,
          paymentMethod: "card",
          billingCycle,
          customerType: form.customerType,
          name: form.name,
          document: form.document,
          email: form.email,
          phone: form.phone.replace(/\D/g, ""),
          postalCode: form.postalCode,
          address: form.address,
          addressNumber: form.number,
          city: form.city,
          state: form.state,
          cardHolderName: card.holderName,
          cardNumber: card.number.replace(/\D/g, ""),
          cardExpiryMonth: card.expiryMonth,
          cardExpiryYear: card.expiryYear,
          cardCcv: card.ccv,
        }),
      });
      setCheckoutState("waiting");
    } catch (error) {
      setCheckoutError(describeBillingCheckoutError(error));
      setCheckoutState("idle");
    }
  }, [formComplete, fetchProtectedJson, selectedPlan, billingCycle, form, card]);

  useEffect(() => {
    if (checkoutState !== "waiting") return;

    async function checkStatus() {
      const billingStatus = await refreshBillingStatus();
      if (billingStatus?.status === "active") {
        setCheckoutState("idle");
        setPaymentSuccess(true);
      }
    }

    // Card is charged immediately — check after 1.5s, then every 10s as fallback
    const initialTimer = setTimeout(checkStatus, 1500);
    const pollingInterval = setInterval(checkStatus, 10_000);

    return () => {
      clearTimeout(initialTimer);
      clearInterval(pollingInterval);
    };
  }, [checkoutState, refreshBillingStatus]);

  const handleCheckPaymentStatus = useCallback(async () => {
    if (isCheckingStatus) return;
    setIsCheckingStatus(true);
    try {
      const billingStatus = await refreshBillingStatus();
      if (billingStatus?.status === "active") {
        setCheckoutState("idle");
        setPaymentSuccess(true);
      }
    } finally {
      setIsCheckingStatus(false);
    }
  }, [isCheckingStatus, refreshBillingStatus]);

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
        <div>
          <h1 className="font-heading text-2xl font-bold tracking-tight">Plano e cobrança</h1>
          <p className="mt-1 text-sm text-muted-foreground">{session?.farmName ?? "Fazenda"} · cobrança da conta</p>
        </div>
        <Badge className={status.className} variant="outline">
          {status.icon}
          {status.label}
        </Badge>
      </div>

      {billing.status === "trialing" && billing.currentPeriodEnd != null && (
        <section className="rounded-md border border-amber-200 bg-amber-50 p-4 dark:border-amber-700 dark:bg-amber-950/20">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex gap-3">
              <ShieldCheck className="mt-0.5 h-5 w-5 shrink-0 text-amber-600" />
              <div>
                <h2 className="text-sm font-semibold text-amber-900 dark:text-amber-100">
                  Teste grátis · {trialDaysRemaining(billing.currentPeriodEnd)} dias restantes
                </h2>
                <p className="text-sm text-amber-800 opacity-90 dark:text-amber-200">
                  Período de teste encerra em {formatDate(billing.currentPeriodEnd)}. Assine para continuar com acesso completo.
                </p>
              </div>
            </div>
          </div>
        </section>
      )}

      {billingWarns ? (
        <section className={`rounded-md border p-4 ${billingBlocked ? "border-destructive/40 bg-destructive/5" : "border-amber-300 bg-amber-50 text-amber-950 dark:border-amber-700 dark:bg-amber-950/20 dark:text-amber-100"}`}>
          <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
            <div className="flex gap-3">
              <AlertCircle className="mt-0.5 h-5 w-5 shrink-0" />
              <div>
                <h2 className="text-sm font-semibold">{billingBlocked ? "Plano suspenso" : "Pagamento pendente"}</h2>
                <p className="text-sm opacity-90">
                  {billingBlocked
                    ? "Novos registros ficam bloqueados até a regularização. Seus dados continuam disponíveis para consulta."
                    : `Regularize até ${formatDate(billing.graceUntil) ?? "o fim da carência"} para manter as operações ativas.`}
                </p>
              </div>
            </div>
            <Button onClick={handleRealizarPagamento} type="button" disabled={!formComplete}>
              Regularizar agora
            </Button>
          </div>
        </section>
      ) : null}

      <section className="grid gap-4 lg:grid-cols-[1.2fr_0.8fr]">
        <div className="rounded-md border bg-card p-5">
          <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
            <div>
              <p className="text-xs font-semibold uppercase text-muted-foreground">Plano atual</p>
              <h2 className="mt-1 text-2xl font-semibold">{plan.label}</h2>
              <p className="mt-1 text-sm text-muted-foreground">{plan.monthlyPrice}</p>
            </div>
            <div className="grid grid-cols-2 gap-2 text-sm sm:grid-cols-4">
              <LimitBox label="Animais" value={plan.animals} />
              <LimitBox label="Usuários" value={plan.users} />
              <LimitBox label="Fazendas" value={plan.farms} />
              <LimitBox label="WhatsApp" value={plan.whatsapp} />
            </div>
          </div>

          <div className="mt-5 grid gap-3 md:grid-cols-3">
            <UsageMeter label="Animais" value={session?.animalCount ?? 0} max={plan.animals === "Ilimitado" ? null : Number(plan.animals.replace(".", ""))} />
            <UsageMeter label="Usuários" value={session?.userCount ?? 1} max={plan.users === "Ilimitado" ? null : Number(plan.users)} />
            <UsageMeter label="Fazendas" value={session?.farmCount ?? 1} max={plan.farms === "Ilimitado" ? null : Number(plan.farms)} />
          </div>
        </div>

        <div className="rounded-md border bg-card p-5">
          <p className="text-xs font-semibold uppercase text-muted-foreground">Alterar plano</p>

          <div className="mt-3 flex rounded-md border p-1 text-sm">
            <button
              className={`flex-1 rounded py-1.5 text-center transition-colors ${billingCycle === "monthly" ? "bg-primary text-primary-foreground font-medium" : "hover:bg-muted/50 text-muted-foreground"}`}
              onClick={() => { setBillingCycle("monthly"); resetCheckout(); }}
              type="button"
            >
              Mensal
            </button>
            <button
              className={`flex-1 rounded py-1.5 text-center transition-colors ${billingCycle === "annual" ? "bg-primary text-primary-foreground font-medium" : "hover:bg-muted/50 text-muted-foreground"}`}
              onClick={() => { setBillingCycle("annual"); resetCheckout(); }}
              type="button"
            >
              Anual
              <span className={`ml-1 text-xs ${billingCycle === "annual" ? "text-emerald-300" : "text-emerald-600"}`}>
                2 meses grátis
              </span>
            </button>
          </div>

          <div className="mt-3 grid gap-2">
            {["starter", "pro", "business"].map((planKey) => {
              const option = PLAN_DETAILS[planKey];
              const selected = selectedPlan === planKey;
              return (
                <button
                  className={`flex items-center justify-between rounded-md border px-3 py-2 text-left transition-colors ${
                    selected ? "border-primary bg-primary/5" : "hover:bg-muted/50"
                  }`}
                  key={planKey}
                  onClick={() => {
                    setSelectedPlan(planKey);
                    resetCheckout();
                  }}
                  type="button"
                >
                  <span>
                    <span className="block text-sm font-semibold">{option.label}</span>
                    <span className="text-xs text-muted-foreground">{option.animals} animais · {option.users} usuários</span>
                  </span>
                  <span className="text-right">
                    <span className="block text-sm font-medium">{getPriceLabel(option, billingCycle)}</span>
                    {billingCycle === "annual" ? (
                      <span className="text-xs text-muted-foreground line-through">{option.monthlyPrice}</span>
                    ) : null}
                  </span>
                </button>
              );
            })}
          </div>
        </div>
      </section>

      <section id="checkout" ref={checkoutRef} className="grid gap-4 xl:grid-cols-[1fr_0.9fr]">
        <form className="rounded-md border bg-card p-5" onSubmit={handleSaveBillingData}>
          <div className="flex flex-col gap-2 border-b pb-4 md:flex-row md:items-center md:justify-between">
            <div>
              <h2 className="text-lg font-semibold">Dados de cobrança</h2>
              <p className="text-sm text-muted-foreground">Dados usados para cobrança e recibos.</p>
            </div>
            {saveState === "saved" && (
              <Badge variant="outline" className="border-emerald-300 bg-emerald-50 text-emerald-700">
                <CheckCircle2 />
                Salvo
              </Badge>
            )}
            {saveState === "error" && (
              <Badge variant="outline" className="border-destructive/40 bg-destructive/5 text-destructive">
                <AlertCircle />
                Erro ao salvar
              </Badge>
            )}
          </div>

          <div className="mt-5 grid gap-4 md:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="customerType">Tipo de cliente</Label>
              <div className="grid grid-cols-2 gap-2">
                <Button
                  aria-pressed={form.customerType === "company"}
                  onClick={() => updateForm("customerType", "company")}
                  type="button"
                  variant={form.customerType === "company" ? "default" : "outline"}
                >
                  <Building2 />
                  Jurídica
                </Button>
                <Button
                  aria-pressed={form.customerType === "individual"}
                  onClick={() => updateForm("customerType", "individual")}
                  type="button"
                  variant={form.customerType === "individual" ? "default" : "outline"}
                >
                  <ShieldCheck />
                  Física
                </Button>
              </div>
            </div>
            <div className="space-y-1">
              <Field
                id="document"
                label={form.customerType === "company" ? "CNPJ" : "CPF"}
                value={form.document}
                onChange={(value) => updateForm("document", value)}
                placeholder={form.customerType === "company" ? "00.000.000/0000-00" : "000.000.000-00"}
              />
              {documentTyped && !documentValid && (
                <p className="text-xs text-destructive">
                  {form.customerType === "company" ? "CNPJ inválido" : "CPF inválido"}
                </p>
              )}
            </div>
            <Field id="name" label="Nome ou razão social" value={form.name} onChange={(value) => updateForm("name", value)} />
            <Field id="email" label="E-mail financeiro" value={form.email} onChange={(value) => updateForm("email", value)} type="email" />
            <Field id="phone" label="WhatsApp financeiro" value={form.phone} onChange={(value) => updateForm("phone", formatPhone(value))} placeholder="(11) 99999-0000" />
            <Field id="postalCode" label="CEP" value={form.postalCode} onChange={(value) => updateForm("postalCode", value)} />
            <Field id="address" label="Endereço" value={form.address} onChange={(value) => updateForm("address", value)} />
            <Field id="number" label="Número" value={form.number} onChange={(value) => updateForm("number", value)} />
            <Field id="city" label="Cidade" value={form.city} onChange={(value) => updateForm("city", value)} />
            <Field id="state" label="UF" value={form.state} onChange={(value) => updateForm("state", value.toUpperCase().slice(0, 2))} />
          </div>

          <div className="mt-5 flex flex-col gap-2 sm:flex-row sm:justify-end">
            <Button
              disabled={savedForm == null}
              onClick={() => {
                setForm(savedForm ?? form);
                setSaveState("idle");
                resetCheckout();
              }}
              type="button"
              variant="outline"
            >
              <RotateCcw />
              Restaurar
            </Button>
            <Button disabled={!formComplete || saveState === "saving"} type="submit">
              {saveState === "saving" ? "Salvando…" : "Salvar dados"}
            </Button>
          </div>
        </form>

        <div className="space-y-4">
          <section className="rounded-md border bg-card p-5">
            <div className="flex items-center gap-2">
              <CreditCard className="h-5 w-5 text-primary" />
              <h2 className="text-lg font-semibold">Cartão de crédito</h2>
            </div>
            <p className="mt-1 text-sm text-muted-foreground">Recorrência automática. Cancele quando quiser.</p>

            <div className="mt-4 grid gap-3">
              <Field
                id="cardNumber"
                label="Número do cartão"
                value={card.number}
                onChange={(v) => updateCard("number", v)}
                placeholder="0000 0000 0000 0000"
              />
              <Field
                id="cardHolderName"
                label="Nome no cartão"
                value={card.holderName}
                onChange={(v) => updateCard("holderName", v.toUpperCase())}
                placeholder="NOME COMO NO CARTÃO"
              />
              <div className="grid grid-cols-2 gap-3">
                <div className="grid grid-cols-2 gap-2">
                  <Field
                    id="cardExpiryMonth"
                    label="Mês"
                    value={card.expiryMonth}
                    onChange={(v) => updateCard("expiryMonth", v.replace(/\D/g, "").slice(0, 2))}
                    placeholder="MM"
                  />
                  <Field
                    id="cardExpiryYear"
                    label="Ano"
                    value={card.expiryYear}
                    onChange={(v) => updateCard("expiryYear", v.replace(/\D/g, "").slice(0, 4))}
                    placeholder="AAAA"
                  />
                </div>
                <Field
                  id="cardCcv"
                  label="CVV"
                  value={card.ccv}
                  onChange={(v) => updateCard("ccv", v.replace(/\D/g, "").slice(0, 4))}
                  placeholder="000"
                />
              </div>
            </div>

            <div className="mt-4 rounded-md bg-muted/50 p-3 text-sm">
              <div className="flex items-center justify-between gap-3">
                <span className="text-muted-foreground">Plano</span>
                <span className="font-medium">{selectedPlanDetails.label}</span>
              </div>
              <div className="mt-1 flex items-center justify-between gap-3">
                <span className="text-muted-foreground">Ciclo</span>
                <span className="font-medium">{billingCycle === "annual" ? "Anual" : "Mensal"}</span>
              </div>
              <div className="mt-1 flex items-center justify-between gap-3">
                <span className="text-muted-foreground">Valor</span>
                <span className="font-medium">{selectedPriceLabel}</span>
              </div>
              {billingCycle === "annual" ? (
                <p className="mt-2 text-xs text-emerald-600">2 meses grátis incluídos no plano anual.</p>
              ) : null}
            </div>

            <div className="mt-4">
              <Button
                className="w-full"
                disabled={!formComplete || checkoutState === "loading"}
                onClick={handleRealizarPagamento}
                type="button"
              >
                <CreditCard />
                {checkoutState === "loading" ? "Processando…" : "Assinar agora"}
              </Button>
            </div>

            {paymentSuccess ? (
              <p className="mt-3 rounded-md border border-emerald-200 bg-emerald-50 p-3 text-sm font-semibold text-emerald-800">
                Pagamento confirmado! Sua assinatura está ativa.
              </p>
            ) : null}
            {checkoutState === "waiting" ? (
              <div className="mt-3 space-y-3 rounded-md border border-blue-200 bg-blue-50 p-3 text-sm text-blue-800">
                <p className="font-semibold text-blue-900">Pagamento processado!</p>
                <p className="text-xs text-blue-700">
                  Cobrança enviada ao banco. A ativação é confirmada automaticamente assim que aprovada.
                </p>
                <Button onClick={handleCheckPaymentStatus} type="button" variant="outline" size="sm" disabled={isCheckingStatus}>
                  {isCheckingStatus ? "Verificando…" : "Verificar pagamento"}
                </Button>
              </div>
            ) : null}
            {checkoutError != null ? (
              <p className="mt-3 rounded-md border border-destructive/40 bg-destructive/5 p-3 text-sm text-destructive">
                {checkoutError}
              </p>
            ) : null}
            {!formComplete ? (
              <p className="mt-3 text-xs text-muted-foreground">Preencha os dados de cobrança e do cartão para liberar o checkout.</p>
            ) : null}
          </section>

        </div>
      </section>
    </div>
  );
}

function describeBillingCheckoutError(error: unknown): string {
  if (typeof error === "object" && error != null && "code" in error) {
    switch ((error as { code: string }).code) {
      case "billing_not_configured":
        return "O serviço de pagamento está sendo configurado. Tente mais tarde ou contate o suporte.";
      case "billing_gateway_client_error":
        return "Não foi possível processar os dados de pagamento. Verifique os dados e tente novamente.";
      case "billing_gateway_error":
        return "Não foi possível conectar ao serviço de pagamento. Tente novamente em alguns minutos.";
    }
  }
  return "Não foi possível realizar o pagamento. Tente novamente.";
}

function normalizeBilling(billing: BillingContext | undefined): BillingContext {
  return {
    plan: billing?.plan ?? "free",
    status: billing?.status ?? "free",
    currentPeriodEnd: billing?.currentPeriodEnd ?? null,
    graceUntil: billing?.graceUntil ?? null,
    billingExempt: billing?.billingExempt ?? false,
    exemptionReason: billing?.exemptionReason ?? null,
    canWrite: billing?.canWrite ?? true,
    canUseWhatsapp: billing?.canUseWhatsapp ?? true,
  };
}

function billingStatusPresentation(billing: BillingContext) {
  if (billing.billingExempt) {
    return {
      label: "Acesso interno",
      className: "border-blue-300 bg-blue-50 text-blue-700",
      icon: <ShieldCheck />,
    };
  }

  switch (billing.status) {
    case "trialing":
      return { label: "Teste grátis", className: "border-amber-300 bg-amber-50 text-amber-700", icon: <ShieldCheck /> };
    case "active":
      return { label: "Ativo", className: "border-emerald-300 bg-emerald-50 text-emerald-700", icon: <CheckCircle2 /> };
    case "past_due":
      return { label: "Pagamento pendente", className: "border-amber-300 bg-amber-50 text-amber-700", icon: <AlertCircle /> };
    case "suspended":
    case "payment_required":
    case "canceled":
      return { label: "Suspenso", className: "border-destructive/40 bg-destructive/10 text-destructive", icon: <AlertCircle /> };
    default:
      return { label: "Free", className: "border-muted-foreground/30 text-muted-foreground", icon: <ShieldCheck /> };
  }
}

function LimitBox({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md border bg-background px-3 py-2">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="mt-1 text-sm font-semibold">{value}</p>
    </div>
  );
}

function UsageMeter({ label, value, max }: { label: string; value: number; max: number | null }) {
  const percent = max == null ? 35 : Math.min(100, Math.round((value / max) * 100));
  return (
    <div>
      <div className="mb-1 flex items-center justify-between text-sm">
        <span className="text-muted-foreground">{label}</span>
        <span className="font-medium">{max == null ? value : `${value}/${max}`}</span>
      </div>
      <div className="h-2 overflow-hidden rounded-full bg-muted">
        <div className="h-full rounded-full bg-primary" style={{ width: `${percent}%` }} />
      </div>
    </div>
  );
}

function Field({
  id,
  label,
  value,
  onChange,
  placeholder,
  type = "text",
}: {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  type?: string;
}) {
  return (
    <div className="space-y-2">
      <Label htmlFor={id}>{label}</Label>
      <Input id={id} value={value} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} type={type} />
    </div>
  );
}

function validateCpf(value: string): boolean {
  const d = value.replace(/\D/g, "");
  if (d.length !== 11 || /^(\d)\1{10}$/.test(d)) return false;
  let sum = 0;
  for (let i = 0; i < 9; i++) sum += +d[i] * (10 - i);
  let rem = sum % 11;
  if ((rem < 2 ? 0 : 11 - rem) !== +d[9]) return false;
  sum = 0;
  for (let i = 0; i < 10; i++) sum += +d[i] * (11 - i);
  rem = sum % 11;
  return (rem < 2 ? 0 : 11 - rem) === +d[10];
}

function validateCnpj(value: string): boolean {
  const d = value.replace(/\D/g, "");
  if (d.length !== 14 || /^(\d)\1{13}$/.test(d)) return false;
  const w1 = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
  let sum = w1.reduce((acc, w, i) => acc + +d[i] * w, 0);
  let rem = sum % 11;
  if ((rem < 2 ? 0 : 11 - rem) !== +d[12]) return false;
  const w2 = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
  sum = w2.reduce((acc, w, i) => acc + +d[i] * w, 0);
  rem = sum % 11;
  return (rem < 2 ? 0 : 11 - rem) === +d[13];
}

function isDocumentValid(form: BillingFormState): boolean {
  return form.customerType === "company"
    ? validateCnpj(form.document)
    : validateCpf(form.document);
}

function isCardFormComplete(card: CardFormState): boolean {
  return (
    card.holderName.trim().length > 0 &&
    card.number.replace(/\D/g, "").length === 16 &&
    /^(0[1-9]|1[0-2])$/.test(card.expiryMonth) &&
    /^\d{4}$/.test(card.expiryYear) &&
    /^\d{3,4}$/.test(card.ccv)
  );
}

function formatPhone(value: string): string {
  // Strip non-digits; remove leading country code 55 when 13 digits
  let d = value.replace(/\D/g, "");
  if (d.length === 13 && d.startsWith("55")) d = d.slice(2);
  d = d.slice(0, 11);
  if (d.length <= 2) return d;
  if (d.length <= 7) return `(${d.slice(0, 2)}) ${d.slice(2)}`;
  if (d.length <= 10) return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`;
  return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`;
}

function isPhoneValid(phone: string): boolean {
  const d = phone.replace(/\D/g, "");
  return d.length === 10 || d.length === 11;
}

function isBillingFormComplete(form: BillingFormState) {
  if (!isDocumentValid(form)) return false;
  if (!isPhoneValid(form.phone)) return false;
  return [
    form.name,
    form.document,
    form.email,
    form.phone,
    form.postalCode,
    form.address,
    form.number,
    form.city,
    form.state,
  ].every((value) => value.trim().length > 0);
}

function trialDaysRemaining(periodEnd: string | null | undefined) {
  if (periodEnd == null) return 0;
  const ms = new Date(periodEnd).getTime() - Date.now();
  return Math.max(0, Math.ceil(ms / (1000 * 60 * 60 * 24)));
}

function formatDate(value: string | null | undefined) {
  if (value == null || value.length === 0) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  return new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit", year: "numeric" }).format(date);
}
