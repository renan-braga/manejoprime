import { fireEvent, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { renderAppRoutes } from "./support/adminWebTestSupport";
import { adminWebRoleScenarios } from "./support/roleMatrix";

describe("admin settings page", () => {
  it("loads farm settings and displays current values prefilled", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;

    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = typeof input === "string" ? input : input.toString();
      const method = init?.method ?? "GET";

      if (url.endsWith("/api/v1/settings") && method === "GET") {
        return json({ farmId: "farm-1", farmName: "Fazenda Boa Vista", carcassYieldPercent: "52.00" });
      }

      throw new Error(`Unexpected request: ${method} ${url}`);
    });

    vi.stubGlobal("fetch", fetchMock);

    renderAppRoutes({ initialEntries: ["/app/settings"], session: admin.session });

    await waitFor(() => {
      expect(screen.getByDisplayValue("Fazenda Boa Vista")).toBeInTheDocument();
    });

    expect(screen.getByDisplayValue("52.00")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Salvar" })).toBeDisabled();
  });

  it("enables save when fields are edited and submits PATCH on save", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;

    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = typeof input === "string" ? input : input.toString();
      const method = init?.method ?? "GET";

      if (url.endsWith("/api/v1/settings") && method === "GET") {
        return json({ farmId: "farm-1", farmName: "Fazenda Boa Vista", carcassYieldPercent: "52.00" });
      }

      if (url.endsWith("/api/v1/settings") && method === "PATCH") {
        const body = JSON.parse(init?.body as string);
        return json({ farmId: "farm-1", farmName: body.farmName, carcassYieldPercent: body.carcassYieldPercent });
      }

      throw new Error(`Unexpected request: ${method} ${url}`);
    });

    vi.stubGlobal("fetch", fetchMock);

    renderAppRoutes({ initialEntries: ["/app/settings"], session: admin.session });

    await waitFor(() => {
      expect(screen.getByDisplayValue("Fazenda Boa Vista")).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText("Nome da fazenda"), { target: { value: "Fazenda Nova" } });

    expect(screen.getByRole("button", { name: "Salvar" })).not.toBeDisabled();

    fireEvent.click(screen.getByRole("button", { name: "Salvar" }));

    await waitFor(() => {
      expect(screen.getByText("Configurações salvas.")).toBeInTheDocument();
    });

    const patchCall = fetchMock.mock.calls.find((call) => {
      const u = typeof call[0] === "string" ? call[0] : call[0].toString();
      return u.endsWith("/api/v1/settings") && call[1]?.method === "PATCH";
    });
    expect(patchCall).toBeDefined();
    expect(patchCall?.[1]?.body).toContain('"farmName":"Fazenda Nova"');
  });

  it("reverts to loaded values when cancel is clicked", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;

    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = typeof input === "string" ? input : input.toString();
      const method = init?.method ?? "GET";

      if (url.endsWith("/api/v1/settings") && method === "GET") {
        return json({ farmId: "farm-1", farmName: "Fazenda Original", carcassYieldPercent: "48.50" });
      }

      throw new Error(`Unexpected request: ${method} ${url}`);
    });

    vi.stubGlobal("fetch", fetchMock);

    renderAppRoutes({ initialEntries: ["/app/settings"], session: admin.session });

    await waitFor(() => {
      expect(screen.getByDisplayValue("Fazenda Original")).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText("Nome da fazenda"), { target: { value: "Fazenda Editada" } });
    expect(screen.getByDisplayValue("Fazenda Editada")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Cancelar" }));

    expect(screen.getByDisplayValue("Fazenda Original")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Salvar" })).toBeDisabled();
  });
});

function json(body: unknown, init: ResponseInit = { status: 200 }) {
  return Promise.resolve(new Response(JSON.stringify(body), init));
}
