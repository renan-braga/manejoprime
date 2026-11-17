import { fireEvent, screen, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { BillingPage } from "../features/billing/BillingPage";
import { jsonResponse, renderAppRoutes, renderWithAuth, setMockFetch } from "./support/adminWebTestSupport";

const ADMIN_PERMISSIONS = ["dashboard.read", "settings.read"];

const ACTIVE_BILLING = {
  plan: "starter",
  status: "active",
  currentPeriodEnd: "2026-06-10T00:00:00Z",
  graceUntil: null,
  billingExempt: false,
  exemptionReason: null,
  canWrite: true,
  canUseWhatsapp: true,
};

const DEFAULT_SESSION = {
  permissions: ADMIN_PERMISSIONS,
  farmName: "Fazenda Pagadora",
  email: "financeiro@fazenda.com",
  phone: "+5511999990001",
  billing: ACTIVE_BILLING,
};

function fillAddressForm() {
  fireEvent.change(screen.getByLabelText("CNPJ"), { target: { value: "11.222.333/0001-81" } });
  fireEvent.change(screen.getByLabelText("CEP"), { target: { value: "74000-000" } });
  fireEvent.change(screen.getByLabelText("Endereço"), { target: { value: "Rodovia Rural" } });
  fireEvent.change(screen.getByLabelText("Número"), { target: { value: "100" } });
  fireEvent.change(screen.getByLabelText("Cidade"), { target: { value: "Goiânia" } });
  fireEvent.change(screen.getByLabelText("UF"), { target: { value: "go" } });
}

function fillCardForm() {
  fireEvent.change(screen.getByLabelText("Número do cartão"), { target: { value: "4111111111111111" } });
  fireEvent.change(screen.getByLabelText("Nome no cartão"), { target: { value: "FAZENDA TESTE LTDA" } });
  fireEvent.change(screen.getByLabelText("Mês"), { target: { value: "12" } });
  fireEvent.change(screen.getByLabelText("Ano"), { target: { value: "2030" } });
  fireEvent.change(screen.getByLabelText("CVV"), { target: { value: "123" } });
}

describe("billing page", () => {
  it("renders the plan, limits, billing form, and card fields", () => {
    renderWithAuth(<BillingPage />, {
      session: {
        permissions: ADMIN_PERMISSIONS,
        billing: {
          plan: "pro",
          status: "active",
          currentPeriodEnd: "2026-06-10T00:00:00Z",
          graceUntil: null,
          billingExempt: false,
          exemptionReason: null,
          canWrite: true,
          canUseWhatsapp: true
        }
      }
    });

    expect(screen.getByRole("heading", { name: "Plano e cobrança" })).toBeInTheDocument();
    expect(screen.getAllByText("Pro").length).toBeGreaterThan(0);
    expect(screen.getAllByText("R$ 169,90/mês").length).toBeGreaterThan(0);
    expect(screen.getByLabelText("CNPJ")).toBeInTheDocument();
    expect(screen.getByLabelText("Número do cartão")).toBeInTheDocument();
    expect(screen.getByLabelText("Nome no cartão")).toBeInTheDocument();
    expect(screen.getByLabelText("CVV")).toBeInTheDocument();
  });

  it("shows a suspended account banner while keeping checkout actions reachable", () => {
    renderWithAuth(<BillingPage />, {
      session: {
        permissions: ADMIN_PERMISSIONS,
        billing: {
          plan: "pro",
          status: "suspended",
          currentPeriodEnd: null,
          graceUntil: null,
          billingExempt: false,
          exemptionReason: null,
          canWrite: false,
          canUseWhatsapp: false
        }
      }
    });

    expect(screen.getByText("Plano suspenso")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Regularizar agora" })).toBeDisabled();
    expect(screen.getByText("Novos registros ficam bloqueados até a regularização. Seus dados continuam disponíveis para consulta.")).toBeInTheDocument();
  });

  it("shows 'Pagamento processado' after card checkout", async () => {
    setMockFetch(
      new Response(null, { status: 204 }), // GET /api/v1/billing/customer on mount
      jsonResponse({ paymentMethod: "card", paymentUrl: null, bankSlipUrl: null, boletoCode: null })
    );

    renderWithAuth(<BillingPage />, { session: DEFAULT_SESSION });
    fillAddressForm();
    fillCardForm();

    fireEvent.click(screen.getByRole("button", { name: "Assinar agora" }));
    await waitFor(() => {
      expect(screen.getByText("Pagamento processado!")).toBeInTheDocument();
    });
    expect(screen.getByRole("button", { name: "Verificar pagamento" })).toBeInTheDocument();
  });

  it("shows checkout error when API returns failure", async () => {
    setMockFetch(
      new Response(null, { status: 204 }), // GET /api/v1/billing/customer on mount
      new Response(JSON.stringify({ code: "billing_gateway_client_error" }), { status: 400 })
    );

    renderWithAuth(<BillingPage />, { session: DEFAULT_SESSION });
    fillAddressForm();
    fillCardForm();

    fireEvent.click(screen.getByRole("button", { name: "Assinar agora" }));
    await waitFor(() => {
      expect(screen.getByText("Não foi possível processar os dados de pagamento. Verifique os dados e tente novamente.")).toBeInTheDocument();
    });
  });

  it("switches to annual prices and shows savings label when annual toggle is clicked", () => {
    renderWithAuth(<BillingPage />, {
      session: {
        permissions: ADMIN_PERMISSIONS,
        billing: {
          plan: "starter",
          status: "active",
          currentPeriodEnd: "2026-06-10T00:00:00Z",
          graceUntil: null,
          billingExempt: false,
          exemptionReason: null,
          canWrite: true,
          canUseWhatsapp: true,
        },
      },
    });

    expect(screen.getAllByText("R$ 89,90/mês").length).toBeGreaterThan(0);

    fireEvent.click(screen.getByRole("button", { name: /anual/i }));

    expect(screen.getAllByText("R$ 899,00/ano").length).toBeGreaterThan(0);
    expect(screen.getByText("2 meses grátis incluídos no plano anual.")).toBeInTheDocument();
  });

  it("shows trial banner with days remaining when status is trialing", () => {
    renderWithAuth(<BillingPage />, {
      session: {
        permissions: ADMIN_PERMISSIONS,
        billing: {
          plan: "trialing",
          status: "trialing",
          currentPeriodEnd: new Date(Date.now() + 15 * 24 * 60 * 60 * 1000).toISOString(),
          graceUntil: null,
          billingExempt: false,
          exemptionReason: null,
          canWrite: true,
          canUseWhatsapp: true,
        },
      },
    });

    expect(screen.getAllByText(/teste grátis/i).length).toBeGreaterThan(0);
    expect(screen.getByText(/15 dias restantes/i)).toBeInTheDocument();
  });

  it("is reachable from the admin shell navigation for settings users", () => {
    renderAppRoutes({
      initialEntries: ["/app/billing"],
      session: {
        permissions: ADMIN_PERMISSIONS,
        billing: {
          plan: "internal",
          status: "active",
          currentPeriodEnd: null,
          graceUntil: null,
          billingExempt: true,
          exemptionReason: "owner test access",
          canWrite: true,
          canUseWhatsapp: true
        }
      }
    });

    expect(screen.getByRole("link", { name: /plano/i })).toBeInTheDocument();
    expect(screen.getAllByText("Acesso interno").length).toBeGreaterThan(0);
  });
});
