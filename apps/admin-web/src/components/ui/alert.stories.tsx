import type { Meta, StoryObj } from "@storybook/react";
import { AlertCircle, Info, Terminal } from "lucide-react";

import { Alert, AlertDescription, AlertTitle } from "./alert";

const meta = {
  title: "UI/Alert",
  component: Alert,
  tags: ["autodocs"],
  argTypes: {
    variant: {
      control: "select",
      options: ["default", "destructive"],
    },
  },
} satisfies Meta<typeof Alert>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  render: () => (
    <Alert>
      <Terminal className="size-4" />
      <AlertTitle>Atenção</AlertTitle>
      <AlertDescription>
        Esta ação não pode ser desfeita após a confirmação.
      </AlertDescription>
    </Alert>
  ),
};

export const Destructive: Story = {
  render: () => (
    <Alert variant="destructive">
      <AlertCircle className="size-4" />
      <AlertTitle>Erro</AlertTitle>
      <AlertDescription>
        Não foi possível salvar as alterações. Tente novamente.
      </AlertDescription>
    </Alert>
  ),
};

export const WithoutIcon: Story = {
  render: () => (
    <Alert>
      <AlertTitle>Sem ícone</AlertTitle>
      <AlertDescription>Alerta sem ícone lateral.</AlertDescription>
    </Alert>
  ),
};

export const InfoOnly: Story = {
  render: () => (
    <Alert>
      <Info className="size-4" />
      <AlertDescription>
        Seu período de teste termina em 3 dias.
      </AlertDescription>
    </Alert>
  ),
};
