import type { Meta, StoryObj } from "@storybook/react";
import { fn } from "@storybook/test";
import { Download, Loader2 } from "lucide-react";

import { Button } from "./button";

const meta = {
  title: "UI/Button",
  component: Button,
  tags: ["autodocs"],
  args: {
    onClick: fn(),
    children: "Button",
  },
  argTypes: {
    variant: {
      control: "select",
      options: ["default", "destructive", "outline", "secondary", "ghost", "link"],
    },
    size: {
      control: "select",
      options: ["default", "sm", "lg", "icon", "icon-sm", "icon-lg"],
    },
    disabled: { control: "boolean" },
    asChild: { table: { disable: true } },
  },
} satisfies Meta<typeof Button>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const Destructive: Story = {
  args: { variant: "destructive", children: "Excluir" },
};

export const Outline: Story = {
  args: { variant: "outline" },
};

export const Secondary: Story = {
  args: { variant: "secondary" },
};

export const Ghost: Story = {
  args: { variant: "ghost" },
};

export const Link: Story = {
  args: { variant: "link" },
};

export const Small: Story = {
  args: { size: "sm", children: "Pequeno" },
};

export const Large: Story = {
  args: { size: "lg", children: "Grande" },
};

export const IconButton: Story = {
  args: { size: "icon", children: <Download /> },
};

export const Loading: Story = {
  args: {
    disabled: true,
    children: (
      <>
        <Loader2 className="animate-spin" />
        Carregando…
      </>
    ),
  },
};

export const Disabled: Story = {
  args: { disabled: true, children: "Desabilitado" },
};

export const AllVariants: Story = {
  render: () => (
    <div className="flex flex-wrap gap-2">
      {(["default", "destructive", "outline", "secondary", "ghost", "link"] as const).map(
        (v) => (
          <Button key={v} variant={v}>
            {v}
          </Button>
        )
      )}
    </div>
  ),
};
