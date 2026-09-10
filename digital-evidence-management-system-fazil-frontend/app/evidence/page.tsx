"use client";

import { useState } from "react";
import DashboardLayout from "@/components/layout/DashboardLayout";
import Card from "@/components/ui/Card";
import Button from "@/components/ui/Button";
import Badge from "@/components/ui/Badge";
import Select from "@/components/ui/Select";
import Modal from "@/components/ui/Modal";
import Table from "@/components/ui/Table";
import { Shield, Search, Eye } from "lucide-react";
import { api } from "@/lib/api";
import type { Evidence } from "@/lib/types";
import { formatDateTime } from "@/lib/utils";

const statusFilters = [
  { value: "", label: "All Status" },
  { value: "stored", label: "Stored" },
  { value: "in_transit", label: "In Transit" },
  { value: "under_review", label: "Under Review" },
  { value: "returned", label: "Returned" },
  { value: "destroyed", label: "Destroyed" },
];

const typeFilters = [
  { value: "", label: "All Types" },
  { value: "digital", label: "Digital" },
  { value: "physical", label: "Physical" },
  { value: "testimonial", label: "Testimonial" },
  { value: "documentary", label: "Documentary" },
];

const statusColors: Record<string, "success" | "warning" | "info" | "default" | "danger"> = {
  stored: "success",
  in_transit: "warning",
  under_review: "info",
  returned: "default",
  destroyed: "danger",
};

export default function EvidencePage() {
  const [evidence, setEvidence] = useState<Evidence[]>([]);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [typeFilter, setTypeFilter] = useState("");
  const [detailOpen, setDetailOpen] = useState(false);
  const [selected, setSelected] = useState<Evidence | null>(null);
  const [loading, setLoading] = useState(false);

  const loadEvidence = async () => {
    setLoading(true);
    const data = await api.evidence.list({ search, status: statusFilter });
    setEvidence(data);
    setLoading(false);
  };

  useState(() => { loadEvidence(); });

  const columns = [
    { key: "title", header: "Evidence", render: (e: Evidence) => (
      <div className="flex items-center gap-3">
        <div className="p-2 bg-emerald-50 rounded-lg"><Shield className="h-5 w-5 text-emerald-600" /></div>
        <div>
          <div className="font-medium">{e.title}</div>
          <div className="text-xs text-gray-500">{e.type} &middot; {e.caseTitle}</div>
        </div>
      </div>
    )},
    { key: "status", header: "Status", render: (e: Evidence) => <Badge variant={statusColors[e.status]}>{e.status.replace("_", " ")}</Badge> },
    { key: "encrypted", header: "Encrypted", render: (e: Evidence) => e.encrypted ? <Badge variant="success">Yes</Badge> : <Badge variant="default">No</Badge> },
    { key: "collectedBy", header: "Collected By" },
    { key: "collectedAt", header: "Collected", render: (e: Evidence) => formatDateTime(e.collectedAt) },
    { key: "chainOfCustody", header: "Chain", render: (e: Evidence) => <span className="text-gray-600">{e.chainOfCustody.length} entries</span> },
    { key: "actions", header: "", render: (e: Evidence) => (
      <button onClick={(ev) => { ev.stopPropagation(); setSelected(e); setDetailOpen(true); }} className="p-1.5 rounded-lg hover:bg-gray-100"><Eye className="h-4 w-4 text-gray-500" /></button>
    )},
  ];

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Evidence</h1>
          <p className="text-gray-500 mt-1">Track evidence with chain of custody</p>
        </div>

        <Card>
          <div className="flex flex-wrap items-center gap-4 mb-6">
            <div className="flex-1 min-w-[300px]">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                <input type="text" placeholder="Search evidence..." value={search} onChange={(e) => setSearch(e.target.value)} className="pl-10 pr-4 py-2 w-full border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
              </div>
            </div>
            <Select options={statusFilters} value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} />
            <Select options={typeFilters} value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)} />
            <Button variant="primary" onClick={loadEvidence}>Search</Button>
          </div>

          <Table columns={columns} data={evidence} keyExtractor={(e) => e.id} />
        </Card>

        <Modal isOpen={detailOpen} onClose={() => setDetailOpen(false)} title="Evidence Details" size="lg">
          {selected && (
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div><span className="text-sm text-gray-500">Title</span><p className="font-medium">{selected.title}</p></div>
                <div><span className="text-sm text-gray-500">Type</span><p className="font-medium capitalize">{selected.type}</p></div>
                <div><span className="text-sm text-gray-500">Case</span><p className="font-medium">{selected.caseTitle}</p></div>
                <div><span className="text-sm text-gray-500">Status</span><p><Badge variant={statusColors[selected.status]}>{selected.status.replace("_", " ")}</Badge></p></div>
              </div>
              <div><span className="text-sm text-gray-500">Description</span><p className="text-sm mt-1">{selected.description}</p></div>
              {selected.hash && <div><span className="text-sm text-gray-500">Hash</span><p className="font-mono text-xs mt-1 bg-gray-100 p-2 rounded">{selected.hash}</p></div>}
              <div>
                <span className="text-sm text-gray-500">Chain of Custody</span>
                <div className="mt-2 space-y-2">
                  {selected.chainOfCustody.map((entry, i) => (
                    <div key={i} className="flex items-start gap-3 p-3 bg-gray-50 rounded-lg">
                      <div className="h-2 w-2 rounded-full bg-primary-500 mt-1.5" />
                      <div className="flex-1">
                        <div className="flex items-center gap-2"><span className="font-medium text-sm">{entry.action}</span><span className="text-xs text-gray-400">{formatDateTime(entry.at)}</span></div>
                        <div className="text-xs text-gray-500">by {entry.by}</div>
                        {entry.notes && <div className="text-xs text-gray-600 mt-1">{entry.notes}</div>}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}
        </Modal>
      </div>
    </DashboardLayout>
  );
}
