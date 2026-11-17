import { fireEvent, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { renderAppRoutes } from "./support/adminWebTestSupport";
import { adminWebRoleScenarios } from "./support/roleMatrix";

describe("admin users page", () => {
  it("loads users list, filters by search and role, and sends a user invite", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;

    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = typeof input === "string" ? input : input.toString();
      const method = init?.method ?? "GET";

      if (url.includes("/api/v1/users?") && method === "GET") {
        return json({
          items: [
            { id: "user-1", name: "João Silva", phone: "+5511999990001", role: "admin", active: true }
          ],
          total: 1,
          limit: 20,
          offset: 0
        });
      }

      if (url.endsWith("/api/v1/invites") && method === "GET") {
        return json([]);
      }

      if (url.endsWith("/api/v1/invites") && method === "POST") {
        return json({
          id: "invite-1",
          email: "maria@example.com",
          phone: null,
          role: "worker",
          status: "pending"
        });
      }

      throw new Error(`Unexpected request: ${method} ${url}`);
    });

    vi.stubGlobal("fetch", fetchMock);

    renderAppRoutes({ initialEntries: ["/app/users"], session: admin.session });

    await waitFor(() => {
      expect(screen.getByText("João Silva")).toBeInTheDocument();
    });

    expect(screen.getByText("+5511999990001")).toBeInTheDocument();
    expect(screen.getAllByText("Admin").length).toBeGreaterThan(0);
    expect(screen.getByText("1 usuários encontrados")).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("Buscar usuários"), { target: { value: "silva" } });

    await waitFor(() => {
      const searchCall = fetchMock.mock.calls.find((call) => {
        const u = typeof call[0] === "string" ? call[0] : call[0].toString();
        return u.includes("search=silva");
      });
      expect(searchCall).toBeDefined();
    });

    fireEvent.change(screen.getByLabelText("Filter role"), { target: { value: "admin" } });

    await waitFor(() => {
      const roleCall = fetchMock.mock.calls.find((call) => {
        const u = typeof call[0] === "string" ? call[0] : call[0].toString();
        return u.includes("role=admin");
      });
      expect(roleCall).toBeDefined();
    });

    fireEvent.change(screen.getByLabelText("E-mail"), { target: { value: "maria@example.com" } });
    fireEvent.change(screen.getByLabelText("Cargo"), { target: { value: "worker" } });

    fireEvent.click(screen.getByRole("button", { name: "Enviar convite" }));

    await waitFor(() => {
      expect(screen.getByText("Convite enviado.")).toBeInTheDocument();
    });

    const inviteCall = fetchMock.mock.calls.find((call) => {
      const u = typeof call[0] === "string" ? call[0] : call[0].toString();
      return u.endsWith("/api/v1/invites") && call[1]?.method === "POST";
    });
    expect(inviteCall).toBeDefined();
    expect(inviteCall?.[1]?.body).toContain('"email":"maria@example.com"');
    expect(inviteCall?.[1]?.body).toContain('"role":"worker"');
  });

  it("shows user detail and revokes member access", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;

    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = typeof input === "string" ? input : input.toString();
      const method = init?.method ?? "GET";

      if (url.includes("/api/v1/users?") && method === "GET") {
        return json({
          items: [
            { id: "user-1", name: "Pedro Melo", phone: "+5511999993333", role: "veterinarian", active: true }
          ],
          total: 1,
          limit: 20,
          offset: 0
        });
      }

      if (url.endsWith("/api/v1/invites") && method === "GET") {
        return json([]);
      }

      if (url.includes("/api/v1/members/user-1") && method === "DELETE") {
        return json({ id: "user-1", farmId: "farm-1", name: "Pedro Melo", phone: "+5511999993333", role: "veterinarian", active: false });
      }

      if (url.includes("/api/v1/users/user-1/activate") && method === "POST") {
        return json({ id: "user-1", farmId: "farm-1", name: "Pedro Melo", phone: "+5511999993333", role: "veterinarian", active: true });
      }

      throw new Error(`Unexpected request: ${method} ${url}`);
    });

    vi.stubGlobal("fetch", fetchMock);

    renderAppRoutes({ initialEntries: ["/app/users"], session: admin.session });

    await waitFor(() => {
      expect(screen.getByText("Pedro Melo")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole("button", { name: /pedro melo/i }));

    await waitFor(() => {
      expect(screen.getByText("Detalhes do usuário")).toBeInTheDocument();
    });

    expect(screen.getByRole("button", { name: "Revogar acesso" })).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Revogar acesso" }));

    await waitFor(() => {
      expect(screen.queryByText("Detalhes do usuário")).not.toBeInTheDocument();
    });

    const revokeCall = fetchMock.mock.calls.find((call) => {
      const u = typeof call[0] === "string" ? call[0] : call[0].toString();
      return u.includes("/api/v1/members/user-1") && call[1]?.method === "DELETE";
    });
    expect(revokeCall).toBeDefined();
  });

  it("shows empty list state when no users exist", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;

    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = typeof input === "string" ? input : input.toString();
      const method = init?.method ?? "GET";

      if (url.includes("/api/v1/users?") && method === "GET") {
        return json({ items: [], total: 0, limit: 20, offset: 0 });
      }

      if (url.endsWith("/api/v1/invites") && method === "GET") {
        return json([]);
      }

      throw new Error(`Unexpected request: ${method} ${url}`);
    });

    vi.stubGlobal("fetch", fetchMock);

    renderAppRoutes({ initialEntries: ["/app/users"], session: admin.session });

    await waitFor(() => {
      expect(screen.getByText("0 usuários encontrados")).toBeInTheDocument();
    });

    expect(screen.getByText("Nenhum usuário encontrado com os filtros atuais.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Enviar convite" })).toBeInTheDocument();
  });
});

function json(body: unknown, init: ResponseInit = { status: 200 }) {
  return Promise.resolve(new Response(JSON.stringify(body), init));
}
