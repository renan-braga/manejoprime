import type { Meta, StoryObj } from "@storybook/react";

import { Tabs, TabsContent, TabsList, TabsTrigger } from "./tabs";

const meta = {
  title: "UI/Tabs",
  component: Tabs,
  tags: ["autodocs"],
  parameters: {
    layout: "padded",
  },
} satisfies Meta<typeof Tabs>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  render: () => (
    <Tabs defaultValue="geral" className="w-[400px]">
      <TabsList>
        <TabsTrigger value="geral">Geral</TabsTrigger>
        <TabsTrigger value="animais">Animais</TabsTrigger>
        <TabsTrigger value="lotes">Lotes</TabsTrigger>
      </TabsList>
      <TabsContent value="geral">
        <p className="text-sm text-muted-foreground pt-2">
          Informações gerais da fazenda.
        </p>
      </TabsContent>
      <TabsContent value="animais">
        <p className="text-sm text-muted-foreground pt-2">
          Lista de animais cadastrados.
        </p>
      </TabsContent>
      <TabsContent value="lotes">
        <p className="text-sm text-muted-foreground pt-2">
          Lotes e divisões de pastagem.
        </p>
      </TabsContent>
    </Tabs>
  ),
};

export const WithDisabled: Story = {
  render: () => (
    <Tabs defaultValue="tab1" className="w-[400px]">
      <TabsList>
        <TabsTrigger value="tab1">Ativo</TabsTrigger>
        <TabsTrigger value="tab2">Também ativo</TabsTrigger>
        <TabsTrigger value="tab3" disabled>
          Desabilitado
        </TabsTrigger>
      </TabsList>
      <TabsContent value="tab1">Conteúdo da tab 1.</TabsContent>
      <TabsContent value="tab2">Conteúdo da tab 2.</TabsContent>
      <TabsContent value="tab3">Nunca visto.</TabsContent>
    </Tabs>
  ),
};

export const ManyTabs: Story = {
  render: () => (
    <Tabs defaultValue="a" className="w-full">
      <TabsList>
        {["Janeiro", "Fevereiro", "Março", "Abril", "Maio"].map((month) => (
          <TabsTrigger key={month} value={month}>
            {month}
          </TabsTrigger>
        ))}
      </TabsList>
      {["Janeiro", "Fevereiro", "Março", "Abril", "Maio"].map((month) => (
        <TabsContent key={month} value={month}>
          <p className="text-sm text-muted-foreground pt-2">
            Dados de {month}.
          </p>
        </TabsContent>
      ))}
    </Tabs>
  ),
};
