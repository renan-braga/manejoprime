import { screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { renderAppRoutes, setPendingFetch } from "./support/adminWebTestSupport";
import { adminWebRoleScenarios } from "./support/roleMatrix";

const USERS_RESPONSE = {
  items: [
    { id: "user-1", name: "João Silva", phone: "+5511999990001", role: "admin", active: true },
    { id: "user-2", name: "Maria Campos", phone: "+5511999990002", role: "worker", active: false }
  ],
  total: 2,
  limit: 20,
  offset: 0
};

function setupSettingsMock() {
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = typeof input === "string" ? input : input.toString();
    const method = init?.method ?? "GET";
    if (url.endsWith("/api/v1/settings") && method === "GET") {
      return json({ farmId: "farm-1", farmName: "Fazenda Boa Vista", carcassYieldPercent: "52.00" });
    }
    throw new Error(`Unexpected: ${method} ${url}`);
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

function setupUsersMock() {
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = typeof input === "string" ? input : input.toString();
    const method = init?.method ?? "GET";
    if (url.includes("/api/v1/users?") && method === "GET") return json(USERS_RESPONSE);
    throw new Error(`Unexpected: ${method} ${url}`);
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

function json(body: unknown, init: ResponseInit = { status: 200 }) {
  return Promise.resolve(new Response(JSON.stringify(body), init));
}

describe("settings page migration", () => {
  it("renders page heading 'Settings' as h1", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setupSettingsMock();
    renderAppRoutes({ initialEntries: ["/app/settings"], session: admin.session });

    await waitFor(() => expect(screen.getByRole("heading", { level: 1, name: "Configurações" })).toBeInTheDocument());
  });

  it("shows form with Farm name and Carcass yield labels and prefilled input values", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setupSettingsMock();
    renderAppRoutes({ initialEntries: ["/app/settings"], session: admin.session });

    await waitFor(() => expect(screen.getByDisplayValue("Fazenda Boa Vista")).toBeInTheDocument());
    expect(screen.getByLabelText("Nome da fazenda")).toBeInTheDocument();
    expect(screen.getByLabelText("Rendimento de carcaça (%)")).toBeInTheDocument();
    expect(screen.getByDisplayValue("52.00")).toBeInTheDocument();
  });

  it("shows skeleton while settings are loading", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setPendingFetch();
    renderAppRoutes({ initialEntries: ["/app/settings"], session: admin.session });

    await waitFor(() => expect(screen.getByTestId("settings-skeleton")).toBeInTheDocument());
    expect(screen.queryByLabelText("Nome da fazenda")).not.toBeInTheDocument();
  });
});

describe("users page migration", () => {
  it("renders page heading 'Users' as h1", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setupUsersMock();
    renderAppRoutes({ initialEntries: ["/app/users"], session: admin.session });

    await waitFor(() => expect(screen.getByRole("heading", { level: 1, name: "Usuários" })).toBeInTheDocument());
  });

  it("shows table with Name, Role, Status column headers and row data", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setupUsersMock();
    renderAppRoutes({ initialEntries: ["/app/users"], session: admin.session });

    await waitFor(() => expect(screen.getByRole("columnheader", { name: "Nome" })).toBeInTheDocument());
    expect(screen.getByRole("columnheader", { name: "Cargo" })).toBeInTheDocument();
    expect(screen.getByRole("columnheader", { name: "Status" })).toBeInTheDocument();

    await waitFor(() => expect(screen.getByText("João Silva")).toBeInTheDocument());
    expect(screen.getByText("Maria Campos")).toBeInTheDocument();
    expect(screen.getAllByText("Inativo").length).toBeGreaterThan(0);
  });

  it("shows skeleton while users are loading", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setPendingFetch();
    renderAppRoutes({ initialEntries: ["/app/users"], session: admin.session });

    await waitFor(() => expect(screen.getByTestId("users-skeleton")).toBeInTheDocument());
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });
});
