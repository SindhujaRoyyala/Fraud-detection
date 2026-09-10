"use client";

import { useState } from "react";
import DashboardLayout from "@/components/layout/DashboardLayout";
import Card from "@/components/ui/Card";
import Button from "@/components/ui/Button";
import Badge from "@/components/ui/Badge";
import Input from "@/components/ui/Input";
import Select from "@/components/ui/Select";
import Modal from "@/components/ui/Modal";
import Table from "@/components/ui/Table";
import { FolderOpen, Plus, Search, Eye } from "lucide-react";
import { api } from "@/lib/api";
import type { Case } from "@/lib/types";
import { formatDateTime } from "@/lib/utils";

const statusFilters = [
  { value: "", label: "All Status" },
  { value: "open", label: "Open" },
  { value: "under_investigation", label: "Under Investigation" },
  { value: "pending_review", label: "Pending Review" },
  { value: "closed", label: "Closed" },
  { value: "archived", label: "Archived" },
];

const priorityFilters = [
  { value: "", label: "All Priority" },
  { value: "critical", label: "Critical" },
  { value: "high", label: "High" },
  { value: "medium", label: "Medium" },
  { value: "low", label: "Low" },
];

const priorityColors: Record<string, "danger" | "warning" | "info" | "default"> = {
  critical: "danger",
  high: "warning",
  medium: "info",
  low: "default",
};

const statusColors: Record<string, "success" | "warning" | "info" | "default" | "purple"> = {
  open: "info",
  under_investigation: "warning",
  pending_review: "purple",
  closed: "success",
  archived: "default",
};

const statusLabels: Record<string, string> = {
  open: "Open",
  under_investigation: "Under Investigation",
  pending_review: "Pending Review",
  closed: "Closed",
  archived: "Archived",
};

export default function CasesPage() {
  const [cases, setCases] = useState<Case[]>([]);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [priorityFilter, setPriorityFilter] = useState("");
  const [createOpen, setCreateOpen] = useState(false);
  const [loading, setLoading] = useState(false);

  const loadCases = async () => {
    setLoading(true);
    const data = await api.cases.list({ search, status: statusFilter, priority: priorityFilter });
    setCases(data);
    setLoading(false);
  };

  useState(() => { loadCases(); });

  const columns = [
    { key: "title", header: "Case", render: (c: Case) => (
      <div className="flex items-center gap-3">
        <div className="p-2 bg-amber-50 rounded-lg"><FolderOpen className="h-5 w-5 text-amber-600" /></div>
        <div>
          <div className="font-medium">{c.title}</div>
          <div className="text-xs text-gray-500 max-w-md truncate">{c.description}</div>
        </div>
      </div>
    )},
    { key: "status", header: "Status", render: (c: Case) => <Badge variant={statusColors[c.status]}>{statusLabels[c.status]}</Badge> },
    { key: "priority", header: "Priority", render: (c: Case) => <Badge variant={priorityColors[c.priority]}>{c.priority}</Badge> },
    { key: "assigneeName", header: "Assignee" },
    { key: "documentsCount", header: "Docs", render: (c: Case) => <span className="text-gray-600">{c.documentsCount}</span> },
    { key: "evidenceCount", header: "Evidence", render: (c: Case) => <span className="text-gray-600">{c.evidenceCount}</span> },
    { key: "updatedAt", header: "Updated", render: (c: Case) => formatDateTime(c.updatedAt) },
    { key: "actions", header: "", render: () => (
      <button className="p-1.5 rounded-lg hover:bg-gray-100"><Eye className="h-4 w-4 text-gray-500" /></button>
    )},
  ];

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Cases</h1>
            <p className="text-gray-500 mt-1">Track and manage investigation cases</p>
          </div>
          <Button onClick={() => setCreateOpen(true)}><Plus className="h-4 w-4 mr-2" />Create Case</Button>
        </div>

        <Card>
          <div className="flex flex-wrap items-center gap-4 mb-6">
            <div className="flex-1 min-w-[300px]">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                <input type="text" placeholder="Search cases..." value={search} onChange={(e) => setSearch(e.target.value)} className="pl-10 pr-4 py-2 w-full border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
              </div>
            </div>
            <Select options={statusFilters} value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} />
            <Select options={priorityFilters} value={priorityFilter} onChange={(e) => setPriorityFilter(e.target.value)} />
            <Button variant="primary" onClick={loadCases}>Search</Button>
          </div>

          <Table columns={columns} data={cases} keyExtractor={(c) => c.id} />
        </Card>

        <Modal isOpen={createOpen} onClose={() => setCreateOpen(false)} title="Create New Case" size="lg">
          <form className="space-y-4" onSubmit={(e) => { e.preventDefault(); setCreateOpen(false); }}>
            <Input label="Case Title" placeholder="Enter case title" required />
            <div className="space-y-1">
              <label className="block text-sm font-medium text-gray-700">Description</label>
              <textarea className="block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500" rows={3} placeholder="Describe the case..." />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <Select label="Priority" options={[
                { value: "critical", label: "Critical" },
                { value: "high", label: "High" },
                { value: "medium", label: "Medium" },
                { value: "low", label: "Low" },
              ]} />
              <Select label="Category" options={[
                { value: "Cybercrime", label: "Cybercrime" },
                { value: "Financial Crimes", label: "Financial Crimes" },
                { value: "General Investigation", label: "General Investigation" },
                { value: "Internal Affairs", label: "Internal Affairs" },
                { value: "National Security", label: "National Security" },
              ]} />
            </div>
            <Select label="Assignee" options={[
              { value: "u2", label: "James Rodriguez" },
              { value: "u3", label: "Emily Chen" },
              { value: "u4", label: "Michael Brown" },
              { value: "u6", label: "David Kim" },
              { value: "u8", label: "Tom Wilson" },
            ]} />
            <Input label="Tags" placeholder="Comma-separated tags" />
            <div className="flex justify-end gap-3">
              <Button variant="secondary" type="button" onClick={() => setCreateOpen(false)}>Cancel</Button>
              <Button type="submit">Create Case</Button>
            </div>
          </form>
        </Modal>
      </div>
    </DashboardLayout>
  );
}
