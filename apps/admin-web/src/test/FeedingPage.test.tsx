import { fireEvent, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { renderAppRoutes, setPendingFetch } from "./support/adminWebTestSupport";
import { adminWebRoleScenarios } from "./support/roleMatrix";

describe("admin feeding page", () => {
  it("lets admin sessions register feedings and inspect denser animal and lot history", async () => {
    const admin = adminWebRoleScenarios.find((scenario) => scenario.name === "admin");
    if (admin == null) {
      throw new Error("Admin role scenario is required for feeding page coverage.");
    }

    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = typeof input === "string" ? input : input.toString();
      const method = init?.method ?? "GET";

      if (url.includes("/api/v1/animals?")) {
        return json({
          items: [
            {
              id: "animal-1",
              identifier: "347",
              name: "Trovao",
              sex: "male",
              breed: "Nelore",
              status: "active",
              lotId: "lot-1",
              lotName: "Engorda 1"
            }
          ],
          total: 1,
          limit: 100,
          offset: 0
        });
      }

      if (url.includes("/api/v1/lots?")) {
        return json({
          items: [
            {
              id: "lot-1",
              name: "Engorda 1",
              purpose: "fattening",
              pastureArea: "Pasto Norte",
              animalsCount: 18
            }
          ],
          total: 1,
          limit: 100,
          offset: 0
        });
      }

      if (url.includes("/api/v1/feedings?animalId=animal-1")) {
        return json({
          items: [
            {
              id: "feeding-1",
              animalId: "animal-1",
              lotId: null,
              feedType: "racao",
              description: "Racao de acabamento",
              quantityKg: 42.5,
              costPerKg: 2.1,
              startDate: "2026-04-20",
              endDate: "2026-04-26",
              source: "app",
              recordedBy: "admin-user"
            }
          ],
          total: 1,
          limit: 20,
          offset: 0
        });
      }

      if (url.includes("/api/v1/feedings?lotId=lot-1")) {
        return json({
          items: [
            {
              id: "feeding-2",
              animalId: null,
              lotId: "lot-1",
              feedType: "silagem",
              description: "Silagem premium",
              quantityKg: 120,
              costPerKg: 1.35,
              startDate: "2026-04-10",
              endDate: null,
              source: "app",
              recordedBy: "admin-user"
            }
          ],
          total: 1,
          limit: 20,
          offset: 0
        });
      }

      if (url.endsWith("/api/v1/feedings") && method === "POST") {
        return json({
          id: "feeding-3",
          animalId: "animal-1",
          lotId: null,
          feedType: "suplemento",
          description: "Suplemento proteico",
          quantityKg: 8.5,
          costPerKg: 3.2,
          startDate: "2026-05-01",
          endDate: "2026-05-03",
          source: "app",
          recordedBy: "admin-user"
        });
      }

      throw new Error(`Unexpected request: ${method} ${url}`);
    });

    vi.stubGlobal("fetch", fetchMock);

    renderAppRoutes({ initialEntries: ["/app/animals/feedings"], session: admin.session });

    await waitFor(() => {
      expect(screen.getByText("Racao de acabamento")).toBeInTheDocument();
    });

    expect(screen.getAllByText("Animal 347 · Trovao").length).toBeGreaterThan(0);
    expect(screen.getAllByText("42.5 kg").length).toBeGreaterThan(0);
    expect(screen.getByText("R$ 2.10/kg")).toBeInTheDocument();
    expect(screen.getByText("2026-04-20 até 2026-04-26")).toBeInTheDocument();

    fireEvent.change(screen.getByRole("combobox", { name: /^Feed type$/ }), {
      target: { value: "suplemento" }
    });
    fireEvent.change(screen.getByLabelText("Description"), {
      target: { value: "Suplemento proteico" }
    });
    fireEvent.change(screen.getByLabelText("Quantity (kg)"), {
      target: { value: "8.50" }
    });
    fireEvent.change(screen.getByLabelText("Cost per kg"), {
      target: { value: "3.20" }
    });
    fireEvent.change(screen.getByLabelText("Data de início"), {
      target: { value: "2026-05-01" }
    });
    fireEvent.change(screen.getByLabelText("Data de fim"), {
      target: { value: "2026-05-03" }
    });

    fireEvent.click(screen.getByRole("button", { name: "Salvar alimentação" }));

    await waitFor(() => {
      expect(screen.getByText("Suplemento proteico")).toBeInTheDocument();
    });

    const createCall = fetchMock.mock.calls.find((call) => {
      const url = typeof call[0] === "string" ? call[0] : call[0].toString();
      return url.endsWith("/api/v1/feedings") && call[1]?.method === "POST";
    });

    expect(createCall).toBeDefined();
    expect(createCall?.[1]?.headers).toMatchObject({ "Content-Type": "application/json" });
    expect(createCall?.[1]?.body).toContain("\"animalId\":\"animal-1\"");
    expect(createCall?.[1]?.body).toContain("\"feedType\":\"suplemento\"");

    fireEvent.click(screen.getByRole("button", { name: "Lote" }));

    await waitFor(() => {
      expect(screen.getByText("Silagem premium")).toBeInTheDocument();
    });

    expect(screen.getAllByText("Lote Engorda 1 · Engorda").length).toBeGreaterThan(0);
    expect(screen.getByText("120 kg")).toBeInTheDocument();
    expect(screen.getByText("R$ 1.35/kg")).toBeInTheDocument();
    expect(screen.getByText("Janela aberta")).toBeInTheDocument();
  });

  it("shows role-aware guidance when the session can reach animals but cannot manage feeding", async () => {
    const worker = adminWebRoleScenarios.find((scenario) => scenario.name === "worker");
    if (worker == null) {
      throw new Error("Worker role scenario is required for feeding page coverage.");
    }

    setPendingFetch();

    renderAppRoutes({ initialEntries: ["/app/animals/feedings"], session: worker.session });

    await waitFor(() => {
      expect(
        screen.getByRole("heading", {
          name: "Planejamento de alimentação disponível apenas para administradores.",
          level: 1
        })
      ).toBeInTheDocument();
    });

    expect(
      screen.getAllByText(
        "Esta rota está dentro do módulo de animais para contexto compartilhado, mas apenas sessões com acesso de administrador podem registrar e inspecionar planos de alimentação."
      ).length
    ).toBeGreaterThan(0);
    expect(screen.queryByRole("button", { name: "Salvar alimentação" })).not.toBeInTheDocument();
  });
});

function json(body: unknown, init: ResponseInit = { status: 200 }) {
  return Promise.resolve(new Response(JSON.stringify(body), init));
}
