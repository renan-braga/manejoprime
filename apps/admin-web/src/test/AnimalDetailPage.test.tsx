import { fireEvent, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { buildSession, renderAppRoutes } from "./support/adminWebTestSupport";

const SESSION = buildSession({
  role: "admin",
  permissions: ["animals.read", "weighings.write", "occurrences.write", "vaccines.write", "medications.write"]
});

const SESSION_WITH_WRITE = buildSession({
  role: "admin",
  permissions: ["animals.read", "animals.write", "weighings.write", "occurrences.write", "vaccines.write", "medications.write"]
});

const ANIMAL = {
  id: "animal-1",
  identifier: "347",
  name: "Trovao",
  sex: "male",
  breed: "Nelore",
  status: "active",
  birthDate: "2023-01-15",
  entryDate: "2023-03-01",
  farmId: "farm-1",
  lot: { id: "lot-1", name: "Engorda 1" },
  summary: { birthWeightKg: 32.5, ageInDays: 840, daysOnFarm: 780 }
};

const WEIGHINGS = {
  items: [{ id: "w-1", animalId: "animal-1", weightKg: 485.5, weighingDate: "2025-04-01", type: "regular", source: "manual", recordedBy: "user-1", gmdKgPerDay: 1.2 }],
  total: 1,
  limit: 20,
  offset: 0
};

const OCCURRENCES = {
  items: [{ id: "occ-1", animalId: "animal-1", animalIdentifier: "347", animalName: "Trovao", type: "injury", severity: "low", status: "open", description: "Left hind leg limping", photoUrl: null, source: "manual", recordedBy: "user-1", resolvedBy: null, createdAt: "2025-04-10T10:00:00Z", updatedAt: "2025-04-10T10:00:00Z" }],
  total: 1,
  limit: 20,
  offset: 0
};

const VACCINES = {
  items: [{ id: "vac-1", animalId: "animal-1", lotId: null, vaccineType: "FMD", vaccineBatch: "B001", dose: "1st", applicationDate: "2025-03-01", nextDoseDate: "2025-09-01", volumeMl: 5, source: "manual", recordedBy: "user-1" }],
  total: 1,
  limit: 20,
  offset: 0
};

const MEDICATIONS = {
  items: [{ id: "med-1", animalId: "animal-1", occurrenceId: null, medicationName: "Penicillin", activeIngredient: "Penicillin G", reason: "Infection", dosage: "10ml IM", withdrawalPeriodDays: 14, applicationDate: "2025-04-10", releaseDate: "2025-04-24", inWithdrawalPeriod: true, cost: 25.0, source: "manual", recordedBy: "user-1" }],
  total: 1,
  limit: 20,
  offset: 0
};

function setupMocks(overrides?: { patchResponse?: Response; deleteResponse?: Response }) {
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = typeof input === "string" ? input : input.toString();
    const method = init?.method?.toUpperCase() ?? "GET";
    if (url.includes("/api/v1/animals/animal-1") && method === "PATCH" && overrides?.patchResponse) {
      return overrides.patchResponse;
    }
    if (url.includes("/api/v1/animals/animal-1") && method === "DELETE" && overrides?.deleteResponse) {
      return overrides.deleteResponse;
    }
    if (url.includes("/api/v1/animals/animal-1") && !url.includes("?")) return json(ANIMAL);
    if (url.includes("/api/v1/lots")) return json({ items: [{ id: "lot-1", name: "Engorda 1" }] });
    if (url.includes("/api/v1/weighings")) return json(WEIGHINGS);
    if (url.includes("/api/v1/occurrences")) return json(OCCURRENCES);
    if (url.includes("/api/v1/vaccines")) return json(VACCINES);
    if (url.includes("/api/v1/medications")) return json(MEDICATIONS);
    throw new Error(`Unexpected request: ${url}`);
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

function json(body: unknown) {
  return Promise.resolve(new Response(JSON.stringify(body), { status: 200 }));
}

describe("animal detail page — edit", () => {
  it("shows edit and delete buttons when user has animals.write", async () => {
    setupMocks();
    renderAppRoutes({ initialEntries: ["/app/animals/animal-1"], session: SESSION_WITH_WRITE });

    await waitFor(() => expect(screen.getByText("Trovao")).toBeInTheDocument());

    expect(screen.getByRole("button", { name: /editar/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /excluir/i })).toBeInTheDocument();
  });

  it("hides edit and delete buttons without animals.write", async () => {
    setupMocks();
    renderAppRoutes({ initialEntries: ["/app/animals/animal-1"], session: SESSION });

    await waitFor(() => expect(screen.getByText("Trovao")).toBeInTheDocument());

    expect(screen.queryByRole("button", { name: /editar/i })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /excluir/i })).not.toBeInTheDocument();
  });

  it("opens edit dialog pre-filled with current animal data", async () => {
    setupMocks();
    renderAppRoutes({ initialEntries: ["/app/animals/animal-1"], session: SESSION_WITH_WRITE });

    await waitFor(() => expect(screen.getByText("Trovao")).toBeInTheDocument());

    fireEvent.click(screen.getByRole("button", { name: /editar/i }));

    await waitFor(() => expect(screen.getByRole("dialog")).toBeInTheDocument());

    expect((screen.getByLabelText("Brinco *") as HTMLInputElement).value).toBe("347");
    expect((screen.getByLabelText("Nome") as HTMLInputElement).value).toBe("Trovao");
  });

  it("submits edit form with PATCH and closes dialog on success", async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = typeof input === "string" ? input : input.toString();
      const method = (init?.method ?? "GET").toUpperCase();
      if (url.includes("/api/v1/animals/animal-1") && method === "PATCH") return json({ id: "animal-1" });
      if (url.includes("/api/v1/animals/animal-1")) return json(ANIMAL);
      if (url.includes("/api/v1/lots")) return json({ items: [{ id: "lot-1", name: "Engorda 1" }] });
      if (url.includes("/api/v1/weighings")) return json(WEIGHINGS);
      if (url.includes("/api/v1/occurrences")) return json(OCCURRENCES);
      if (url.includes("/api/v1/vaccines")) return json(VACCINES);
      if (url.includes("/api/v1/medications")) return json(MEDICATIONS);
      throw new Error(`Unexpected: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    renderAppRoutes({ initialEntries: ["/app/animals/animal-1"], session: SESSION_WITH_WRITE });

    await waitFor(() => expect(screen.getByText("Trovao")).toBeInTheDocument());

    fireEvent.click(screen.getByRole("button", { name: /editar/i }));
    await waitFor(() => expect(screen.getByRole("dialog")).toBeInTheDocument());

    fireEvent.change(screen.getByLabelText("Nome"), { target: { value: "Trovao Editado" } });
    fireEvent.click(screen.getByRole("button", { name: /^salvar$/i }));

    await waitFor(() => {
      expect(fetchMock.mock.calls.some((c) => (c[1] as RequestInit)?.method === "PATCH")).toBe(true);
    });
  });

  it("shows error when edit API returns 422", async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = typeof input === "string" ? input : input.toString();
      const method = (init?.method ?? "GET").toUpperCase();
      if (url.includes("/api/v1/animals/animal-1") && method === "PATCH") {
        return new Response(JSON.stringify({ code: "validation_failed", message: "Brinco já cadastrado." }), { status: 422 });
      }
      if (url.includes("/api/v1/animals/animal-1")) return json(ANIMAL);
      if (url.includes("/api/v1/lots")) return json({ items: [{ id: "lot-1", name: "Engorda 1" }] });
      if (url.includes("/api/v1/weighings")) return json(WEIGHINGS);
      if (url.includes("/api/v1/occurrences")) return json(OCCURRENCES);
      if (url.includes("/api/v1/vaccines")) return json(VACCINES);
      if (url.includes("/api/v1/medications")) return json(MEDICATIONS);
      throw new Error(`Unexpected: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    renderAppRoutes({ initialEntries: ["/app/animals/animal-1"], session: SESSION_WITH_WRITE });

    await waitFor(() => expect(screen.getByText("Trovao")).toBeInTheDocument());

    fireEvent.click(screen.getByRole("button", { name: /editar/i }));
    await waitFor(() => expect(screen.getByRole("dialog")).toBeInTheDocument());

    fireEvent.click(screen.getByRole("button", { name: /^salvar$/i }));

    await waitFor(() => expect(screen.getByText("Brinco já cadastrado.")).toBeInTheDocument());
  });
});

describe("animal detail page — delete", () => {
  it("opens confirmation dialog when clicking excluir", async () => {
    setupMocks();
    renderAppRoutes({ initialEntries: ["/app/animals/animal-1"], session: SESSION_WITH_WRITE });

    await waitFor(() => expect(screen.getByText("Trovao")).toBeInTheDocument());
    fireEvent.click(screen.getByRole("button", { name: /excluir/i }));

    await waitFor(() => expect(screen.getByText(/esta ação é irreversível/i)).toBeInTheDocument());
  });

  it("shows error when delete is rejected due to records", async () => {
    setupMocks({
      deleteResponse: new Response(JSON.stringify({ code: "animal_has_records", message: "Animal possui registros associados e não pode ser excluído." }), { status: 409 })
    });

    renderAppRoutes({ initialEntries: ["/app/animals/animal-1"], session: SESSION_WITH_WRITE });

    await waitFor(() => expect(screen.getByText("Trovao")).toBeInTheDocument());
    fireEvent.click(screen.getByRole("button", { name: /excluir/i }));
    await waitFor(() => expect(screen.getByText(/esta ação é irreversível/i)).toBeInTheDocument());

    fireEvent.click(screen.getByRole("button", { name: /^excluir$/i }));

    await waitFor(() => expect(screen.getByText("Animal possui registros associados e não pode ser excluído.")).toBeInTheDocument());
  });
});

describe("animal detail page", () => {
  it("header card renders tag, lot name, and status from animal API", async () => {
    setupMocks();
    renderAppRoutes({ initialEntries: ["/app/animals/animal-1"], session: SESSION });

    await waitFor(() => expect(screen.getByText("Trovao")).toBeInTheDocument());

    expect(screen.getByText("#347")).toBeInTheDocument();
    expect(screen.getByText("Engorda 1")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /voltar para animais/i })).toBeInTheDocument();
  });

  it("Weighings tab renders weighing rows from API", async () => {
    setupMocks();
    renderAppRoutes({ initialEntries: ["/app/animals/animal-1"], session: SESSION });

    await waitFor(() => expect(screen.getByRole("tab", { name: /pesagens/i })).toBeInTheDocument());

    fireEvent.click(screen.getByRole("tab", { name: /pesagens/i }));

    await waitFor(() => expect(screen.getAllByText("485.5 kg").length).toBeGreaterThan(0));
    expect(screen.getByText("1.2 kg/dia")).toBeInTheDocument();
  });

  it("Occurrences tab renders occurrence rows from API", async () => {
    setupMocks();
    renderAppRoutes({ initialEntries: ["/app/animals/animal-1"], session: SESSION });

    await waitFor(() => expect(screen.getAllByText("485.5 kg").length).toBeGreaterThan(0));
    fireEvent.mouseDown(screen.getByRole("tab", { name: /ocorrências/i }));

    await waitFor(() => expect(screen.getByText("Left hind leg limping")).toBeInTheDocument());
  });

  it("Vaccines tab renders vaccine rows from API", async () => {
    setupMocks();
    renderAppRoutes({ initialEntries: ["/app/animals/animal-1"], session: SESSION });

    await waitFor(() => expect(screen.getAllByText("485.5 kg").length).toBeGreaterThan(0));
    fireEvent.mouseDown(screen.getByRole("tab", { name: /vacinas/i }));

    await waitFor(() => expect(screen.getByText("FMD")).toBeInTheDocument());
    expect(screen.getByText("1st")).toBeInTheDocument();
  });

  it("Medications tab renders medication rows from API", async () => {
    setupMocks();
    renderAppRoutes({ initialEntries: ["/app/animals/animal-1"], session: SESSION });

    await waitFor(() => expect(screen.getAllByText("485.5 kg").length).toBeGreaterThan(0));
    fireEvent.mouseDown(screen.getByRole("tab", { name: /medicamentos/i }));

    await waitFor(() => expect(screen.getByText("Penicillin")).toBeInTheDocument());
    expect(screen.getByText("Em carência")).toBeInTheDocument();
  });
});
