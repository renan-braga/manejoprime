import { act, screen, waitFor } from "@testing-library/react";
import { toast } from "sonner";
import { describe, expect, it } from "vitest";
import { jsonResponse, renderAppRoutes, setMockFetch, setPendingFetch } from "./support/adminWebTestSupport";
import {
  adminWebRoleScenarios,
  forbiddenAdminWebRouteScenarios
} from "./support/roleMatrix";

function setupDashboardMock() {
  setMockFetch(
    jsonResponse({
      totalAnimals: 10,
      activeAnimals: 9,
      lotsCount: 2,
      activeUsers: 3,
      statusBreakdown: { active: 9, sold: 0, dead: 1, transferred: 0 },
      alerts: [],
      shortcuts: []
    }),
    jsonResponse([]),
    jsonResponse([])
  );
}

describe("admin web and mobile boundary ownership regression", () => {
  it("keeps admin-owned management modules reachable from the web shell for admins", async () => {
    const admin = adminWebRoleScenarios.find((scenario) => scenario.name === "admin");
    if (admin == null) {
      throw new Error("Admin role scenario is required for boundary regression coverage.");
    }

    setMockFetch(
      jsonResponse({
        totalAnimals: 18,
        activeAnimals: 17,
        lotsCount: 3,
        activeUsers: 2,
        statusBreakdown: {
          active: 17,
          sold: 1,
          dead: 0,
          transferred: 0
        },
        alerts: [],
        shortcuts: ["animals", "users", "settings"]
      })
    );

    renderAppRoutes({ session: admin.session });

    await waitFor(() => {
      expect(screen.getByRole("link", { name: "Painel" })).toBeInTheDocument();
    });

    expect(screen.getAllByText("Usuários").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Configurações").length).toBeGreaterThan(0);
    expect(screen.getByText("Ofertas")).toBeInTheDocument();
    expect(screen.getByText("Relatórios")).toBeInTheDocument();
  });

  it("keeps shared animal management denser on admin web than mobile", async () => {
    const admin = adminWebRoleScenarios.find((scenario) => scenario.name === "admin");
    if (admin == null) {
      throw new Error("Admin role scenario is required for boundary regression coverage.");
    }

    setMockFetch(
      jsonResponse({
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
        limit: 20,
        offset: 0
      }),
      jsonResponse({ items: [{ id: "lot-1", name: "Engorda 1" }] })
    );

    renderAppRoutes({ initialEntries: ["/app/animals"], session: admin.session });

    await waitFor(() => {
      expect(screen.getByRole("table")).toBeInTheDocument();
    });

    expect(screen.getByLabelText("Search animals")).toBeInTheDocument();
    expect(screen.getByLabelText("Filter lot")).toBeInTheDocument();
    expect(screen.getByLabelText("Filter status")).toBeInTheDocument();
    expect(screen.queryByText(/offline/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/draft/i)).not.toBeInTheDocument();
  });
});

describe("admin web role matrix regression", () => {
  it.each(adminWebRoleScenarios)("keeps the shell navigation aligned for $name", async ({ session, visibleNavItems, hiddenNavItems }) => {
    setPendingFetch();

    renderAppRoutes({ session });

    await waitFor(() => {
      expect(screen.getByRole("link", { name: "Painel" })).toBeInTheDocument();
    });

    for (const navItem of visibleNavItems) {
      expect(screen.getByRole("link", { name: navItem })).toBeInTheDocument();
    }

    for (const navItem of hiddenNavItems) {
      expect(screen.queryByRole("link", { name: navItem })).not.toBeInTheDocument();
    }
  });

  it.each(forbiddenAdminWebRouteScenarios)(
    "redirects forbidden management routes back to the dashboard for $name",
    async ({ session, initialEntry, blockedCopy }) => {
      setPendingFetch();

      renderAppRoutes({ initialEntries: [initialEntry], session });

      await waitFor(() => {
        expect(screen.getByRole("heading", { name: "Dashboard" })).toBeInTheDocument();
      });

      expect(screen.queryByText(blockedCopy)).not.toBeInTheDocument();
    }
  );
});

describe("QA-012 shadcn pattern coverage", () => {
  it("Toaster is mounted in the admin shell and displays toasts fired programmatically", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setupDashboardMock();
    renderAppRoutes({ session: admin.session });

    await waitFor(() => expect(screen.getByText("Total de Animais")).toBeInTheDocument());

    act(() => {
      toast("QA-012 sonner smoke");
    });

    await waitFor(() =>
      expect(screen.getByText("QA-012 sonner smoke")).toBeInTheDocument()
    );
  });

  it("Toaster displays multiple sequential toasts", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setupDashboardMock();
    renderAppRoutes({ session: admin.session });

    await waitFor(() => expect(screen.getByText("Total de Animais")).toBeInTheDocument());

    act(() => {
      toast("First toast");
      toast("Second toast");
    });

    await waitFor(() => expect(screen.getByText("First toast")).toBeInTheDocument());
    expect(screen.getByText("Second toast")).toBeInTheDocument();
  });
});
