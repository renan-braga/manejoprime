import React from "react";

type ChartProps = { children?: React.ReactNode; data?: unknown[] };

const chart =
  (testId: string) =>
  ({ children, data }: ChartProps) =>
    React.createElement("div", { "data-testid": testId, "data-point-count": data?.length ?? 0 }, children);

const div =
  (testId: string) =>
  ({ children }: { children?: React.ReactNode }) =>
    React.createElement("div", { "data-testid": testId }, children);

export const ResponsiveContainer = div("recharts-responsive-container");
export const LineChart = chart("recharts-line-chart");
export const BarChart = chart("recharts-bar-chart");
export const AreaChart = chart("recharts-area-chart");
export const PieChart = chart("recharts-pie-chart");
export const ComposedChart = chart("recharts-composed-chart");
export const RadarChart = chart("recharts-radar-chart");
export const RadialBarChart = chart("recharts-radial-bar-chart");
export const ScatterChart = chart("recharts-scatter-chart");
export const FunnelChart = chart("recharts-funnel-chart");
export const Treemap = div("recharts-treemap");

export const Line = () => null;
export const Bar = () => null;
export const Area = () => null;
export const Pie = () => null;
export const Cell = () => null;
export const Radar = () => null;
export const RadialBar = () => null;
export const Scatter = () => null;
export const Funnel = () => null;

export const XAxis = () => null;
export const YAxis = () => null;
export const ZAxis = () => null;
export const CartesianGrid = () => null;
export const PolarGrid = () => null;
export const PolarAngleAxis = () => null;
export const PolarRadiusAxis = () => null;

export const Tooltip = () => null;
export const Legend = () => null;
export const LegendProps = {};
export const Label = () => null;
export const LabelList = () => null;
export const ReferenceLine = () => null;
export const ReferenceArea = () => null;
export const ReferenceDot = () => null;
export const Brush = () => null;
export const ErrorBar = () => null;
