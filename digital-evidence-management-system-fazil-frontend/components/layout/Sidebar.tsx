"use client";

import { useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  LayoutDashboard, FileText, FolderOpen, Shield, Users, ClipboardList,
  BarChart3, Settings, ChevronLeft, ChevronRight, Lock
} from "lucide-react";
import { cn } from "@/lib/utils";

const navItems = [
  { href: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { href: "/documents", label: "Documents", icon: FileText },
  { href: "/cases", label: "Cases", icon: FolderOpen },
  { href: "/evidence", label: "Evidence", icon: Shield },
  { href: "/users", label: "Users", icon: Users },
  { href: "/audit-logs", label: "Audit Logs", icon: ClipboardList },
  { href: "/reports", label: "Reports", icon: BarChart3 },
  { href: "/settings", label: "Settings", icon: Settings },
];

export default function Sidebar() {
  const [collapsed, setCollapsed] = useState(false);
  const pathname = usePathname();

  return (
    <aside className={cn("h-screen bg-gray-900 text-white flex flex-col transition-all duration-300", collapsed ? "w-16" : "w-64")}>
      <div className="flex items-center gap-3 px-4 h-16 border-b border-gray-800">
        <Lock className="h-7 w-7 text-primary-400 flex-shrink-0" />
        {!collapsed && <span className="text-lg font-bold tracking-tight">SDDMS</span>}
      </div>

      <nav className="flex-1 py-4 space-y-1 px-2 overflow-y-auto">
        {navItems.map((item) => {
          const isActive = pathname === item.href || pathname?.startsWith(item.href + "/");
          return (
            <Link key={item.href} href={item.href} className={cn("flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors", isActive ? "bg-primary-600 text-white" : "text-gray-300 hover:bg-gray-800 hover:text-white")} title={collapsed ? item.label : undefined}>
              <item.icon className="h-5 w-5 flex-shrink-0" />
              {!collapsed && <span>{item.label}</span>}
            </Link>
          );
        })}
      </nav>

      <div className="border-t border-gray-800 p-2">
        <button onClick={() => setCollapsed(!collapsed)} className="flex items-center justify-center w-full p-2 rounded-lg text-gray-400 hover:bg-gray-800 hover:text-white transition-colors">
          {collapsed ? <ChevronRight className="h-5 w-5" /> : <ChevronLeft className="h-5 w-5" />}
        </button>
      </div>
    </aside>
  );
}
