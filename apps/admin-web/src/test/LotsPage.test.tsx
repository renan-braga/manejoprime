import { fireEvent, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { renderAppRoutes } from "./support/adminWebTestSupport";
import { adminWebRoleScenarios } from "./support/roleMatrix";

function setupMock(overrides?: (url: string, method: string) => Response | null) {
  let lot1Status = "active";

  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = typeof input === "string" ? input : input.toString();
    const method = init?.method ?? "GET";

    const custom = overrides?.(url, method);
    if (custom != null) return custom;

    if (url.includes("/api/v1/lots") && method === "GET") {
      return json({
        items: [
          { id: "lot-1", name: "Engorda 1", animalsCount: 18, status: lot1Status },
          { id: "lot-2", name: "Recria 2", animalsCount: 5, status: "active" }
        ],
        total: 2,
        limit: 20,
        offset: 0
      });
    }

    if (url.endsWith("/api/v1/lots") && method === "POST") {
      return json({ id: "lot-3", name: "Cria 3", animalsCount: 0, status: "active" });
    }

    if (url.includes("/api/v1/lots/lot-1") && method === "PATCH") {
      lot1Status = "archived";
      return json({ id: "lot-1", name: "Engorda 1", animalsCount: 18, status: "archived" });
    }

    throw new Error(`Unexpected request: ${method} ${url}`);
  });

  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

describe("lots management page", () => {
  it("renders lots list with name, head count, and status from API", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setupMock();
    renderAppRoutes({ initialEntries: ["/app/lots"], session: admin.session });

    await waitFor(() => expect(screen.getByText("Engorda 1")).toBeInTheDocument());

    expect(screen.getByText("Recria 2")).toBeInTheDocument();
    expect(screen.getByText("18")).toBeInTheDocument();
    expect(screen.getByText("5")).toBeInTheDocument();
    expect(screen.getAllByText("Ativo").length).toBeGreaterThan(0);
    expect(screen.getByRole("button", { name: /novo lote/i })).toBeInTheDocument();
  });

  it("create dialog submits POST /api/v1/lots and shows new lot", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    const fetchMock = setupMock();
    renderAppRoutes({ initialEntries: ["/app/lots"], session: admin.session });

    await waitFor(() => expect(screen.getByText("Engorda 1")).toBeInTheDocument());

    fireEvent.click(screen.getByRole("button", { name: /novo lote/i }));

    await waitFor(() => expect(screen.getByRole("dialog")).toBeInTheDocument());

    fireEvent.change(screen.getByLabelText("Nome do lote"), { target: { value: "Cria 3" } });
    fireEvent.click(screen.getByRole("button", { name: /^criar lote$/i }));

    await waitFor(() => expect(screen.getByText("Cria 3")).toBeInTheDocument());

    const postCall = fetchMock.mock.calls.find((c) => {
      const u = typeof c[0] === "string" ? c[0] : c[0].toString();
      return u.endsWith("/api/v1/lots") && c[1]?.method === "POST";
    });
    expect(postCall).toBeDefined();
    expect(postCall?.[1]?.body).toContain('"name":"Cria 3"');
  });

  it("archive confirmation sends PATCH /api/v1/lots/:id and updates row", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    const fetchMock = setupMock();
    renderAppRoutes({ initialEntries: ["/app/lots"], session: admin.session });

    await waitFor(() => expect(screen.getByText("Engorda 1")).toBeInTheDocument());

    fireEvent.click(screen.getByRole("button", { name: /archive engorda 1/i }));

    await waitFor(() => expect(screen.getByRole("alertdialog")).toBeInTheDocument());

    fireEvent.click(screen.getByRole("button", { name: /^arquivar$/i }));

    await waitFor(() => expect(screen.getByText("Arquivado")).toBeInTheDocument());

    const patchCall = fetchMock.mock.calls.find((c) => {
      const u = typeof c[0] === "string" ? c[0] : c[0].toString();
      return u.includes("/api/v1/lots/lot-1") && c[1]?.method === "PATCH";
    });
    expect(patchCall).toBeDefined();
    expect(patchCall?.[1]?.body).toContain('"status":"archived"');
  });

  it("worker sees lots list but no create or archive controls", async () => {
    const worker = adminWebRoleScenarios.find((s) => s.name === "worker")!;
    setupMock();
    renderAppRoutes({ initialEntries: ["/app/lots"], session: worker.session });

    await waitFor(() => expect(screen.getByText("Engorda 1")).toBeInTheDocument());

    expect(screen.getByText("Recria 2")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /new lot/i })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /archive/i })).not.toBeInTheDocument();
  });
});

function json(body: unknown, init: ResponseInit = { status: 200 }) {
  return Promise.resolve(new Response(JSON.stringify(body), init));
}
