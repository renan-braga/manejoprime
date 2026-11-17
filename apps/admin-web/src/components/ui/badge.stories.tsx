import type { Meta, StoryObj } from "@storybook/react";

import { Badge } from "./badge";

const meta = {
  title: "UI/Badge",
  component: Badge,
  tags: ["autodocs"],
  args: {
    children: "Badge",
  },
  argTypes: {
    variant: {
      control: "select",
      options: ["default", "secondary", "destructive", "outline"],
    },
    asChild: { table: { disable: true } },
  },
} satisfies Meta<typeof Badge>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const Secondary: Story = {
  args: { variant: "secondary" },
};

export const Destructive: Story = {
  args: { variant: "destructive", children: "Erro" },
};

export const Outline: Story = {
  args: { variant: "outline" },
};

export const AllVariants: Story = {
  render: () => (
    <div className="flex flex-wrap gap-2">
      {(["default", "secondary", "destructive", "outline"] as const).map((v) => (
        <Badge key={v} variant={v}>
          {v}
        </Badge>
      ))}
    </div>
  ),
};

export const UseCases: Story = {
  render: () => (
    <div className="flex flex-wrap gap-2">
      <Badge variant="default">Ativo</Badge>
      <Badge variant="secondary">Pendente</Badge>
      <Badge variant="destructive">Inativo</Badge>
      <Badge variant="outline">Rascunho</Badge>
    </div>
  ),
};
