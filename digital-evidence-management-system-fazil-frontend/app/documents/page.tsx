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
import { FileText, Upload, Download, Search, Eye, Trash2, Lock } from "lucide-react";
import { api } from "@/lib/api";
import type { Document } from "@/lib/types";
import { formatFileSize, formatDateTime } from "@/lib/utils";

const typeFilters = [
  { value: "", label: "All Types" },
  { value: "pdf", label: "PDF" },
  { value: "docx", label: "DOCX" },
  { value: "xlsx", label: "XLSX" },
  { value: "image", label: "Images" },
  { value: "audio", label: "Audio" },
  { value: "video", label: "Video" },
];

const statusFilters = [
  { value: "", label: "All Status" },
  { value: "active", label: "Active" },
  { value: "pending", label: "Pending" },
  { value: "archived", label: "Archived" },
  { value: "restricted", label: "Restricted" },
];

const classificationColors: Record<string, "danger" | "warning" | "info" | "default"> = {
  confidential: "danger",
  restricted: "warning",
  internal: "info",
  public: "default",
};

export default function DocumentsPage() {
  const [documents, setDocuments] = useState<Document[]>([]);
  const [search, setSearch] = useState("");
  const [typeFilter, setTypeFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [uploadOpen, setUploadOpen] = useState(false);
  const [loading, setLoading] = useState(false);

  const loadDocuments = async () => {
    setLoading(true);
    const docs = await api.documents.list({ search, type: typeFilter, status: statusFilter });
    setDocuments(docs);
    setLoading(false);
  };

  useState(() => { loadDocuments(); });

  const columns = [
    { key: "title", header: "Document", render: (d: Document) => (
      <div className="flex items-center gap-3">
        <div className="p-2 bg-primary-50 rounded-lg"><FileText className="h-5 w-5 text-primary-600" /></div>
        <div>
          <div className="font-medium">{d.title}</div>
          <div className="text-xs text-gray-500">{d.type.toUpperCase()} &middot; {formatFileSize(d.size)}</div>
        </div>
      </div>
    )},
    { key: "category", header: "Category", render: (d: Document) => <span className="capitalize">{d.category}</span> },
    { key: "classification", header: "Classification", render: (d: Document) => <Badge variant={classificationColors[d.classification]}>{d.classification}</Badge> },
    { key: "status", header: "Status", render: (d: Document) => <Badge variant={d.status === "active" ? "success" : d.status === "pending" ? "warning" : "default"}>{d.status}</Badge> },
    { key: "encrypted", header: "Encrypted", render: (d: Document) => d.encrypted ? <Lock className="h-4 w-4 text-emerald-500" /> : <span className="text-gray-400">No</span> },
    { key: "uploadedBy", header: "Uploaded By" },
    { key: "uploadedAt", header: "Date", render: (d: Document) => formatDateTime(d.uploadedAt) },
    { key: "actions", header: "", render: () => (
      <div className="flex items-center gap-2">
        <button className="p-1.5 rounded-lg hover:bg-gray-100"><Eye className="h-4 w-4 text-gray-500" /></button>
        <button className="p-1.5 rounded-lg hover:bg-gray-100"><Download className="h-4 w-4 text-gray-500" /></button>
        <button className="p-1.5 rounded-lg hover:bg-gray-100"><Trash2 className="h-4 w-4 text-red-500" /></button>
      </div>
    )},
  ];

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Documents</h1>
            <p className="text-gray-500 mt-1">Manage encrypted documents and files</p>
          </div>
          <Button onClick={() => setUploadOpen(true)}><Upload className="h-4 w-4 mr-2" />Upload Document</Button>
        </div>

        <Card>
          <div className="flex flex-wrap items-center gap-4 mb-6">
            <div className="flex-1 min-w-[300px]">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                <input type="text" placeholder="Search documents..." value={search} onChange={(e) => setSearch(e.target.value)} className="pl-10 pr-4 py-2 w-full border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
              </div>
            </div>
            <Select options={typeFilters} value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)} />
            <Select options={statusFilters} value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} />
            <Button variant="primary" onClick={loadDocuments}>Search</Button>
          </div>

          <Table columns={columns} data={documents} keyExtractor={(d) => d.id} />
        </Card>

        <Modal isOpen={uploadOpen} onClose={() => setUploadOpen(false)} title="Upload Document" size="lg">
          <form className="space-y-4" onSubmit={(e) => { e.preventDefault(); setUploadOpen(false); }}>
            <Input label="Document Title" placeholder="Enter document title" required />
            <div className="grid grid-cols-2 gap-4">
              <Select label="Category" options={[
                { value: "evidence", label: "Evidence" },
                { value: "report", label: "Report" },
                { value: "correspondence", label: "Correspondence" },
                { value: "legal", label: "Legal" },
                { value: "internal", label: "Internal" },
              ]} />
              <Select label="Classification" options={[
                { value: "confidential", label: "Confidential" },
                { value: "restricted", label: "Restricted" },
                { value: "internal", label: "Internal" },
                { value: "public", label: "Public" },
              ]} />
            </div>
            <div className="border-2 border-dashed border-gray-300 rounded-xl p-8 text-center hover:border-primary-400 transition-colors cursor-pointer">
              <Upload className="h-10 w-10 text-gray-400 mx-auto mb-3" />
              <p className="text-sm text-gray-600">Click to upload or drag and drop</p>
              <p className="text-xs text-gray-400 mt-1">PDF, DOCX, XLSX, Images, Audio, Video (max 100MB)</p>
            </div>
            <div className="flex justify-end gap-3">
              <Button variant="secondary" type="button" onClick={() => setUploadOpen(false)}>Cancel</Button>
              <Button type="submit">Upload & Encrypt</Button>
            </div>
          </form>
        </Modal>
      </div>
    </DashboardLayout>
  );
}
