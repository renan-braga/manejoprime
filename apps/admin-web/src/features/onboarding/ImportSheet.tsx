import { useCallback, useRef, useState } from "react";
import { Upload, Download, ChevronDown, ChevronUp, AlertTriangle, CheckCircle2, Loader2 } from "lucide-react";
import { Button } from "../../components/ui/button";
import { useAuth } from "../auth/AuthContext";
import { describeApiError } from "../../lib/apiErrors";

export type ImportDataType = "lots" | "animals" | "vaccines" | "feedings";

type ParsedRow = Record<string, string>;

type ValidationError = { row: number; message: string };

type ImportResult = { imported: number; skipped: number; errors: ValidationError[] };

type Props = {
  dataType: ImportDataType;
  hasLots?: boolean;
  hasAnimals?: boolean;
  onImported: () => void;
  onNext?: () => void;
};

// ─── column specs ────────────────────────────────────────────────────────────

const COLUMN_SPECS: Record<ImportDataType, { col: string; required: boolean; values?: string; example: string }[]> = {
  lots: [
    { col: "nome", required: true, example: "Piquete Norte" },
    { col: "finalidade", required: true, values: "engorda, recria, cria, reproducao", example: "engorda" },
    { col: "area_pasto", required: false, example: "5.2" },
  ],
  animals: [
    { col: "brinco", required: true, example: "001" },
    { col: "nome", required: false, example: "Touro 1" },
    { col: "raca", required: false, example: "Nelore" },
    { col: "sexo", required: true, values: "macho, femea", example: "macho" },
    { col: "data_nascimento", required: false, values: "AAAA-MM-DD", example: "2020-01-15" },
    { col: "peso_nascimento_kg", required: false, example: "32.5" },
    { col: "nome_lote", required: false, example: "Piquete Norte" },
    { col: "data_entrada", required: false, values: "AAAA-MM-DD", example: "2021-03-01" },
    { col: "status", required: false, values: "ativo, vendido, morto, transferido", example: "ativo" },
  ],
  vaccines: [
    { col: "brinco_animal", required: true, example: "001" },
    { col: "tipo_vacina", required: true, example: "Aftosa" },
    { col: "lote_vacina", required: false, example: "BATCH2024-A" },
    { col: "dose", required: true, values: "D1, D2, reforco, anual", example: "D1" },
    { col: "data_aplicacao", required: true, values: "AAAA-MM-DD", example: "2024-01-15" },
    { col: "proxima_dose", required: false, values: "AAAA-MM-DD", example: "2024-07-15" },
    { col: "volume_ml", required: false, example: "5" },
  ],
  feedings: [
    { col: "brinco_animal", required: false, example: "001" },
    { col: "nome_lote", required: false, example: "Piquete Norte" },
    { col: "tipo_alimento", required: true, values: "racao, silagem, suplemento, sal_mineral, pasto, misto", example: "racao" },
    { col: "descricao", required: false, example: "Ração concentrada 20%" },
    { col: "quantidade_kg", required: true, example: "5.5" },
    { col: "custo_por_kg", required: false, example: "2.80" },
    { col: "data_inicio", required: true, values: "AAAA-MM-DD", example: "2024-01-15" },
    { col: "data_fim", required: false, values: "AAAA-MM-DD", example: "2024-03-01" },
  ],
};

// ─── template CSV generation ──────────────────────────────────────────────────

const CSV_TEMPLATES: Record<ImportDataType, string> = {
  lots: "nome,finalidade,area_pasto\nPiquete Norte,engorda,5.2\nMaternidade,reproducao,2.0\n",
  animals:
    "brinco,nome,raca,sexo,data_nascimento,peso_nascimento_kg,nome_lote,data_entrada,status\n001,Touro 1,Nelore,macho,2020-01-15,32.5,Piquete Norte,2021-03-01,ativo\n002,Vaca 2,Angus,femea,2019-06-10,,Maternidade,2020-01-01,ativo\n",
  vaccines:
    "brinco_animal,tipo_vacina,lote_vacina,dose,data_aplicacao,proxima_dose,volume_ml\n001,Aftosa,BATCH2024-A,D1,2024-01-15,2024-07-15,5\n002,Brucelose,BATCH2024-B,D1,2024-01-20,,2\n",
  feedings:
    "brinco_animal,nome_lote,tipo_alimento,descricao,quantidade_kg,custo_por_kg,data_inicio,data_fim\n001,,racao,Ração concentrada 20%,5.5,2.80,2024-01-15,\n,Piquete Norte,silagem,,120.0,,2024-01-15,2024-03-01\n",
};

const LABELS: Record<ImportDataType, string> = {
  lots: "lotes",
  animals: "animais",
  vaccines: "vacinas",
  feedings: "alimentações",
};

// ─── column normalisation (flexible matching) ─────────────────────────────────

function normalizeHeader(h: string): string {
  return h
    .toLowerCase()
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .replace(/\s+/g, "_")
    .trim();
}

// Maps normalised header → canonical column name
const HEADER_ALIASES: Record<ImportDataType, Record<string, string>> = {
  lots: {
    nome: "nome",
    name: "nome",
    finalidade: "finalidade",
    purpose: "finalidade",
    area_pasto: "area_pasto",
    area_pasto_ha: "area_pasto",
    pasture_area: "area_pasto",
  },
  animals: {
    brinco: "brinco",
    identificacao: "brinco",
    identifier: "brinco",
    nome: "nome",
    name: "nome",
    raca: "raca",
    breed: "raca",
    sexo: "sexo",
    sex: "sexo",
    data_nascimento: "data_nascimento",
    birth_date: "data_nascimento",
    peso_nascimento_kg: "peso_nascimento_kg",
    birth_weight: "peso_nascimento_kg",
    nome_lote: "nome_lote",
    lot_name: "nome_lote",
    data_entrada: "data_entrada",
    entry_date: "data_entrada",
    status: "status",
  },
  vaccines: {
    brinco_animal: "brinco_animal",
    animal_identifier: "brinco_animal",
    tipo_vacina: "tipo_vacina",
    vaccine_type: "tipo_vacina",
    lote_vacina: "lote_vacina",
    vaccine_batch: "lote_vacina",
    dose: "dose",
    data_aplicacao: "data_aplicacao",
    application_date: "data_aplicacao",
    proxima_dose: "proxima_dose",
    next_dose: "proxima_dose",
    volume_ml: "volume_ml",
  },
  feedings: {
    brinco_animal: "brinco_animal",
    animal_identifier: "brinco_animal",
    nome_lote: "nome_lote",
    lot_name: "nome_lote",
    tipo_alimento: "tipo_alimento",
    feed_type: "tipo_alimento",
    descricao: "descricao",
    description: "descricao",
    quantidade_kg: "quantidade_kg",
    quantity_kg: "quantidade_kg",
    custo_por_kg: "custo_por_kg",
    cost_per_kg: "custo_por_kg",
    data_inicio: "data_inicio",
    start_date: "data_inicio",
    data_fim: "data_fim",
    end_date: "data_fim",
  },
};

function sanitizeFormula(value: string): string {
  // Prevent CSV injection / formula injection
  if (/^[=+\-@\t\r]/.test(value)) return "'" + value;
  return value;
}

function normalizeRow(raw: ParsedRow, dataType: ImportDataType): Record<string, string> {
  const aliases = HEADER_ALIASES[dataType];
  const out: Record<string, string> = {};
  for (const [key, val] of Object.entries(raw)) {
    const canonical = aliases[normalizeHeader(key)];
    if (canonical) out[canonical] = sanitizeFormula(val ?? "");
  }
  return out;
}

// ─── row → API payload row ────────────────────────────────────────────────────

function toApiRow(row: Record<string, string>, dataType: ImportDataType): Record<string, string> {
  switch (dataType) {
    case "lots":
      return { name: row.nome ?? "", purpose: row.finalidade ?? "", pastureArea: row.area_pasto ?? "" };
    case "animals":
      return {
        brinco: row.brinco ?? "",
        nome: row.nome ?? "",
        raca: row.raca ?? "",
        sexo: row.sexo ?? "",
        dataNascimento: row.data_nascimento ?? "",
        pesoNascimentoKg: row.peso_nascimento_kg ?? "",
        nomeLote: row.nome_lote ?? "",
        dataEntrada: row.data_entrada ?? "",
        status: row.status ?? "",
      };
    case "vaccines":
      return {
        brincoAnimal: row.brinco_animal ?? "",
        tipoVacina: row.tipo_vacina ?? "",
        loteVacina: row.lote_vacina ?? "",
        dose: row.dose ?? "",
        dataAplicacao: row.data_aplicacao ?? "",
        proximaDose: row.proxima_dose ?? "",
        volumeMl: row.volume_ml ?? "",
      };
    case "feedings":
      return {
        brincoAnimal: row.brinco_animal ?? "",
        nomeLote: row.nome_lote ?? "",
        tipoAlimento: row.tipo_alimento ?? "",
        descricao: row.descricao ?? "",
        quantidadeKg: row.quantidade_kg ?? "",
        custoPorKg: row.custo_por_kg ?? "",
        dataInicio: row.data_inicio ?? "",
        dataFim: row.data_fim ?? "",
      };
  }
}

// ─── component ────────────────────────────────────────────────────────────────

type Phase = "idle" | "parsing" | "preview" | "importing" | "success" | "error";

export function ImportSheet({ dataType, hasLots, hasAnimals, onImported, onNext }: Props) {
  const { fetchProtectedJson } = useAuth();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [phase, setPhase] = useState<Phase>("idle");
  const [fileName, setFileName] = useState("");
  const [rows, setRows] = useState<Record<string, string>[]>([]);
  const [validationErrors, setValidationErrors] = useState<ValidationError[]>([]);
  const [result, setResult] = useState<ImportResult | null>(null);
  const [errorMessage, setErrorMessage] = useState("");
  const [isDragging, setIsDragging] = useState(false);
  const [schemaOpen, setSchemaOpen] = useState(false);

  const label = LABELS[dataType];
  const columns = COLUMN_SPECS[dataType];

  // ── dependency warning ──────────────────────────────────────────────────────
  if (dataType === "animals" && hasLots === false) {
    return (
      <div className="rounded-lg border border-amber-200 bg-amber-50 dark:bg-amber-950/20 p-4 text-sm text-amber-800 dark:text-amber-300">
        <AlertTriangle className="inline w-4 h-4 mr-1.5" />
        Importe os piquetes/lotes primeiro para poder associar animais a eles.
      </div>
    );
  }
  if (dataType === "vaccines" && hasAnimals === false) {
    return (
      <div className="rounded-lg border border-amber-200 bg-amber-50 dark:bg-amber-950/20 p-4 text-sm text-amber-800 dark:text-amber-300">
        <AlertTriangle className="inline w-4 h-4 mr-1.5" />
        Importe os animais primeiro para poder registrar vacinas.
      </div>
    );
  }

  // ── file processing ─────────────────────────────────────────────────────────
  async function processFile(file: File) {
    const MAX_SIZE = 5 * 1024 * 1024;
    if (file.size > MAX_SIZE) {
      setPhase("error");
      setErrorMessage("Arquivo muito grande. Limite: 5 MB.");
      return;
    }
    const ext = file.name.split(".").pop()?.toLowerCase();
    if (!["csv", "xlsx"].includes(ext ?? "")) {
      setPhase("error");
      setErrorMessage("Formato não suportado. Use .csv ou .xlsx.");
      return;
    }

    setFileName(file.name);
    setPhase("parsing");

    if (ext === "csv") {
      const { default: Papa } = await import("papaparse");
      Papa.parse<ParsedRow>(file, {
        header: true,
        skipEmptyLines: true,
        complete: (result) => handleParsed(result.data),
        error: () => {
          setPhase("error");
          setErrorMessage("Não foi possível ler o arquivo CSV.");
        },
      });
    } else {
      const XLSX = await import("xlsx");
      const reader = new FileReader();
      reader.onload = (e) => {
        try {
          const data = new Uint8Array(e.target!.result as ArrayBuffer);
          const workbook = XLSX.read(data, {
            type: "array",
            cellFormula: false,
            cellHTML: false,
            WTF: false,
          });
          const sheet = workbook.Sheets[workbook.SheetNames[0]];
          const parsed = XLSX.utils.sheet_to_json<ParsedRow>(sheet, { defval: "" });
          handleParsed(parsed);
        } catch {
          setPhase("error");
          setErrorMessage("Não foi possível ler o arquivo Excel.");
        }
      };
      reader.readAsArrayBuffer(file);
    }
  }

  function handleParsed(rawRows: ParsedRow[]) {
    if (rawRows.length > 10_000) {
      setPhase("error");
      setErrorMessage("Planilha com mais de 10.000 linhas. Divida o arquivo e tente novamente.");
      return;
    }
    const normalized = rawRows.map((r) => normalizeRow(r, dataType));
    setRows(normalized);
    setValidationErrors([]);
    setPhase("preview");
  }

  // ── drag-drop ───────────────────────────────────────────────────────────────
  const onDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    const file = e.dataTransfer.files[0];
    if (file) void processFile(file);
  }, [dataType]);

  const onFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) void processFile(file);
    e.target.value = "";
  };

  // ── import ──────────────────────────────────────────────────────────────────
  async function runImport() {
    setPhase("importing");
    const apiRows = rows.map((r) => toApiRow(r, dataType));
    const endpoint =
      dataType === "lots"
        ? "/api/v1/lots/import"
        : dataType === "animals"
        ? "/api/v1/animals/import"
        : dataType === "feedings"
        ? "/api/v1/feedings/import"
        : "/api/v1/vaccines/import";

    try {
      const res = await fetchProtectedJson<ImportResult>(endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ rows: apiRows }),
      });
      setResult(res);
      setValidationErrors(res.errors);
      if (res.imported > 0) {
        setPhase("success");
        onImported();
      } else {
        setPhase("preview");
      }
    } catch (err) {
      setPhase("error");
      setErrorMessage(describeApiError(err));
    }
  }

  // ── download error file ─────────────────────────────────────────────────────
  async function downloadErrors() {
    const { default: Papa } = await import("papaparse");
    const errorRowNumbers = new Set(validationErrors.map((e) => e.row));
    const withError = rows
      .filter((_, i) => errorRowNumbers.has(i + 1))
      .map((row, i) => ({
        ...row,
        erro: validationErrors.find((e) => e.row === i + 1)?.message ?? "",
      }));
    const csv = Papa.unparse(withError);
    const blob = new Blob([csv], { type: "text/csv" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `erros_${dataType}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }

  function downloadTemplate() {
    const csv = CSV_TEMPLATES[dataType];
    const blob = new Blob([csv], { type: "text/csv" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${dataType}_modelo.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }

  function reset() {
    setPhase("idle");
    setRows([]);
    setValidationErrors([]);
    setResult(null);
    setErrorMessage("");
    setFileName("");
  }

  const errorRowNumbers = new Set(validationErrors.map((e) => e.row));
  const validCount = rows.length - errorRowNumbers.size;
  const cols = columns.map((c) => c.col);

  return (
    <div className="space-y-4">
      {/* Download template */}
      <div className="rounded-lg border border-emerald-200 bg-emerald-50 dark:bg-emerald-950/20 dark:border-emerald-800 p-3 flex items-start gap-3">
        <Download className="w-5 h-5 text-emerald-600 shrink-0 mt-0.5" />
        <div className="flex-1 min-w-0">
          <button
            onClick={downloadTemplate}
            className="font-medium text-emerald-700 dark:text-emerald-400 hover:underline text-sm"
            type="button"
          >
            Baixar planilha modelo (CSV)
          </button>
          <p className="text-xs text-emerald-600 dark:text-emerald-500 mt-0.5">
            Preencha com os dados da sua fazenda e envie aqui.
          </p>
        </div>
      </div>

      {/* Schema accordion */}
      <div className="border rounded-lg overflow-hidden">
        <button
          className="w-full flex items-center justify-between px-4 py-2.5 text-sm font-medium bg-muted/50 hover:bg-muted transition-colors"
          onClick={() => setSchemaOpen((v) => !v)}
          type="button"
        >
          Ver colunas esperadas
          {schemaOpen ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
        </button>
        {schemaOpen && (
          <div className="overflow-x-auto">
            <table className="w-full text-xs">
              <thead className="bg-muted/30">
                <tr>
                  <th className="text-left px-3 py-2 font-medium">Coluna</th>
                  <th className="text-left px-3 py-2 font-medium">Obrigatório</th>
                  <th className="text-left px-3 py-2 font-medium">Valores aceitos</th>
                  <th className="text-left px-3 py-2 font-medium">Exemplo</th>
                </tr>
              </thead>
              <tbody>
                {columns.map((c) => (
                  <tr key={c.col} className="border-t">
                    <td className="px-3 py-2 font-mono">{c.col}</td>
                    <td className="px-3 py-2">{c.required ? "Sim" : "Não"}</td>
                    <td className="px-3 py-2 text-muted-foreground">{c.values ?? "Texto livre"}</td>
                    <td className="px-3 py-2 text-muted-foreground">{c.example}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Drop zone (always shown unless in success state) */}
      {phase !== "success" && (
        <div
          className={`rounded-lg border-2 border-dashed transition-colors p-8 text-center cursor-pointer ${
            isDragging
              ? "border-primary bg-primary/5"
              : "border-muted-foreground/30 hover:border-muted-foreground/50"
          }`}
          onDragOver={(e) => { e.preventDefault(); setIsDragging(true); }}
          onDragLeave={() => setIsDragging(false)}
          onDrop={onDrop}
          onClick={() => fileInputRef.current?.click()}
        >
          <input
            ref={fileInputRef}
            type="file"
            accept=".csv,.xlsx"
            className="hidden"
            onChange={onFileChange}
          />
          {phase === "parsing" ? (
            <div className="flex flex-col items-center gap-2 text-muted-foreground">
              <Loader2 className="w-8 h-8 animate-spin" />
              <span className="text-sm">Lendo {fileName}...</span>
            </div>
          ) : (
            <div className="flex flex-col items-center gap-3 text-muted-foreground">
              <Upload className="w-8 h-8" />
              <div>
                <p className="text-sm font-medium">Arraste a planilha aqui</p>
                <p className="text-xs mt-0.5">ou clique para selecionar</p>
              </div>
              <p className="text-xs">Aceita: .csv · .xlsx — máx. 5 MB</p>
            </div>
          )}
        </div>
      )}

      {/* Error phase */}
      {phase === "error" && (
        <div className="rounded-lg border border-red-200 bg-red-50 dark:bg-red-950/20 p-4 space-y-3">
          <p className="text-sm font-medium text-red-700 dark:text-red-400">{errorMessage}</p>
          <Button variant="outline" size="sm" onClick={reset} type="button">
            Tentar novamente
          </Button>
        </div>
      )}

      {/* Preview phase */}
      {(phase === "preview" || phase === "importing") && rows.length > 0 && (
        <div className="space-y-3">
          <div className="flex items-center justify-between text-sm">
            <span>
              <span className="font-medium text-green-700 dark:text-green-400">{validCount} linhas válidas</span>
              {errorRowNumbers.size > 0 && (
                <span className="text-red-600 dark:text-red-400"> · {errorRowNumbers.size} com problema</span>
              )}
            </span>
            <button
              onClick={reset}
              className="text-xs text-muted-foreground hover:text-foreground underline"
              type="button"
            >
              Trocar arquivo
            </button>
          </div>

          {/* Preview table */}
          <div className="overflow-auto max-h-64 rounded-lg border text-xs">
            <table className="w-full">
              <thead className="sticky top-0 bg-muted/80 backdrop-blur-sm">
                <tr>
                  <th className="px-2 py-1.5 text-left font-medium text-muted-foreground w-8">#</th>
                  {cols.map((c) => (
                    <th key={c} className="px-2 py-1.5 text-left font-medium text-muted-foreground whitespace-nowrap">
                      {c}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {rows.slice(0, 10).map((row, i) => {
                  const rowNum = i + 1;
                  const err = validationErrors.find((e) => e.row === rowNum);
                  return (
                    <tr key={i} className={err ? "bg-red-50 dark:bg-red-950/20" : ""}>
                      <td className="px-2 py-1.5 text-muted-foreground">{rowNum}</td>
                      {cols.map((c) => (
                        <td key={c} className={`px-2 py-1.5 ${err ? "text-red-700 dark:text-red-400" : ""}`}>
                          {row[c] ?? ""}
                        </td>
                      ))}
                    </tr>
                  );
                })}
              </tbody>
            </table>
            {rows.length > 10 && (
              <p className="text-center text-xs text-muted-foreground py-2 border-t">
                Exibindo 10 de {rows.length} linhas
              </p>
            )}
          </div>

          {/* Validation error details */}
          {validationErrors.length > 0 && (
            <div className="rounded-lg border border-amber-200 bg-amber-50 dark:bg-amber-950/20 p-3 space-y-2">
              <p className="text-sm text-amber-800 dark:text-amber-300 flex items-center gap-1.5">
                <AlertTriangle className="w-4 h-4 shrink-0" />
                {validationErrors.length} {validationErrors.length === 1 ? "linha não será importada" : "linhas não serão importadas"} por conter erros.
              </p>
              <ul className="space-y-1">
                {validationErrors.slice(0, 5).map((e, i) => (
                  <li key={i} className="text-xs text-amber-700 dark:text-amber-400">
                    Linha {e.row}: {e.message}
                  </li>
                ))}
                {validationErrors.length > 5 && (
                  <li className="text-xs text-amber-600">… e mais {validationErrors.length - 5} erros</li>
                )}
              </ul>
              <Button variant="outline" size="sm" onClick={() => void downloadErrors()} type="button">
                Baixar planilha de erros
              </Button>
            </div>
          )}

          {/* Import button */}
          {result == null || validCount > 0 ? (
            <Button
              onClick={runImport}
              disabled={phase === "importing" || validCount === 0}
              className="w-full"
              type="button"
            >
              {phase === "importing" ? (
                <><Loader2 className="w-4 h-4 mr-2 animate-spin" /> Importando {validCount} {label}...</>
              ) : (
                `Importar ${validCount > 0 && errorRowNumbers.size > 0 ? `apenas as ${validCount} válidas` : validCount > 0 ? `${validCount} ${label}` : "arquivo"}`
              )}
            </Button>
          ) : null}
        </div>
      )}

      {/* Success phase */}
      {phase === "success" && result != null && (
        <div className="rounded-lg border border-green-200 bg-green-50 dark:bg-green-950/20 p-4 space-y-3">
          <p className="text-sm font-medium text-green-700 dark:text-green-400 flex items-center gap-2">
            <CheckCircle2 className="w-5 h-5" />
            {result.imported} {label} importados com sucesso!
            {result.skipped > 0 && ` (${result.skipped} já existiam e foram ignorados)`}
          </p>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" onClick={reset} type="button">
              Importar outro arquivo
            </Button>
            {onNext && (
              <Button size="sm" onClick={onNext} type="button">
                Próxima etapa
              </Button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
