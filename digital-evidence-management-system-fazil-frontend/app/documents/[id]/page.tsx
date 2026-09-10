"use client";

import { useState, useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import DashboardLayout from "@/components/layout/DashboardLayout";
import Card from "@/components/ui/Card";
import Button from "@/components/ui/Button";
import Badge from "@/components/ui/Badge";
import { ArrowLeft, Download, Lock, Shield, FileText, Clock, User } from "lucide-react";
import { api } from "@/lib/api";
import type { Document } from "@/lib/types";
import { formatFileSize, formatDateTime } from "@/lib/utils";

export default function DocumentDetailPage() {
  const params = useParams();
  const router = useRouter();
  const [doc, setDoc] = useState<Document | null>(null);

  useEffect(() => {
    if (params.id) {
      api.documents.getById(params.id as string).then((d) => {
        if (d) setDoc(d);
      });
    }
  }, [params.id]);

  if (!doc) {
    return (
      <DashboardLayout>
        <div className="flex items-center justify-center h-64">
          <div className="text-gray-500">Loading...</div>
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex items-center gap-4">
          <Button variant="ghost" onClick={() => router.back()}><ArrowLeft className="h-4 w-4 mr-2" />Back</Button>
          <div className="flex-1">
            <h1 className="text-2xl font-bold text-gray-900">{doc.title}</h1>
            <p className="text-gray-500 mt-1">{doc.type.toUpperCase()} &middot; {formatFileSize(doc.size)}</p>
          </div>
          <Button><Download className="h-4 w-4 mr-2" />Download</Button>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 space-y-6">
            <Card>
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Document Details</h3>
              <div className="grid grid-cols-2 gap-4">
                <div className="flex items-center gap-2"><FileText className="h-4 w-4 text-gray-400" /><span className="text-sm text-gray-500">Category:</span><span className="text-sm font-medium capitalize">{doc.category}</span></div>
                <div className="flex items-center gap-2"><User className="h-4 w-4 text-gray-400" /><span className="text-sm text-gray-500">Uploaded by:</span><span className="text-sm font-medium">{doc.uploadedBy}</span></div>
                <div className="flex items-center gap-2"><Clock className="h-4 w-4 text-gray-400" /><span className="text-sm text-gray-500">Date:</span><span className="text-sm font-medium">{formatDateTime(doc.uploadedAt)}</span></div>
                <div className="flex items-center gap-2"><Shield className="h-4 w-4 text-gray-400" /><span className="text-sm text-gray-500">Classification:</span><Badge variant={doc.classification === "confidential" ? "danger" : doc.classification === "restricted" ? "warning" : "info"}>{doc.classification}</Badge></div>
              </div>
            </Card>

            <Card>
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Integrity</h3>
              <div className="bg-gray-50 rounded-lg p-4">
                <div className="text-sm text-gray-500 mb-1">SHA-256 Hash</div>
                <div className="font-mono text-sm bg-white p-3 rounded border border-gray-200">{doc.hash}</div>
              </div>
              <div className="mt-4 flex items-center gap-2">
                <Lock className="h-4 w-4 text-emerald-500" />
                <span className="text-sm font-medium text-emerald-700">Encrypted with AES-256</span>
              </div>
            </Card>
          </div>

          <div>
            <Card>
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Tags</h3>
              <div className="flex flex-wrap gap-2">
                {doc.tags.map((tag) => (
                  <Badge key={tag} variant="info">{tag}</Badge>
                ))}
              </div>
            </Card>

            <Card className="mt-6">
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Status</h3>
              <Badge variant={doc.status === "active" ? "success" : doc.status === "pending" ? "warning" : "default"} className="text-sm">
                {doc.status}
              </Badge>
              {doc.caseId && (
                <div className="mt-4">
                  <div className="text-sm text-gray-500">Associated Case</div>
                  <div className="text-sm font-medium text-primary-600 mt-1">Case {doc.caseId.toUpperCase()}</div>
                </div>
              )}
            </Card>
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
}
