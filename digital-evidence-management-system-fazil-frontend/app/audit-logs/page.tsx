"use client";

import { useState } from "react";
import DashboardLayout from "@/components/layout/DashboardLayout";
import Card from "@/components/ui/Card";
import Button from "@/components/ui/Button";
import Badge from "@/components/ui/Badge";
import Select from "@/components/ui/Select";
import Table from "@/components/ui/Table";
import { Search, CheckCircle, XCircle, AlertTriangle } from "lucide-react";
import { api } from "@/lib/api";
import type { AuditLog } from "@/lib/types";
import { formatDateTime } from "@/lib/utils";

const categoryFilters = [
  { value: "", label: "All Categories" },
  { value: "authentication", label: "Authentication" },
  { value: "document", label: "Document" },
  { value: "case", label: "Case" },
  { value: "user", label: "User" },
  { value: "system", label: "System" },
  { value: "security", label: "Security" },
];

const statusFilters = [
  { value: "", label: "All Status" },
  { value: "success", label: "Success" },
  { value: "failure", label: "Failure" },
  { value: "warning", label: "Warning" },
];

const statusIcons: Record<string, typeof CheckCircle> = {
  success: CheckCircle,
  failure: XCircle,
  warning: AlertTriangle,
};

const statusColors: Record<string, "success" | "danger" | "warning"> = {
  success: "success",
  failure: "danger",
  warning: "warning",
};

const categoryColors: Record<string, "info" | "success" | "warning" | "purple" | "default" | "danger"> = {
  authentication: "info",
  document: "success",
  case: "warning",
  user: "purple",
  system: "default",
  security: "danger",
};

export default function AuditLogsPage() {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [search, setSearch] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [loading, setLoading] = useState(false);

  const loadLogs = async () => {
    setLoading(true);
    const data = await api.audit.list({ search, category: categoryFilter, status: statusFilter });
    setLogs(data);
    setLoading(false);
  };

  useState(() => { loadLogs(); });

  const columns = [
    { key: "timestamp", header: "Time", render: (l: AuditLog) => <span className="text-xs text-gray-500">{formatDateTime(l.timestamp)}</span> },
    { key: "userName", header: "User", render: (l: AuditLog) => <span className="font-medium">{l.userName}</span> },
    { key: "action", header: "Action", render: (l: AuditLog) => (
      <div className="flex items-center gap-2">
        {(() => { const Icon = statusIcons[l.status]; return Icon ? <Icon className={`h-4 w-4 text-${statusColors[l.status] === "success" ? "emerald" : statusColors[l.status] === "danger" ? "red" : "amber"}-500`} /> : null; })()}
        <span>{l.action}</span>
      </div>
    )},
    { key: "category", header: "Category", render: (l: AuditLog) => <Badge variant={categoryColors[l.category]}>{l.category}</Badge> },
    { key: "resource", header: "Resource", render: (l: AuditLog) => <span className="text-gray-600">{l.resource}</span> },
    { key: "details", header: "Details", render: (l: AuditLog) => <span className="text-sm text-gray-600 max-w-xs truncate block">{l.details}</span> },
    { key: "ipAddress", header: "IP Address", render: (l: AuditLog) => <span className="font-mono text-xs">{l.ipAddress}</span> },
    { key: "status", header: "Status", render: (l: AuditLog) => <Badge variant={statusColors[l.status]}>{l.status}</Badge> },
  ];

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Audit Logs</h1>
          <p className="text-gray-500 mt-1">Track all system activities and security events</p>
        </div>

        <Card>
          <div className="flex flex-wrap items-center gap-4 mb-6">
            <div className="flex-1 min-w-[300px]">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                <input type="text" placeholder="Search audit logs..." value={search} onChange={(e) => setSearch(e.target.value)} className="pl-10 pr-4 py-2 w-full border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
              </div>
            </div>
            <Select options={categoryFilters} value={categoryFilter} onChange={(e) => setCategoryFilter(e.target.value)} />
            <Select options={statusFilters} value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} />
            <Button variant="primary" onClick={loadLogs}>Search</Button>
          </div>

          <Table columns={columns} data={logs} keyExtractor={(l) => l.id} />
        </Card>
      </div>
    </DashboardLayout>
  );
}
