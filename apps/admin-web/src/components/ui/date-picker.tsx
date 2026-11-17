import * as React from "react";
import { ptBR } from "date-fns/locale";
import { CalendarIcon } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "./button";
import { Calendar } from "./calendar";
import { Popover, PopoverContent, PopoverTrigger } from "./popover";

function parseLocalDate(str: string): Date | undefined {
  if (str.length !== 10) return undefined;
  const parts = str.split("-").map(Number);
  const [y, m, d] = parts;
  if (!y || !m || !d) return undefined;
  return new Date(y, m - 1, d);
}

function toIsoDate(date: Date): string {
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, "0"),
    String(date.getDate()).padStart(2, "0")
  ].join("-");
}

interface DatePickerInputProps {
  id?: string;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  className?: string;
}

export function DatePickerInput({ id, value, onChange, placeholder = "Selecionar data", className }: DatePickerInputProps) {
  const [open, setOpen] = React.useState(false);
  const selected = parseLocalDate(value);

  const displayLabel = selected
    ? selected.toLocaleDateString("pt-BR", { day: "2-digit", month: "long", year: "numeric" })
    : placeholder;

  return (
    <div className={cn("relative", className)}>
      {/* Accessible hidden input: connects <Label htmlFor={id}> to this component and allows fireEvent.change in tests */}
      <input
        className="sr-only"
        id={id}
        onChange={(e) => onChange(e.target.value)}
        tabIndex={-1}
        type="text"
        value={value}
      />
      <Popover onOpenChange={setOpen} open={open}>
        <PopoverTrigger asChild>
          <Button
            className={cn(
              "w-full justify-start text-left font-normal",
              !selected && "text-muted-foreground"
            )}
            type="button"
            variant="outline"
          >
            <CalendarIcon className="mr-2 h-4 w-4 shrink-0" />
            {displayLabel}
          </Button>
        </PopoverTrigger>
        <PopoverContent align="start" className="w-auto p-0">
          <Calendar
            captionLayout="dropdown"
            fromYear={1990}
            locale={ptBR}
            mode="single"
            onSelect={(date) => {
              onChange(date ? toIsoDate(date) : "");
              setOpen(false);
            }}
            selected={selected}
            toYear={new Date().getFullYear() + 2}
          />
        </PopoverContent>
      </Popover>
    </div>
  );
}
