import { fireEvent, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { buildSession, renderAppRoutes, setPendingFetch } from "./support/adminWebTestSupport";
import { adminWebRoleScenarios } from "./support/roleMatrix";

describe("admin weighings page", () => {
  it("lets authorized roles register weighings and inspect dense history context", async () => {
    const admin = adminWebRoleScenarios.find((scenario) => scenario.name === "admin");
    if (admin == null) {
      throw new Error("Admin role scenario is required for weighings page coverage.");
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

      if (url.includes("/api/v1/weighings?animalId=animal-1")) {
        return json({
          items: [
            {
              id: "weighing-1",
              animalId: "animal-1",
              weightKg: 511.49,
              weighingDate: "2026-03-21",
              type: "routine",
              source: "app",
              recordedBy: "admin-user",
              gmdKgPerDay: 1.0
            },
            {
              id: "weighing-2",
              animalId: "animal-1",
              weightKg: 507.49,
              weighingDate: "2026-03-17",
              type: "entry",
              source: "app",
              recordedBy: "admin-user",
              gmdKgPerDay: 1.07
            }
          ],
          total: 2,
          limit: 20,
          offset: 0
        });
      }

      if (url.endsWith("/api/v1/weighings") && method === "POST") {
        return json({
          id: "weighing-3",
          animalId: "animal-1",
          weightKg: 515.49,
          weighingDate: "2026-03-25",
          type: "routine",
          source: "app",
          recordedBy: "admin-user",
          gmdKgPerDay: 1.0
        });
      }

      throw new Error(`Unexpected request: ${method} ${url}`);
    });

    vi.stubGlobal("fetch", fetchMock);

    renderAppRoutes({ initialEntries: ["/app/animals/weighings"], session: admin.session });

    await waitFor(() => {
      expect(screen.getAllByText("511.49 kg").length).toBeGreaterThan(0);
    });

    expect(screen.getAllByText("Animal 347 · Trovao").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Delta +4.00 kg").length).toBeGreaterThan(0);
    expect(screen.getAllByText("1.00 kg/dia GMD").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Rotina").length).toBeGreaterThan(0);

    fireEvent.change(screen.getByLabelText("Peso (kg)"), {
      target: { value: "515.49" }
    });
    fireEvent.change(screen.getByLabelText("Data da pesagem"), {
      target: { value: "2026-03-25" }
    });
    fireEvent.change(screen.getByLabelText("Tipo de pesagem"), {
      target: { value: "routine" }
    });

    fireEvent.click(screen.getByRole("button", { name: "Salvar pesagem" }));

    await waitFor(() => {
      expect(screen.getByText("Pesagem salva.")).toBeInTheDocument();
    });

    expect(screen.getAllByText("515.49 kg").length).toBeGreaterThan(0);
    expect(screen.getAllByText("2026-03-25").length).toBeGreaterThan(0);

    const createCall = fetchMock.mock.calls.find((call) => {
      const url = typeof call[0] === "string" ? call[0] : call[0].toString();
      return url.endsWith("/api/v1/weighings") && call[1]?.method === "POST";
    });

    expect(createCall).toBeDefined();
    expect(createCall?.[1]?.body).toContain("\"animalId\":\"animal-1\"");
    expect(createCall?.[1]?.body).toContain("\"weightKg\":\"515.49\"");
    expect(createCall?.[1]?.body).toContain("\"weighingDate\":\"2026-03-25\"");
  });

  it("shows validation feedback when weighing submission fails", async () => {
    const admin = adminWebRoleScenarios.find((scenario) => scenario.name === "admin");
    if (admin == null) {
      throw new Error("Admin role scenario is required for weighings page coverage.");
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

      if (url.includes("/api/v1/weighings?animalId=animal-1")) {
        return json({
          items: [],
          total: 0,
          limit: 20,
          offset: 0
        });
      }

      if (url.endsWith("/api/v1/weighings") && method === "POST") {
        return json(
          {
            code: "validation_failed",
            message: "Dados inválidos.",
            fieldErrors: {
              weightKg: "Weight must be greater than zero.",
              weighingDate: "Weighing date is required."
            }
          },
          { status: 400 }
        );
      }

      throw new Error(`Unexpected request: ${method} ${url}`);
    });

    vi.stubGlobal("fetch", fetchMock);

    renderAppRoutes({ initialEntries: ["/app/animals/weighings"], session: admin.session });

    await waitFor(() => {
      expect(screen.getByText("Nenhuma pesagem registrada")).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText("Peso (kg)"), {
      target: { value: "0" }
    });

    fireEvent.click(screen.getByRole("button", { name: "Salvar pesagem" }));

    await waitFor(() => {
      expect(screen.getByText("Weight must be greater than zero.")).toBeInTheDocument();
    });

    expect(screen.getByText("Weighing date is required.")).toBeInTheDocument();
    expect(screen.getByText("Dados inválidos.")).toBeInTheDocument();
  });

  it("shows role-aware guidance when the session can reach animals but cannot manage weighings", async () => {
    const session = buildSession({
      userId: "restricted-animal-user",
      name: "Restricted Animal User",
      phone: "5511999990900",
      role: "worker",
      farmId: "farm-1",
      farmName: "Fazenda Restrita",
      permissions: ["dashboard.read", "animals.read"]
    });

    setPendingFetch();

    renderAppRoutes({ initialEntries: ["/app/animals/weighings"], session });

    await waitFor(() => {
      expect(
        screen.getByRole("heading", {
          name: "Pesagens",
          level: 1
        })
      ).toBeInTheDocument();
    });

    expect(
      screen.getByText(
        "Apenas usuários com permissão de pesagem podem registrar e consultar o histórico de pesagens."
      )
    ).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Salvar pesagem" })).not.toBeInTheDocument();
  });
});

function json(body: unknown, init: ResponseInit = { status: 200 }) {
  return Promise.resolve(new Response(JSON.stringify(body), init));
}
