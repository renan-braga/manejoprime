export function capitalize(s: string): string {
  return s.charAt(0).toUpperCase() + s.slice(1);
}

export function formatAnimalStatus(status: string): string {
  switch (status) {
    case "active": return "Ativo";
    case "sold": return "Vendido";
    case "dead": return "Morto";
    case "transferred": return "Transferido";
    default: return status;
  }
}

export function formatLotStatus(status: string): string {
  switch (status) {
    case "active": return "Ativo";
    case "archived": return "Arquivado";
    default: return status;
  }
}

export function formatRole(role: string): string {
  switch (role) {
    case "admin": return "Admin";
    case "veterinarian": return "Veterinário";
    case "worker": return "Funcionário";
    default: return role;
  }
}

export function formatSex(sex: string): string {
  switch (sex) {
    case "male": return "Macho";
    case "female": return "Fêmea";
    default: return sex;
  }
}

export function formatSexWithSymbol(sex: string): string {
  if (sex === "male") return "♂ Macho";
  if (sex === "female") return "♀ Fêmea";
  return sex;
}

export function formatOccurrenceStatus(value: string): string {
  switch (value) {
    case "open": return "Aberto";
    case "in_follow_up": return "Em acompanhamento";
    case "resolved": return "Resolvido";
    default: return capitalize(value);
  }
}

export function formatOccurrenceSource(value: string): string {
  return value === "whatsapp" ? "Entrada WhatsApp" : "Entrada app";
}
