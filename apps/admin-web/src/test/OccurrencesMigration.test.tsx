import { fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { renderAppRoutes, setPendingFetch } from "./support/adminWebTestSupport";
import { adminWebRoleScenarios } from "./support/roleMatrix";

const ANIMALS_RESPONSE = {
  items: [
    { id: "animal-1", identifier: "347", name: "Trovao", sex: "male", breed: "Nelore", status: "active", lotId: "lot-1", lotName: "Engorda 1" }
  ],
  total: 1,
  limit: 100,
  offset: 0,
};

const OCCURRENCES_RESPONSE = {
  items: [
    {
      id: "occurrence-1",
      animalId: "animal-1",
      animalIdentifier: "347",
      animalName: "Trovao",
      type: "injury",
      severity: "medium",
      status: "in_follow_up",
      description: "Hoof treatment is still under observation.",
      photoUrl: null,
      source: "whatsapp",
      recordedBy: "worker-user",
      resolvedBy: null,
      createdAt: "2026-04-28T08:00:00Z",
      updatedAt: "2026-04-29T09:15:00Z",
    },
    {
      id: "occurrence-2",
      animalId: "animal-1",
      animalIdentifier: "347",
      animalName: "Trovao",
      type: "health",
      severity: "high",
      status: "open",
      description: "Animal limping after transport.",
      photoUrl: null,
      source: "app",
      recordedBy: "admin-user",
      resolvedBy: null,
      createdAt: "2026-04-26T08:00:00Z",
      updatedAt: "2026-04-26T09:30:00Z",
    },
  ],
  total: 2,
  limit: 20,
  offset: 0,
};

function setupMock() {
  const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
    const url = typeof input === "string" ? input : input.toString();
    if (url.includes("/api/v1/animals?")) return json(ANIMALS_RESPONSE);
    if (url.includes("/api/v1/occurrences?")) return json(OCCURRENCES_RESPONSE);
    throw new Error(`Unexpected: ${url}`);
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

afterEach(() => { vi.unstubAllGlobals(); });

describe("occurrences page migration", () => {
  it("renders /health/occurrences with occurrence rows and table columns", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setupMock();
    renderAppRoutes({ initialEntries: ["/app/health/occurrences"], session: admin.session });

    await waitFor(() => expect(screen.getByRole("heading", { level: 1, name: "Ocorrências" })).toBeInTheDocument());
    expect(screen.getByRole("columnheader", { name: "Animal" })).toBeInTheDocument();
    expect(screen.getByRole("columnheader", { name: "Gravidade" })).toBeInTheDocument();
    expect(screen.getByRole("columnheader", { name: "Status" })).toBeInTheDocument();
    expect(screen.getAllByText("Animal 347 · Trovao").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Hoof treatment is still under observation.").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Entrada WhatsApp").length).toBeGreaterThan(0);
  });

  it("lets workers with occurrences.write access the standalone route without resolve actions", async () => {
    const worker = adminWebRoleScenarios.find((s) => s.name === "worker")!;
    setupMock();
    renderAppRoutes({ initialEntries: ["/app/health/occurrences"], session: worker.session });

    await waitFor(() => expect(screen.getAllByText("Hoof treatment is still under observation.").length).toBeGreaterThan(0));
    expect(screen.getByText("Resolver e reabrir são ações reservadas para veterinários e administradores.")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Resolver ocorrência" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Reabrir ocorrência" })).not.toBeInTheDocument();
  });

  it("filters occurrence rows by search, status, and severity", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setupMock();
    renderAppRoutes({ initialEntries: ["/app/health/occurrences"], session: admin.session });

    await waitFor(() => expect(screen.getAllByText("Hoof treatment is still under observation.").length).toBeGreaterThan(0));
    expect(screen.getAllByText("Animal limping after transport.").length).toBeGreaterThan(0);

    fireEvent.change(screen.getByLabelText("Buscar ocorrências"), { target: { value: "limping" } });

    expect(screen.queryByText("Hoof treatment is still under observation.")).not.toBeInTheDocument();
    expect(screen.getAllByText("Animal limping after transport.").length).toBeGreaterThan(0);

    fireEvent.change(screen.getByLabelText("Buscar ocorrências"), { target: { value: "" } });
    fireEvent.change(screen.getByLabelText("Filtro de status"), { target: { value: "in_follow_up" } });
    fireEvent.change(screen.getByLabelText("Filtro de gravidade"), { target: { value: "medium" } });

    expect(screen.getAllByText("Hoof treatment is still under observation.").length).toBeGreaterThan(0);
    expect(screen.queryByText("Animal limping after transport.")).not.toBeInTheDocument();
  });

  it("shows skeleton while occurrences are loading", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setPendingFetch();
    renderAppRoutes({ initialEntries: ["/app/health/occurrences"], session: admin.session });

    await waitFor(() => expect(screen.getByTestId("occurrences-skeleton")).toBeInTheDocument());
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });
});

function json(body: unknown, init: ResponseInit = { status: 200 }) {
  return Promise.resolve(new Response(JSON.stringify(body), init));
}
