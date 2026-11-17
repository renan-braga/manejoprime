import { screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { renderAppRoutes } from "./support/adminWebTestSupport";
import { adminWebRoleScenarios } from "./support/roleMatrix";

describe("admin reports page", () => {
  it("gives admin sessions a broader farm and health reporting foundation", async () => {
    const admin = adminWebRoleScenarios.find((scenario) => scenario.name === "admin");
    if (admin == null) {
      throw new Error("Admin role scenario is required for reports coverage.");
    }

    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = typeof input === "string" ? input : input.toString();

      if (url.endsWith("/api/v1/dashboard")) {
        return json({
          totalAnimals: 48,
          activeAnimals: 43,
          lotsCount: 6,
          activeUsers: 5,
          statusBreakdown: {
            active: 43,
            sold: 3,
            dead: 1,
            transferred: 1
          },
          alerts: [
            {
              type: "occurrence",
              title: "Open health occurrences",
              count: 4
            }
          ],
          shortcuts: ["animals", "users", "settings"]
        });
      }

      if (url.includes("/api/v1/occurrences?limit=20&offset=0")) {
        return json({
          items: [
            {
              id: "occurrence-1",
              animalId: "animal-1",
              animalIdentifier: "347",
              animalName: "Trovao",
              type: "health",
              severity: "critical",
              status: "open",
              description: "High fever after transport.",
              photoUrl: null,
              source: "app",
              recordedBy: "admin-user",
              resolvedBy: null,
              createdAt: "2026-05-01T08:00:00Z",
              updatedAt: "2026-05-01T09:00:00Z"
            },
            {
              id: "occurrence-2",
              animalId: "animal-2",
              animalIdentifier: "348",
              animalName: "Aurora",
              type: "injury",
              severity: "medium",
              status: "in_follow_up",
              description: "Hoof treatment still under observation.",
              photoUrl: null,
              source: "whatsapp",
              recordedBy: "worker-user",
              resolvedBy: null,
              createdAt: "2026-04-30T10:00:00Z",
              updatedAt: "2026-05-01T07:30:00Z"
            }
          ],
          total: 2,
          limit: 20,
          offset: 0
        });
      }

      throw new Error(`Unexpected request: GET ${url}`);
    });

    vi.stubGlobal("fetch", fetchMock);

    renderAppRoutes({ initialEntries: ["/app/reports"], session: admin.session });

    await waitFor(() => {
      expect(screen.getByText("Resumo da fazenda")).toBeInTheDocument();
    });

    expect(screen.getByText("Usuários com acesso")).toBeInTheDocument();
    expect(screen.getByText("Acompanhamento crítico")).toBeInTheDocument();
    expect(screen.getByText("High fever after transport.")).toBeInTheDocument();
    expect(screen.getByText("Exportação de saúde")).toBeInTheDocument();
    expect(screen.getByText("Exportação de operações")).toBeInTheDocument();
  });

  it("keeps veterinarian reports focused on health follow-up and export placeholders", async () => {
    const veterinarian = adminWebRoleScenarios.find((scenario) => scenario.name === "veterinarian");
    if (veterinarian == null) {
      throw new Error("Veterinarian role scenario is required for reports coverage.");
    }

    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = typeof input === "string" ? input : input.toString();

      if (url.endsWith("/api/v1/dashboard")) {
        return json({
          totalAnimals: 30,
          activeAnimals: 27,
          lotsCount: 4,
          activeUsers: 3,
          statusBreakdown: {
            active: 27,
            sold: 2,
            dead: 1,
            transferred: 0
          },
          alerts: [],
          shortcuts: ["occurrence"]
        });
      }

      if (url.includes("/api/v1/occurrences?limit=20&offset=0")) {
        return json({
          items: [],
          total: 0,
          limit: 20,
          offset: 0
        });
      }

      throw new Error(`Unexpected request: GET ${url}`);
    });

    vi.stubGlobal("fetch", fetchMock);

    renderAppRoutes({ initialEntries: ["/app/reports"], session: veterinarian.session });

    await waitFor(() => {
      expect(screen.getByText("Acompanhamento de saúde")).toBeInTheDocument();
    });

    expect(screen.getByText("Nenhum item de acompanhamento de saúde encontrado.")).toBeInTheDocument();
    expect(screen.queryByText("Resumo da fazenda")).not.toBeInTheDocument();
    expect(screen.queryByText("Usuários com acesso")).not.toBeInTheDocument();
    expect(screen.getByText("Exportação de saúde")).toBeInTheDocument();
    expect(screen.queryByText("Exportação de operações")).not.toBeInTheDocument();
  });
});

function json(body: unknown, init: ResponseInit = { status: 200 }) {
  return Promise.resolve(new Response(JSON.stringify(body), init));
}
