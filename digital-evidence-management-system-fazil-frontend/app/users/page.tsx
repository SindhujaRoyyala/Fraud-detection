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
import { Search, Plus, Shield, UserX, UserCheck } from "lucide-react";
import { api } from "@/lib/api";
import type { User } from "@/lib/types";
import { formatDateTime } from "@/lib/utils";

const roleFilters = [
  { value: "", label: "All Roles" },
  { value: "admin", label: "Admin" },
  { value: "investigator", label: "Investigator" },
  { value: "analyst", label: "Analyst" },
  { value: "viewer", label: "Viewer" },
];

const statusFilters = [
  { value: "", label: "All Status" },
  { value: "active", label: "Active" },
  { value: "inactive", label: "Inactive" },
  { value: "suspended", label: "Suspended" },
];

const roleColors: Record<string, "danger" | "warning" | "info" | "default"> = {
  admin: "danger",
  investigator: "warning",
  analyst: "info",
  viewer: "default",
};

export default function UsersPage() {
  const [users, setUsers] = useState<User[]>([]);
  const [search, setSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [addOpen, setAddOpen] = useState(false);
  const [loading, setLoading] = useState(false);

  const loadUsers = async () => {
    setLoading(true);
    const data = await api.users.list({ search, role: roleFilter, status: statusFilter });
    setUsers(data);
    setLoading(false);
  };

  useState(() => { loadUsers(); });

  const columns = [
    { key: "name", header: "User", render: (u: User) => (
      <div className="flex items-center gap-3">
        <div className="h-10 w-10 bg-primary-100 rounded-full flex items-center justify-center text-primary-700 font-medium">{u.name.split(" ").map(n => n[0]).join("")}</div>
        <div>
          <div className="font-medium">{u.name}</div>
          <div className="text-xs text-gray-500">{u.email}</div>
        </div>
      </div>
    )},
    { key: "role", header: "Role", render: (u: User) => <Badge variant={roleColors[u.role]}>{u.role}</Badge> },
    { key: "department", header: "Department" },
    { key: "status", header: "Status", render: (u: User) => <Badge variant={u.status === "active" ? "success" : u.status === "suspended" ? "danger" : "default"}>{u.status}</Badge> },
    { key: "twoFactorEnabled", header: "2FA", render: (u: User) => u.twoFactorEnabled ? <Shield className="h-4 w-4 text-emerald-500" /> : <UserX className="h-4 w-4 text-red-400" /> },
    { key: "lastLogin", header: "Last Login", render: (u: User) => u.lastLogin === "-" ? <span className="text-gray-400">Never</span> : formatDateTime(u.lastLogin) },
    { key: "actions", header: "", render: () => (
      <button className="p-1.5 rounded-lg hover:bg-gray-100"><UserCheck className="h-4 w-4 text-gray-500" /></button>
    )},
  ];

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Users</h1>
            <p className="text-gray-500 mt-1">Manage system users and access control</p>
          </div>
          <Button onClick={() => setAddOpen(true)}><Plus className="h-4 w-4 mr-2" />Add User</Button>
        </div>

        <Card>
          <div className="flex flex-wrap items-center gap-4 mb-6">
            <div className="flex-1 min-w-[300px]">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                <input type="text" placeholder="Search users..." value={search} onChange={(e) => setSearch(e.target.value)} className="pl-10 pr-4 py-2 w-full border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
              </div>
            </div>
            <Select options={roleFilters} value={roleFilter} onChange={(e) => setRoleFilter(e.target.value)} />
            <Select options={statusFilters} value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} />
            <Button variant="primary" onClick={loadUsers}>Search</Button>
          </div>

          <Table columns={columns} data={users} keyExtractor={(u) => u.id} />
        </Card>

        <Modal isOpen={addOpen} onClose={() => setAddOpen(false)} title="Add User" size="lg">
          <form className="space-y-4" onSubmit={(e) => { e.preventDefault(); setAddOpen(false); }}>
            <div className="grid grid-cols-2 gap-4">
              <Input label="Full Name" placeholder="Enter full name" required />
              <Input label="Email" type="email" placeholder="user@agency.gov" required />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <Select label="Role" options={[
                { value: "admin", label: "Admin" },
                { value: "investigator", label: "Investigator" },
                { value: "analyst", label: "Analyst" },
                { value: "viewer", label: "Viewer" },
              ]} />
              <Select label="Department" options={[
                { value: "Administration", label: "Administration" },
                { value: "Investigations", label: "Investigations" },
                { value: "Intelligence", label: "Intelligence" },
                { value: "Cybersecurity", label: "Cybersecurity" },
                { value: "Legal", label: "Legal" },
                { value: "IT Security", label: "IT Security" },
              ]} />
            </div>
            <Input label="Temporary Password" type="password" placeholder="Set initial password" required />
            <div className="flex justify-end gap-3">
              <Button variant="secondary" type="button" onClick={() => setAddOpen(false)}>Cancel</Button>
              <Button type="submit">Create User</Button>
            </div>
          </form>
        </Modal>
      </div>
    </DashboardLayout>
  );
}
