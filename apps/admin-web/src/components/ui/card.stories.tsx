import type { Meta, StoryObj } from "@storybook/react";

import { Button } from "./button";
import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "./card";

const meta = {
  title: "UI/Card",
  component: Card,
  tags: ["autodocs"],
  parameters: {
    layout: "padded",
  },
} satisfies Meta<typeof Card>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  render: () => (
    <Card className="w-[380px]">
      <CardHeader>
        <CardTitle>Título do Card</CardTitle>
        <CardDescription>Descrição ou subtítulo opcional.</CardDescription>
      </CardHeader>
      <CardContent>
        <p className="text-sm text-muted-foreground">
          Conteúdo do card. Pode ser qualquer elemento React.
        </p>
      </CardContent>
      <CardFooter className="gap-2">
        <Button variant="outline" size="sm">Cancelar</Button>
        <Button size="sm">Confirmar</Button>
      </CardFooter>
    </Card>
  ),
};

export const WithAction: Story = {
  render: () => (
    <Card className="w-[380px]">
      <CardHeader>
        <CardTitle>Animais no pasto</CardTitle>
        <CardDescription>Lote A — Fazenda Central</CardDescription>
        <CardAction>
          <Button variant="outline" size="sm">Ver todos</Button>
        </CardAction>
      </CardHeader>
      <CardContent>
        <p className="text-3xl font-bold">142</p>
        <p className="text-sm text-muted-foreground mt-1">cabeças de gado</p>
      </CardContent>
    </Card>
  ),
};

export const Minimal: Story = {
  render: () => (
    <Card className="w-[320px]">
      <CardContent>
        <p className="text-sm">Card sem header nem footer.</p>
      </CardContent>
    </Card>
  ),
};
