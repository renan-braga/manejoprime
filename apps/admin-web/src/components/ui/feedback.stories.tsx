import type { Meta, StoryObj } from "@storybook/react";

import { Skeleton } from "./skeleton";
import { Spinner } from "./spinner";

const meta = {
  title: "UI/Feedback",
  component: Spinner,
  tags: ["autodocs"],
  parameters: {
    layout: "padded",
  },
} satisfies Meta<typeof Spinner>;

export default meta;
type Story = StoryObj<typeof meta>;

// ── Spinner ──────────────────────────────────────────────────────────────────

export const SpinnerDefault: Story = {
  name: "Spinner / Default",
  render: () => <Spinner />,
};

export const SpinnerSizes: Story = {
  name: "Spinner / Tamanhos",
  render: () => (
    <div className="flex items-center gap-4">
      <Spinner className="size-4" />
      <Spinner className="size-6" />
      <Spinner className="size-8" />
      <Spinner className="size-12" />
    </div>
  ),
};

export const SpinnerColors: Story = {
  name: "Spinner / Cores",
  render: () => (
    <div className="flex items-center gap-4">
      <Spinner className="text-primary" />
      <Spinner className="text-destructive" />
      <Spinner className="text-muted-foreground" />
    </div>
  ),
};

// ── Skeleton ─────────────────────────────────────────────────────────────────

export const SkeletonText: Story = {
  name: "Skeleton / Texto",
  render: () => (
    <div className="flex flex-col gap-2 w-[300px]">
      <Skeleton className="h-4 w-full" />
      <Skeleton className="h-4 w-4/5" />
      <Skeleton className="h-4 w-3/5" />
    </div>
  ),
};

export const SkeletonCard: Story = {
  name: "Skeleton / Card",
  render: () => (
    <div className="flex flex-col gap-4 w-[320px] border rounded-xl p-6">
      <div className="flex items-center gap-3">
        <Skeleton className="size-10 rounded-full" />
        <div className="flex flex-col gap-2 flex-1">
          <Skeleton className="h-4 w-1/2" />
          <Skeleton className="h-3 w-1/3" />
        </div>
      </div>
      <Skeleton className="h-24 w-full rounded-md" />
      <div className="flex gap-2">
        <Skeleton className="h-8 w-20 rounded-md" />
        <Skeleton className="h-8 w-20 rounded-md" />
      </div>
    </div>
  ),
};

export const SkeletonTable: Story = {
  name: "Skeleton / Tabela",
  render: () => (
    <div className="flex flex-col gap-2 w-full">
      {Array.from({ length: 5 }).map((_, i) => (
        <div key={i} className="flex gap-4">
          <Skeleton className="h-8 w-8 rounded" />
          <Skeleton className="h-8 flex-1" />
          <Skeleton className="h-8 w-24" />
          <Skeleton className="h-8 w-20" />
        </div>
      ))}
    </div>
  ),
};
