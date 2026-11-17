import { Info } from "lucide-react";
import { Tooltip, TooltipContent, TooltipTrigger } from "./ui/tooltip";

export function InfoTooltip({ text }: { text: string }) {
  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <Info className="inline h-3.5 w-3.5 cursor-help text-muted-foreground" />
      </TooltipTrigger>
      <TooltipContent className="max-w-xs text-sm leading-snug">
        {text}
      </TooltipContent>
    </Tooltip>
  );
}
