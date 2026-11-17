import type { Meta, StoryObj } from "@storybook/react";

import { Input } from "./input";

const meta = {
  title: "UI/Input",
  component: Input,
  tags: ["autodocs"],
  args: {
    placeholder: "Digite aqui…",
  },
  argTypes: {
    type: {
      control: "select",
      options: ["text", "email", "password", "number", "search", "tel"],
    },
    disabled: { control: "boolean" },
    "aria-invalid": { control: "boolean" },
  },
} satisfies Meta<typeof Input>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const WithValue: Story = {
  args: { defaultValue: "João da Silva" },
};

export const Email: Story = {
  args: { type: "email", placeholder: "email@exemplo.com" },
};

export const Password: Story = {
  args: { type: "password", placeholder: "Senha" },
};

export const Disabled: Story = {
  args: { disabled: true, defaultValue: "Campo desabilitado" },
};

export const Invalid: Story = {
  args: {
    "aria-invalid": true,
    defaultValue: "valor inválido",
  },
};

export const File: Story = {
  args: { type: "file" },
};
