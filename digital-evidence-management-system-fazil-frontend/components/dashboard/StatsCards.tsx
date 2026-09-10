"use client";

import { FileText, FolderOpen, Users, AlertTriangle, Database, Clock } from "lucide-react";
import { mockDashboardStats } from "@/lib/mock-data";

const stats = [
  { label: "Total Documents", value: mockDashboardStats.totalDocuments.toLocaleString(), icon: FileText, color: "bg-blue-500", change: `+${mockDashboardStats.documentsThisWeek} this week` },
  { label: "Active Cases", value: mockDashboardStats.totalCases.toString(), icon: FolderOpen, color: "bg-amber-500", change: `+${mockDashboardStats.casesThisMonth} this month` },
  { label: "Active Users", value: mockDashboardStats.activeUsers.toString(), icon: Users, color: "bg-emerald-500", change: "All systems operational" },
  { label: "Pending Reviews", value: mockDashboardStats.pendingReviews.toString(), icon: Clock, color: "bg-purple-500", change: "Requires attention" },
  { label: "Security Alerts", value: mockDashboardStats.securityAlerts.toString(), icon: AlertTriangle, color: "bg-red-500", change: "Review recommended" },
  { label: "Storage Used", value: `${mockDashboardStats.storageUsed} GB`, icon: Database, color: "bg-cyan-500", change: "of 500 GB allocated" },
];

export default function StatsCards() {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
      {stats.map((stat) => (
        <div key={stat.label} className="bg-white rounded-xl border border-gray-200 p-4 shadow-sm">
          <div className="flex items-center gap-3 mb-3">
            <div className={`p-2 rounded-lg ${stat.color}`}>
              <stat.icon className="h-5 w-5 text-white" />
            </div>
          </div>
          <div className="text-2xl font-bold text-gray-900">{stat.value}</div>
          <div className="text-sm font-medium text-gray-600 mt-1">{stat.label}</div>
          <div className="text-xs text-gray-500 mt-1">{stat.change}</div>
        </div>
      ))}
    </div>
  );
}
