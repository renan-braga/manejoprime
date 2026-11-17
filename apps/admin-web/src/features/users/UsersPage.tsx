import { useDeferredValue, useEffect, useState } from "react";
import { useAuth } from "../auth/AuthContext";
import { usePageTour } from "@/hooks/usePageTour";
import { AppTour } from "../shell/AppTour";
import type { Step } from "react-joyride";
import { describeApiError, readFieldErrors } from "@/lib/apiErrors";
import { formatRole } from "@/lib/formatters";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { BillingGatedButton } from "@/components/BillingGatedButton";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { InfoTooltip } from "@/components/InfoTooltip";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";

type UserListResponse = {
  items: UserListItem[];
  total: number;
  limit: number;
  offset: number;
};

type UserListItem = {
  id: string;
  name: string;
  phone: string | null;
  role: string;
  active: boolean;
};

type UserMutationResponse = {
  id: string;
  farmId: string;
  name: string;
  phone: string | null;
  role: string;
  active: boolean;
};

type InviteListItem = {
  id: string;
  email: string;
  phone: string | null;
  role: string;
  status: string;
};

type InviteMutationResponse = {
  id: string;
  email: string;
  phone: string | null;
  role: string;
  status: string;
};

type InviteFormState = {
  email: string;
  phone: string;
  role: string;
};

type RoleFilter = "all" | "admin" | "veterinarian" | "worker";
type ActiveFilter = "all" | "true" | "false";

const USERS_TOUR_STEPS: Step[] = [
  {
    target: "[data-tour='users-table']",
    title: "Membros da fazenda",
    content: "Veja todos os membros da sua fazenda. Clique em um nome para ver detalhes ou gerenciar o acesso.",
    placement: "top",
  },
  {
    target: "[data-tour='users-invite']",
    title: "Convidar usuário",
    content: "Convide veterinários e funcionários por e-mail. Cada um terá acesso conforme o cargo definido.",
    placement: "left",
  },
];

export function UsersPage() {
  const { fetchProtectedJson, session } = useAuth();
  const canWrite = session?.permissions.includes("users.write") ?? false;
  const { run: runTour, finish: finishTour } = usePageTour("users");

  const [users, setUsers] = useState<UserListResponse | null>(null);
  const [listError, setListError] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState<RoleFilter>("all");
  const [activeFilter, setActiveFilter] = useState<ActiveFilter>("all");
  const [offset, setOffset] = useState(0);
  const deferredSearch = useDeferredValue(search);

  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const [invites, setInvites] = useState<InviteListItem[] | null>(null);
  const [inviteForm, setInviteForm] = useState<InviteFormState>({ email: "", phone: "", role: "worker" });
  const [inviteFieldErrors, setInviteFieldErrors] = useState<Record<string, string>>({});
  const [inviteFormError, setInviteFormError] = useState<string | null>(null);
  const [inviteSubmitting, setInviteSubmitting] = useState(false);
  const [inviteSuccess, setInviteSuccess] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function loadUsers() {
      try {
        const result = await fetchProtectedJson<UserListResponse>(
          buildListPath({ search: deferredSearch, role: roleFilter, active: activeFilter, offset })
        );
        if (!cancelled) {
          setUsers(result);
          setListError(null);
        }
      } catch (error) {
        if (!cancelled) {
          setUsers(null);
          setListError(describeApiError(error));
        }
      }
    }

    void loadUsers();

    return () => {
      cancelled = true;
    };
  }, [deferredSearch, roleFilter, activeFilter, offset, fetchProtectedJson]);

  useEffect(() => {
    if (!canWrite) return;
    let cancelled = false;

    async function loadInvites() {
      try {
        const result = await fetchProtectedJson<InviteListItem[]>("/api/v1/invites");
        if (!cancelled) setInvites(result);
      } catch {
        if (!cancelled) setInvites([]);
      }
    }

    void loadInvites();

    return () => {
      cancelled = true;
    };
  }, [canWrite, fetchProtectedJson]);

  const selectedUser = users?.items.find((u) => u.id === selectedUserId) ?? null;
  const pendingInvites = (invites ?? []).filter((i) => i.status === "pending");

  async function handleInvite(event: React.FormEvent) {
    event.preventDefault();
    setInviteFieldErrors({});
    setInviteFormError(null);
    setInviteSubmitting(true);
    setInviteSuccess(false);
    try {
      const created = await fetchProtectedJson<InviteMutationResponse>("/api/v1/invites", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          email: inviteForm.email.trim(),
          phone: inviteForm.phone.trim() || null,
          role: inviteForm.role
        })
      });
      setInvites((current) => [...(current ?? []), created]);
      setInviteForm({ email: "", phone: "", role: "worker" });
      setInviteSuccess(true);
    } catch (error) {
      setInviteFieldErrors(readFieldErrors(error));
      setInviteFormError(describeApiError(error));
    } finally {
      setInviteSubmitting(false);
    }
  }

  async function handleRevokeInvite(inviteId: string) {
    try {
      await fetchProtectedJson(`/api/v1/invites/${inviteId}`, { method: "DELETE" });
      setInvites((current) => (current ?? []).filter((i) => i.id !== inviteId));
    } catch (error) {
      setActionError(describeApiError(error));
    }
  }

  async function handleActivate(userId: string) {
    setActionError(null);
    try {
      const updated = await fetchProtectedJson<UserMutationResponse>(`/api/v1/users/${userId}/activate`, {
        method: "POST"
      });
      setUsers((current) =>
        current == null
          ? null
          : { ...current, items: current.items.map((u) => (u.id === userId ? { ...u, active: updated.active } : u)) }
      );
    } catch (error) {
      setActionError(describeApiError(error));
    }
  }

  async function handleRevokeMember(userId: string) {
    setActionError(null);
    try {
      await fetchProtectedJson(`/api/v1/members/${userId}`, { method: "DELETE" });
      setUsers((current) =>
        current == null
          ? null
          : { ...current, items: current.items.map((u) => (u.id === userId ? { ...u, active: false } : u)) }
      );
      setSelectedUserId(null);
    } catch (error) {
      setActionError(describeApiError(error));
    }
  }

  return (
    <div className="flex flex-col gap-6">
        <h1 className="font-heading text-2xl font-bold tracking-tight">Usuários</h1>

        {canWrite && pendingInvites.length > 0 && (
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Convites pendentes</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="divide-y divide-border">
                {pendingInvites.map((invite) => (
                  <div key={invite.id} className="flex items-center justify-between py-2">
                    <div>
                      <p className="text-sm font-medium">{invite.email}</p>
                      <p className="text-xs text-muted-foreground">{formatRole(invite.role)}</p>
                    </div>
                    <Button
                      onClick={() => void handleRevokeInvite(invite.id)}
                      size="sm"
                      type="button"
                      variant="outline"
                    >
                      Revogar
                    </Button>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        )}

        <div className="flex gap-6 items-start">
          <div className="flex-1 min-w-0 space-y-4">
            <div className="flex gap-2 flex-wrap">
              <Input
                aria-label="Buscar usuários"
                className="max-w-xs"
                onChange={(event) => {
                  setSearch(event.target.value);
                  setOffset(0);
                }}
                placeholder="Buscar por nome ou telefone"
                value={search}
              />
              <select
                aria-label="Filter role"
                className="h-9 rounded-md border border-input bg-background px-3 py-1 text-sm shadow-sm"
                onChange={(event) => {
                  setRoleFilter(event.target.value as RoleFilter);
                  setOffset(0);
                }}
                value={roleFilter}
              >
                <option value="all">Todos os cargos</option>
                <option value="admin">Admin</option>
                <option value="veterinarian">Veterinário</option>
                <option value="worker">Funcionário</option>
              </select>
              <select
                aria-label="Filter active"
                className="h-9 rounded-md border border-input bg-background px-3 py-1 text-sm shadow-sm"
                onChange={(event) => {
                  setActiveFilter(event.target.value as ActiveFilter);
                  setOffset(0);
                }}
                value={activeFilter}
              >
                <option value="all">Todos os status</option>
                <option value="true">Ativo</option>
                <option value="false">Inativo</option>
              </select>
            </div>

            <p className="text-sm text-muted-foreground">
              {users != null ? `${users.total} usuários encontrados` : "Carregando usuários..."}
            </p>

            {users == null && listError == null ? (
              <div data-testid="users-skeleton" className="space-y-2">
                {Array.from({ length: 5 }).map((_, i) => (
                  <Skeleton key={i} className="h-12 rounded-md" />
                ))}
              </div>
            ) : null}

            {users == null && listError != null ? (
              <Card>
                <CardHeader>
                  <CardTitle>Usuários indisponíveis</CardTitle>
                </CardHeader>
                <CardContent>
                  <p className="text-sm text-destructive">{listError}</p>
                </CardContent>
              </Card>
            ) : null}

            {users != null ? (
              <>
                <Table data-tour="users-table">
                  <TableHeader>
                    <TableRow>
                      <TableHead>Nome</TableHead>
                      <TableHead>Cargo</TableHead>
                      <TableHead>Status</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {users.items.length === 0 ? (
                      <TableRow>
                        <TableCell colSpan={3} className="text-center text-muted-foreground">
                          Nenhum usuário encontrado com os filtros atuais.
                        </TableCell>
                      </TableRow>
                    ) : (
                      users.items.map((user) => (
                        <TableRow
                          className="cursor-pointer"
                          data-active={selectedUserId === user.id}
                          key={user.id}
                          onClick={() => {
                            setSelectedUserId(selectedUserId === user.id ? null : user.id);
                            setActionError(null);
                          }}
                          role="button"
                          tabIndex={0}
                          onKeyDown={(e) => {
                            if (e.key === "Enter" || e.key === " ") {
                              setSelectedUserId(selectedUserId === user.id ? null : user.id);
                              setActionError(null);
                            }
                          }}
                        >
                          <TableCell>
                            <p className="font-semibold text-sm">{user.name}</p>
                            <p className="text-xs text-muted-foreground">{user.phone}</p>
                          </TableCell>
                          <TableCell className="text-sm">{formatRole(user.role)}</TableCell>
                          <TableCell>
                            <Badge variant={user.active ? "default" : "secondary"}>
                              {user.active ? "Ativo" : "Inativo"}
                            </Badge>
                          </TableCell>
                        </TableRow>
                      ))
                    )}
                  </TableBody>
                </Table>

                <div className="flex items-center justify-between">
                  <Button
                    disabled={users.offset === 0}
                    onClick={() => setOffset(Math.max(0, offset - users.limit))}
                    size="sm"
                    type="button"
                    variant="outline"
                  >
                    Anterior
                  </Button>
                  <span className="text-xs text-muted-foreground">
                    {users.offset + 1}–{Math.min(users.offset + users.limit, users.total)} de {users.total}
                  </span>
                  <Button
                    disabled={users.offset + users.limit >= users.total}
                    onClick={() => setOffset(offset + users.limit)}
                    size="sm"
                    type="button"
                    variant="outline"
                  >
                    Próximo
                  </Button>
                </div>
              </>
            ) : null}
          </div>

          <aside className="w-80 shrink-0">
            {selectedUser != null ? (
              <Card>
                <CardHeader>
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <p className="text-xs text-muted-foreground mb-1">Detalhes do usuário</p>
                      <CardTitle>{selectedUser.name}</CardTitle>
                      <p className="text-sm text-muted-foreground mt-1">
                        {selectedUser.phone} · {formatRole(selectedUser.role)}
                      </p>
                    </div>
                    <Button
                      onClick={() => {
                        setSelectedUserId(null);
                        setActionError(null);
                      }}
                      size="sm"
                      type="button"
                      variant="ghost"
                    >
                      Fechar
                    </Button>
                  </div>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="grid grid-cols-3 gap-3">
                    <InfoCard label="Cargo" value={formatRole(selectedUser.role)} />
                    <InfoCard label="Telefone" value={selectedUser.phone ?? "—"} />
                    <InfoCard label="Status" value={selectedUser.active ? "Ativo" : "Inativo"} />
                  </div>

                  {canWrite ? (
                    <div className="flex gap-2">
                      {selectedUser.active ? (
                        <Button
                          onClick={() => void handleRevokeMember(selectedUser.id)}
                          size="sm"
                          type="button"
                          variant="destructive"
                        >
                          Revogar acesso
                        </Button>
                      ) : (
                        <Button
                          onClick={() => void handleActivate(selectedUser.id)}
                          size="sm"
                          type="button"
                        >
                          Ativar
                        </Button>
                      )}
                    </div>
                  ) : null}

                  {actionError != null ? (
                    <p className="text-sm text-destructive">{actionError}</p>
                  ) : null}
                </CardContent>
              </Card>
            ) : canWrite ? (
              <Card data-tour="users-invite">
                <CardHeader>
                  <p className="text-xs text-muted-foreground">Convidar usuário</p>
                  <CardTitle>Enviar convite</CardTitle>
                </CardHeader>
                <CardContent>
                  <form className="space-y-4" onSubmit={(e) => void handleInvite(e)}>
                    <div className="space-y-2">
                      <Label htmlFor="inviteEmail">E-mail</Label>
                      <Input
                        id="inviteEmail"
                        onChange={(e) => setInviteForm((f) => ({ ...f, email: e.target.value }))}
                        placeholder="colleague@example.com"
                        type="email"
                        value={inviteForm.email}
                      />
                      {inviteFieldErrors["email"] ? (
                        <p className="text-xs text-destructive">{inviteFieldErrors["email"]}</p>
                      ) : null}
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="invitePhone">
                        Telefone <span className="text-xs text-muted-foreground">(opcional)</span>
                      </Label>
                      <Input
                        id="invitePhone"
                        onChange={(e) => setInviteForm((f) => ({ ...f, phone: e.target.value }))}
                        placeholder="Ex: +5511999990001"
                        type="text"
                        value={inviteForm.phone}
                      />
                      {inviteFieldErrors["phone"] ? (
                        <p className="text-xs text-destructive">{inviteFieldErrors["phone"]}</p>
                      ) : null}
                    </div>

                    <div className="space-y-2">
                      <Label className="flex items-center gap-1.5" htmlFor="inviteRole">
                        Cargo
                        <InfoTooltip text="Funcionário: registra pesagens e eventos · Veterinário: registra vacinas e medicamentos · Admin: acesso completo incluindo configurações e financeiro" />
                      </Label>
                      <select
                        aria-label="Cargo"
                        className="w-full h-9 rounded-md border border-input bg-background px-3 py-1 text-sm shadow-sm"
                        id="inviteRole"
                        onChange={(e) => setInviteForm((f) => ({ ...f, role: e.target.value }))}
                        value={inviteForm.role}
                      >
                        <option value="worker">Funcionário</option>
                        <option value="veterinarian">Veterinário</option>
                        <option value="admin">Admin</option>
                      </select>
                      {inviteFieldErrors["role"] ? (
                        <p className="text-xs text-destructive">{inviteFieldErrors["role"]}</p>
                      ) : null}
                    </div>

                    {inviteFormError != null ? <p className="text-sm text-destructive">{inviteFormError}</p> : null}
                    {inviteSuccess ? <p className="text-sm text-green-600">Convite enviado.</p> : null}

                    <BillingGatedButton className="w-full" disabled={inviteSubmitting} type="submit">
                      {inviteSubmitting ? "Enviando..." : "Enviar convite"}
                    </BillingGatedButton>
                  </form>
                </CardContent>
              </Card>
            ) : (
              <Card>
                <CardHeader>
                  <CardTitle>Selecione um usuário</CardTitle>
                </CardHeader>
                <CardContent>
                  <p className="text-sm text-muted-foreground">Clique em uma linha para ver os detalhes do usuário.</p>
                </CardContent>
              </Card>
            )}
          </aside>
        </div>
      <AppTour steps={USERS_TOUR_STEPS} run={runTour} onFinish={finishTour} />
    </div>
  );
}

function InfoCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="space-y-0.5">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="text-sm font-semibold">{value}</p>
    </div>
  );
}

function buildListPath(filters: { search: string; role: RoleFilter; active: ActiveFilter; offset: number }) {
  const params = new URLSearchParams({ limit: "20", offset: String(filters.offset) });
  if (filters.search.trim().length > 0) params.set("search", filters.search.trim());
  if (filters.role !== "all") params.set("role", filters.role);
  if (filters.active !== "all") params.set("active", filters.active);
  return `/api/v1/users?${params.toString()}`;
}

