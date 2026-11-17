import { fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { renderAppRoutes, setPendingFetch } from "./support/adminWebTestSupport";
import { adminWebRoleScenarios } from "./support/roleMatrix";

const ANIMALS_RESPONSE = {
  items: [
    { id: "animal-1", identifier: "347", name: "Trovao", sex: "male", breed: "Nelore", status: "active", lotId: "lot-1", lotName: "Engorda 1" }
  ],
  total: 1, limit: 100, offset: 0
};

const WEIGHINGS_RESPONSE = {
  items: [
    { id: "w-1", animalId: "animal-1", weightKg: 511.49, weighingDate: "2026-03-21", type: "routine", source: "app", recordedBy: "u-1", gmdKgPerDay: 1.0 },
    { id: "w-2", animalId: "animal-1", weightKg: 507.49, weighingDate: "2026-03-17", type: "entry", source: "app", recordedBy: "u-1", gmdKgPerDay: 1.07 }
  ],
  total: 2, limit: 20, offset: 0
};

function setupMock() {
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = typeof input === "string" ? input : input.toString();
    const method = init?.method ?? "GET";
    if (url.includes("/api/v1/animals?")) return json(ANIMALS_RESPONSE);
    if (url.includes("/api/v1/weighings?animalId=animal-1")) return json(WEIGHINGS_RESPONSE);
    if (url.endsWith("/api/v1/weighings") && method === "POST") {
      return json({ id: "w-new", animalId: "animal-1", weightKg: 520.0, weighingDate: "2026-04-01", type: "routine", source: "app", recordedBy: "u-1", gmdKgPerDay: 1.5 });
    }
    throw new Error(`Unexpected: ${method} ${url}`);
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

afterEach(() => { vi.unstubAllGlobals(); });

describe("weighings page migration", () => {
  it("renders page heading 'Weighings' and a table with Date, Weight (kg), Type columns", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setupMock();
    renderAppRoutes({ initialEntries: ["/app/animals/weighings"], session: admin.session });

    await waitFor(() => {
      expect(screen.getByRole("heading", { level: 1, name: "Pesagens" })).toBeInTheDocument();
      expect(screen.getByRole("columnheader", { name: /data/i })).toBeInTheDocument();
      expect(screen.getByRole("columnheader", { name: /peso/i })).toBeInTheDocument();
      expect(screen.getByRole("columnheader", { name: /tipo/i })).toBeInTheDocument();
    });

    await waitFor(() => expect(screen.getByText("511.49 kg")).toBeInTheDocument());
    expect(screen.getByText("2026-03-21")).toBeInTheDocument();
    expect(screen.getAllByText("Rotina").length).toBeGreaterThan(0);
  });

  it("shows skeleton while weighings are loading", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setPendingFetch();
    renderAppRoutes({ initialEntries: ["/app/animals/weighings"], session: admin.session });

    await waitFor(() => expect(screen.getByTestId("weighings-skeleton")).toBeInTheDocument());
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });

  it("saves new weighing via POST and prepends it to the table", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    const fetchMock = setupMock();
    renderAppRoutes({ initialEntries: ["/app/animals/weighings"], session: admin.session });

    await waitFor(() => expect(screen.getByText("511.49 kg")).toBeInTheDocument());

    fireEvent.change(screen.getByLabelText("Peso (kg)"), { target: { value: "520" } });
    fireEvent.change(screen.getByLabelText("Data da pesagem"), { target: { value: "2026-04-01" } });
    fireEvent.click(screen.getByRole("button", { name: /salvar pesagem/i }));

    await waitFor(() => expect(screen.getByText("520.00 kg")).toBeInTheDocument());

    const postCall = fetchMock.mock.calls.find((c) => {
      const u = typeof c[0] === "string" ? c[0] : c[0].toString();
      return u.endsWith("/api/v1/weighings") && c[1]?.method === "POST";
    });
    expect(postCall).toBeDefined();
    expect(postCall?.[1]?.body).toContain('"animalId":"animal-1"');
    expect(postCall?.[1]?.body).toContain('"weightKg":"520"');
  });
});

function json(body: unknown, init: ResponseInit = { status: 200 }) {
  return Promise.resolve(new Response(JSON.stringify(body), init));
}
