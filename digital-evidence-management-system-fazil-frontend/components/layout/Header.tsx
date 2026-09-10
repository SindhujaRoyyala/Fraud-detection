"use client";

import { Bell, Search, Shield } from "lucide-react";
import { useState } from "react";

export default function Header() {
  const [searchOpen, setSearchOpen] = useState(false);

  return (
    <header className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-6">
      <div className="flex items-center gap-4">
        {searchOpen ? (
          <div className="flex items-center">
            <Search className="h-5 w-5 text-gray-400 -mr-8 ml-2 relative z-10" />
            <input type="text" placeholder="Search documents, cases, users..." className="pl-10 pr-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500 w-96" autoFocus onBlur={() => setSearchOpen(false)} />
          </div>
        ) : (
          <button onClick={() => setSearchOpen(true)} className="p-2 rounded-lg hover:bg-gray-100 transition-colors">
            <Search className="h-5 w-5 text-gray-500" />
          </button>
        )}
      </div>

      <div className="flex items-center gap-4">
        <button className="relative p-2 rounded-lg hover:bg-gray-100 transition-colors">
          <Bell className="h-5 w-5 text-gray-500" />
          <span className="absolute top-1 right-1 h-2 w-2 bg-red-500 rounded-full" />
        </button>

        <div className="flex items-center gap-3 pl-4 border-l border-gray-200">
          <div className="h-9 w-9 bg-primary-100 rounded-full flex items-center justify-center">
            <Shield className="h-5 w-5 text-primary-600" />
          </div>
          <div className="text-sm">
            <div className="font-medium text-gray-900">Sarah Mitchell</div>
            <div className="text-gray-500">Admin</div>
          </div>
        </div>
      </div>
    </header>
  );
}
