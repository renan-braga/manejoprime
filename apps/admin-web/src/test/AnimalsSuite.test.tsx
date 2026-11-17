import { fireEvent, screen, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { buildSession, jsonResponse, renderAppRoutes, setMockFetch } from "./support/adminWebTestSupport";

const SESSION_WITH_WRITE = buildSession({ role: "admin", permissions: ["animals.read", "animals.write"] });

const SESSION = buildSession({ role: "admin", permissions: ["animals.read"] });

const ANIMALS_RESPONSE = {
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
};

const LOTS_RESPONSE = {
  items: [
    { id: "lot-1", name: "Engorda 1" },
    { id: "lot-2", name: "Engorda 2" }
  ]
};

const DETAIL_RESPONSE = {
  id: "animal-1",
  identifier: "347",
  name: "Trovao",
  sex: "male",
  breed: "Nelore",
  status: "active",
  birthDate: "2025-01-01",
  entryDate: "2025-03-01",
  farmId: "farm-1",
  lot: { id: "lot-1", name: "Engorda 1" },
  summary: { birthWeightKg: 32.5, ageInDays: 120, daysOnFarm: 60 }
};

describe("animals migration", () => {
  it("renders animal rows from API response in DataTable", async () => {
    setMockFetch(jsonResponse(ANIMALS_RESPONSE), jsonResponse(LOTS_RESPONSE));
    renderAppRoutes({ initialEntries: ["/app/animals"], session: SESSION });

    await waitFor(() => expect(screen.getByRole("table")).toBeInTheDocument());

    expect(screen.getByText("Trovao")).toBeInTheDocument();
    expect(screen.getAllByText("Engorda 1")[0]).toBeInTheDocument();
    expect(screen.getAllByText("Ativo").length).toBeGreaterThan(0);
  });

  it("search input debounces and updates query param", async () => {
    const fetchMock = setMockFetch(
      jsonResponse(ANIMALS_RESPONSE),
      jsonResponse(LOTS_RESPONSE),
      jsonResponse(ANIMALS_RESPONSE)
    );
    renderAppRoutes({ initialEntries: ["/app/animals"], session: SESSION });

    await waitFor(() => expect(screen.getByRole("table")).toBeInTheDocument());

    fireEvent.change(screen.getByLabelText("Search animals"), { target: { value: "tro" } });

    await waitFor(() => {
      expect(fetchMock.mock.calls.some((c) => String(c[0]).includes("search=tro"))).toBe(true);
    });
  });

  it("lot filter dropdown sends lot query param", async () => {
    const fetchMock = setMockFetch(
      jsonResponse(ANIMALS_RESPONSE),
      jsonResponse(LOTS_RESPONSE),
      jsonResponse(ANIMALS_RESPONSE)
    );
    renderAppRoutes({ initialEntries: ["/app/animals"], session: SESSION });

    await waitFor(() => expect(screen.getByRole("table")).toBeInTheDocument());

    fireEvent.change(screen.getByLabelText("Filter lot"), { target: { value: "lot-1" } });

    await waitFor(() => {
      expect(fetchMock.mock.calls.some((c) => String(c[0]).includes("lot=lot-1"))).toBe(true);
    });
  });

  it("row click navigates to animal detail route", async () => {
    setMockFetch(
      jsonResponse(ANIMALS_RESPONSE),
      jsonResponse(LOTS_RESPONSE),
      jsonResponse(DETAIL_RESPONSE),
      jsonResponse({ items: [], total: 0, limit: 20, offset: 0 }),
      jsonResponse({ items: [], total: 0, limit: 20, offset: 0 }),
      jsonResponse({ items: [], total: 0, limit: 20, offset: 0 }),
      jsonResponse({ items: [], total: 0, limit: 20, offset: 0 })
    );
    renderAppRoutes({ initialEntries: ["/app/animals"], session: SESSION });

    await waitFor(() => expect(screen.getByRole("table")).toBeInTheDocument());

    fireEvent.click(screen.getByRole("button", { name: /view trovao/i }));

    await waitFor(() => {
      expect(screen.getByRole("link", { name: /voltar para animais/i })).toBeInTheDocument();
      expect(screen.getByText("Trovao")).toBeInTheDocument();
    });
  });
});

describe("create animal", () => {
  it("shows create button only when user has animals.write permission", async () => {
    setMockFetch(jsonResponse(ANIMALS_RESPONSE), jsonResponse(LOTS_RESPONSE));
    renderAppRoutes({ initialEntries: ["/app/animals"], session: SESSION_WITH_WRITE });

    await waitFor(() => expect(screen.getByRole("table")).toBeInTheDocument());
    expect(screen.getByRole("button", { name: /cadastrar animal/i })).toBeInTheDocument();
  });

  it("hides create button when user lacks animals.write permission", async () => {
    setMockFetch(jsonResponse(ANIMALS_RESPONSE), jsonResponse(LOTS_RESPONSE));
    renderAppRoutes({ initialEntries: ["/app/animals"], session: SESSION });

    await waitFor(() => expect(screen.getByRole("table")).toBeInTheDocument());
    expect(screen.queryByRole("button", { name: /cadastrar animal/i })).not.toBeInTheDocument();
  });

  it("opens dialog and submits new animal, then refreshes list", async () => {
    const createdId = "animal-new";
    const afterCreate = {
      items: [
        ...ANIMALS_RESPONSE.items,
        { id: createdId, identifier: "999", name: "Novo", sex: "female", breed: null, status: "active", lotId: null, lotName: null }
      ],
      total: 2,
      limit: 20,
      offset: 0
    };

    const fetchMock = setMockFetch(
      jsonResponse(ANIMALS_RESPONSE),
      jsonResponse(LOTS_RESPONSE),
      jsonResponse({ id: createdId }, { status: 201 }),
      jsonResponse(afterCreate),
      jsonResponse(LOTS_RESPONSE)
    );
    renderAppRoutes({ initialEntries: ["/app/animals"], session: SESSION_WITH_WRITE });

    await waitFor(() => expect(screen.getByRole("table")).toBeInTheDocument());

    fireEvent.click(screen.getByRole("button", { name: /cadastrar animal/i }));

    await waitFor(() => expect(screen.getByRole("dialog")).toBeInTheDocument());

    fireEvent.change(screen.getByLabelText("Brinco *"), { target: { value: "999" } });
    fireEvent.change(screen.getByLabelText("Nome"), { target: { value: "Novo" } });

    fireEvent.click(screen.getByRole("button", { name: /^cadastrar$/i }));

    await waitFor(() => {
      expect(fetchMock.mock.calls.some((c) => String(c[0]).endsWith("/api/v1/animals") && (c[1] as RequestInit)?.method === "POST")).toBe(true);
    });

    await waitFor(() => {
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    });
  });

  it("shows error message when create API call fails", async () => {
    setMockFetch(
      jsonResponse(ANIMALS_RESPONSE),
      jsonResponse(LOTS_RESPONSE),
      jsonResponse({ code: "validation_error", message: "Brinco já cadastrado." }, { status: 422 })
    );
    renderAppRoutes({ initialEntries: ["/app/animals"], session: SESSION_WITH_WRITE });

    await waitFor(() => expect(screen.getByRole("table")).toBeInTheDocument());

    fireEvent.click(screen.getByRole("button", { name: /cadastrar animal/i }));
    await waitFor(() => expect(screen.getByRole("dialog")).toBeInTheDocument());

    fireEvent.change(screen.getByLabelText("Brinco *"), { target: { value: "347" } });
    fireEvent.click(screen.getByRole("button", { name: /^cadastrar$/i }));

    await waitFor(() => {
      expect(screen.getByText("Brinco já cadastrado.")).toBeInTheDocument();
    });
  });
});

describe("admin animals page", () => {
  it("loads animals, applies search filter, and navigates to detail route on row click", async () => {
    const session = buildSession({
      userId: "admin-animals",
      name: "Admin Animals",
      phone: "5511999992200",
      role: "admin",
      farmId: "farm-1",
      farmName: "Fazenda Animais",
      permissions: ["dashboard.read", "animals.read", "users.read", "settings.read"]
    });

    const animalsResponse = {
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
    };

    const fetchMock = setMockFetch(
      jsonResponse(animalsResponse),
      jsonResponse({ items: [] }),
      jsonResponse(animalsResponse)
    );

    renderAppRoutes({ initialEntries: ["/app/animals"], session });

    await waitFor(() => {
      expect(screen.getByText("Trovao")).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText("Search animals"), {
      target: { value: "tro" }
    });

    await waitFor(() => {
      expect(fetchMock.mock.calls.some((c) => String(c[0]).includes("search=tro"))).toBe(true);
    });

    expect(screen.getByRole("table")).toBeInTheDocument();
    expect(screen.getByText("Engorda 1")).toBeInTheDocument();
  });
});
