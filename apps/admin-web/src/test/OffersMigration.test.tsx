import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { renderAppRoutes, setPendingFetch } from "./support/adminWebTestSupport";
import { adminWebRoleScenarios } from "./support/roleMatrix";

const ANIMALS_RESPONSE = {
  items: [
    { id: "animal-1", identifier: "C-001", name: "Trovao", sex: "male", breed: "Nelore", status: "active", lotId: "lot-1", lotName: "Engorda" }
  ],
  total: 1,
  limit: 100,
  offset: 0,
};

const DRAFT_LIST_ITEM = {
  id: "offer-1",
  farmId: "farm-1",
  title: "Lote Engorda",
  askingPrice: 75000,
  buyerContact: null,
  expiresAt: null,
  status: "draft",
  animalCount: 0,
  createdAt: "2026-05-07T10:00:00Z",
  updatedAt: "2026-05-07T10:00:00Z",
};

const ACTIVE_LIST_ITEM = { ...DRAFT_LIST_ITEM, status: "active" };
const CANCELLED_LIST_ITEM = { ...DRAFT_LIST_ITEM, status: "cancelled" };
const SOLD_LIST_ITEM = { ...DRAFT_LIST_ITEM, status: "sold" };

const CREATED_OFFER_DETAIL = {
  id: "offer-1",
  farmId: "farm-1",
  title: "Lote Engorda",
  notes: null,
  askingPrice: 75000,
  buyerContact: null,
  expiresAt: null,
  status: "draft",
  animalIds: [],
  createdAt: "2026-05-07T10:00:00Z",
  updatedAt: "2026-05-07T10:00:00Z",
};

function setupMock(initialOffers: typeof DRAFT_LIST_ITEM[] = []) {
  let offers = [...initialOffers];

  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = typeof input === "string" ? input : input.toString();
    const method = init?.method ?? "GET";

    if (url.includes("/api/v1/animals?")) return json(ANIMALS_RESPONSE);

    if (url.endsWith("/api/v1/offers") && method === "GET") {
      return json({ items: offers });
    }

    if (url.endsWith("/api/v1/offers") && method === "POST") {
      offers = [DRAFT_LIST_ITEM];
      return json(CREATED_OFFER_DETAIL);
    }

    if (url.includes("/api/v1/offers/offer-1/activate") && method === "POST") {
      offers = [ACTIVE_LIST_ITEM];
      return json({ ...CREATED_OFFER_DETAIL, status: "active" });
    }

    if (url.includes("/api/v1/offers/offer-1/cancel") && method === "POST") {
      offers = [CANCELLED_LIST_ITEM];
      return json({ ...CREATED_OFFER_DETAIL, status: "cancelled" });
    }

    if (url.includes("/api/v1/offers/offer-1/mark-sold") && method === "POST") {
      offers = [SOLD_LIST_ITEM];
      return json({ ...CREATED_OFFER_DETAIL, status: "sold" });
    }

    throw new Error(`Unexpected request: ${method} ${url}`);
  });

  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("offers page migration", () => {
  it("renders /offers with heading and new offer button for admin", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setupMock();
    renderAppRoutes({ initialEntries: ["/app/offers"], session: admin.session });

    await waitFor(() => expect(screen.getByRole("heading", { level: 1, name: "Ofertas" })).toBeInTheDocument());
    expect(screen.getByRole("button", { name: "Nova oferta" })).toBeInTheDocument();
  });

  it("loads existing offers from the API on mount", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setupMock([DRAFT_LIST_ITEM]);
    renderAppRoutes({ initialEntries: ["/app/offers"], session: admin.session });

    await waitFor(() => expect(screen.getByText("Lote Engorda")).toBeInTheDocument());
    const table = screen.getByRole("table");
    expect(within(table).getByText("Rascunho")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Ativar" })).toBeInTheDocument();
  });

  it("admin can create an offer via the wizard and sees it in the list", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    const fetchMock = setupMock();
    renderAppRoutes({ initialEntries: ["/app/offers"], session: admin.session });

    await waitFor(() => expect(screen.getByRole("button", { name: "Nova oferta" })).toBeInTheDocument());

    fireEvent.click(screen.getByRole("button", { name: "Nova oferta" }));

    await waitFor(() => expect(screen.getByRole("button", { name: "Próximo" })).toBeInTheDocument());

    fireEvent.click(screen.getByRole("button", { name: "Próximo" }));

    await waitFor(() => expect(screen.getByLabelText("Título")).toBeInTheDocument());

    fireEvent.change(screen.getByLabelText("Título"), { target: { value: "Lote Engorda" } });
    fireEvent.change(screen.getByLabelText("Preço pedido"), { target: { value: "75000" } });

    fireEvent.click(screen.getByRole("button", { name: "Salvar oferta" }));

    await waitFor(() => expect(screen.getByText("Lote Engorda")).toBeInTheDocument());
    const table = screen.getByRole("table");
    expect(within(table).getByText("Rascunho")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Salvar oferta" })).not.toBeInTheDocument();

    const createCall = fetchMock.mock.calls.find((call) => {
      const url = typeof call[0] === "string" ? call[0] : call[0].toString();
      return url.endsWith("/api/v1/offers") && call[1]?.method === "POST";
    });
    expect(createCall).toBeDefined();
    expect(createCall?.[1]?.body).toContain('"title":"Lote Engorda"');
  });

  it("wizard step 1 shows animal checkboxes", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setupMock();
    renderAppRoutes({ initialEntries: ["/app/offers"], session: admin.session });

    await waitFor(() => expect(screen.getByRole("button", { name: "Nova oferta" })).toBeInTheDocument());

    fireEvent.click(screen.getByRole("button", { name: "Nova oferta" }));

    await waitFor(() => expect(screen.getByText("C-001 — Trovao")).toBeInTheDocument());
    expect(screen.getByRole("checkbox")).toBeInTheDocument();
  });

  it("admin can activate a draft offer", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setupMock([DRAFT_LIST_ITEM]);
    renderAppRoutes({ initialEntries: ["/app/offers"], session: admin.session });

    await waitFor(() => expect(screen.getByRole("button", { name: "Ativar" })).toBeInTheDocument());

    fireEvent.click(screen.getByRole("button", { name: "Ativar" }));

    await waitFor(() => {
      const table = screen.getByRole("table");
      expect(within(table).getByText("Ativa")).toBeInTheDocument();
    });
    expect(screen.getByRole("button", { name: "Marcar vendida" })).toBeInTheDocument();
  });

  it("admin can cancel a draft offer", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setupMock([DRAFT_LIST_ITEM]);
    renderAppRoutes({ initialEntries: ["/app/offers"], session: admin.session });

    await waitFor(() => expect(screen.getByRole("button", { name: "Cancelar" })).toBeInTheDocument());

    fireEvent.click(screen.getByRole("button", { name: "Cancelar" }));

    await waitFor(() =>
      expect(screen.queryByRole("button", { name: "Ativar" })).not.toBeInTheDocument()
    );
  });

  it("admin can mark an active offer as sold via the confirmation modal", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setupMock([ACTIVE_LIST_ITEM]);
    renderAppRoutes({ initialEntries: ["/app/offers"], session: admin.session });

    await waitFor(() => expect(screen.getByRole("button", { name: "Marcar vendida" })).toBeInTheDocument());

    fireEvent.click(screen.getByRole("button", { name: "Marcar vendida" }));

    await waitFor(() => expect(screen.getByText("Marcar como vendida?")).toBeInTheDocument());

    fireEvent.click(screen.getByRole("button", { name: "Confirmar" }));

    await waitFor(() =>
      expect(screen.queryByRole("button", { name: "Marcar vendida" })).not.toBeInTheDocument()
    );
  });

  it("shows a skeleton while offers are loading", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setPendingFetch();
    renderAppRoutes({ initialEntries: ["/app/offers"], session: admin.session });

    await waitFor(() => expect(screen.getByTestId("offers-skeleton")).toBeInTheDocument());
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });

  it("redirects veterinarian from /offers to dashboard", async () => {
    const veterinarian = adminWebRoleScenarios.find((s) => s.name === "veterinarian")!;
    setupMock();
    renderAppRoutes({ initialEntries: ["/app/offers"], session: veterinarian.session });

    await waitFor(() => expect(screen.getByRole("heading", { name: "Dashboard" })).toBeInTheDocument());
    expect(screen.queryByRole("heading", { name: "Ofertas" })).not.toBeInTheDocument();
  });

  it("redirects worker from /offers to dashboard", async () => {
    const worker = adminWebRoleScenarios.find((s) => s.name === "worker")!;
    setupMock();
    renderAppRoutes({ initialEntries: ["/app/offers"], session: worker.session });

    await waitFor(() => expect(screen.getByRole("heading", { name: "Dashboard" })).toBeInTheDocument());
    expect(screen.queryByRole("heading", { name: "Ofertas" })).not.toBeInTheDocument();
  });
});

function json(body: unknown, init: ResponseInit = { status: 200 }) {
  return Promise.resolve(new Response(JSON.stringify(body), init));
}
