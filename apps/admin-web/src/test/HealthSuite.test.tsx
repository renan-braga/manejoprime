import { screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { renderAppRoutes } from "./support/adminWebTestSupport";
import { adminWebRoleScenarios } from "./support/roleMatrix";

const OCCURRENCES_RESPONSE = {
  items: [{ id: "occ-1", animalId: "animal-1", animalIdentifier: "347", animalName: "Trovao", type: "health", severity: "high", status: "open", description: "Animal limping after transport.", photoUrl: null, source: "app", recordedBy: "u-1", resolvedBy: null, createdAt: "2026-04-26T08:00:00Z", updatedAt: "2026-04-26T09:30:00Z" }],
  total: 1, limit: 20, offset: 0
};

function setupHealthMock() {
  const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
    const url = typeof input === "string" ? input : input.toString();
    if (url.includes("/api/v1/occurrences?")) return json(OCCURRENCES_RESPONSE);
    throw new Error(`Unexpected: ${url}`);
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

function json(body: unknown, init: ResponseInit = { status: 200 }) {
  return Promise.resolve(new Response(JSON.stringify(body), init));
}

describe("health page migration", () => {
  it("renders page heading 'Health' as h1", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setupHealthMock();
    renderAppRoutes({ initialEntries: ["/app/health"], session: admin.session });

    await waitFor(() => expect(screen.getByRole("heading", { level: 1, name: "Saúde" })).toBeInTheDocument());
  });

  it("keeps dedicated health workflows out of the inline health route", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setupHealthMock();
    renderAppRoutes({ initialEntries: ["/app/health"], session: admin.session });

    await waitFor(() => expect(screen.getByRole("heading", { level: 1, name: "Saúde" })).toBeInTheDocument());
    expect(screen.queryByRole("tab", { name: "Vaccines" })).not.toBeInTheDocument();
    expect(screen.queryByRole("tab", { name: "Occurrences" })).not.toBeInTheDocument();
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
    expect(screen.queryByText("Aftosa")).not.toBeInTheDocument();
    expect(screen.queryByText("Ivermectin")).not.toBeInTheDocument();
    expect(screen.queryByText("Animal limping after transport.")).not.toBeInTheDocument();
  });
});

describe("admin health page", () => {
  it("keeps the health landing route focused on dedicated workflow navigation", async () => {
    const admin = adminWebRoleScenarios.find((scenario) => scenario.name === "admin");
    if (admin == null) {
      throw new Error("Admin role scenario is required for health page coverage.");
    }

    renderAppRoutes({ initialEntries: ["/app/health"], session: admin.session });

    await waitFor(() => {
      expect(screen.getByRole("heading", { level: 1, name: "Saúde" })).toBeInTheDocument();
    });

    expect(screen.getByText("Os fluxos de saúde estão em rotas dedicadas.")).toBeInTheDocument();
    screen.getAllByRole("link", { name: /Medicamentos/i }).forEach((link) => {
      expect(link).toHaveAttribute("href", "/app/health/medications");
    });
    expect(screen.queryByText("Ivermectin")).not.toBeInTheDocument();
  });
});
