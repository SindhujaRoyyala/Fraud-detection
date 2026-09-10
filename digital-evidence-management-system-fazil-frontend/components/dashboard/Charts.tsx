"use client";

import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from "recharts";
import { documentActivityData, documentTypeData } from "@/lib/mock-data";

export function DocumentActivityChart() {
  return (
    <ResponsiveContainer width="100%" height={300}>
      <BarChart data={documentActivityData}>
        <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
        <XAxis dataKey="name" tick={{ fontSize: 12 }} stroke="#94a3b8" />
        <YAxis tick={{ fontSize: 12 }} stroke="#94a3b8" />
        <Tooltip />
        <Bar dataKey="uploads" fill="#3b82f6" radius={[4, 4, 0, 0]} name="Uploads" />
        <Bar dataKey="downloads" fill="#10b981" radius={[4, 4, 0, 0]} name="Downloads" />
        <Bar dataKey="views" fill="#f59e0b" radius={[4, 4, 0, 0]} name="Views" />
      </BarChart>
    </ResponsiveContainer>
  );
}

export function DocumentTypeChart() {
  return (
    <ResponsiveContainer width="100%" height={300}>
      <PieChart>
        <Pie data={documentTypeData} cx="50%" cy="50%" innerRadius={60} outerRadius={100} paddingAngle={4} dataKey="value" label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}>
          {documentTypeData.map((entry, index) => (
            <Cell key={index} fill={entry.color} />
          ))}
        </Pie>
        <Tooltip />
      </PieChart>
    </ResponsiveContainer>
  );
}
