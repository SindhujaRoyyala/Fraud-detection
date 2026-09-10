import { useState, useEffect, useRef, useCallback } from "react";
import { api, getStoredAuthSession, clearStoredAuthSession } from "../lib/api";
import {
  Shield, FolderOpen, Upload, ClipboardList, BarChart3, Users, Settings,
  Bell, Search, ChevronRight, AlertTriangle, CheckCircle2, Clock,
  MoreHorizontal, Download, Eye, Plus, X, Menu, ChevronDown,
  Activity, Link2, Zap, Send, FileText, FileLock2, Fingerprint,
  ShieldCheck, ShieldAlert, Lock, RefreshCw, Edit3, Trash2,
  Terminal, Cpu, Database, Radio, Wifi, Filter, TrendingUp,
  ArrowUpRight, LayoutDashboard, Hash, Layers, LogOut
} from "lucide-react";
import {
  AreaChart, Area, BarChart, Bar, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer, PieChart, Pie, Cell, LineChart, Line
} from "recharts";

type Page = "dashboard" | "cases" | "evidence" | "evidence-detail" | "upload" | "audit" | "reports" | "users" | "settings";

// ─── Colors ────────────────────────────────────────────────────────────────

const C = {
  bg: "#F4F6FA",
  surface: "rgba(255,255,255,0.82)",
  green: "#087B52",
  greenDim: "rgba(0,130,83,0.1)",
  greenBorder: "rgba(0,130,83,0.2)",
  teal: "#087F9D",
  tealDim: "rgba(8,127,157,0.1)",
  text: "#1D2935",
  muted: "#687887",
  red: "#FF4455",
  amber: "#FFB800",
  blue: "#4488FF",
};

// ─── Mock Data ─────────────────────────────────────────────────────────────

const activityData = [
  { day: "Mon", uploads: 14, views: 38, verifications: 9 },
  { day: "Tue", uploads: 22, views: 55, verifications: 14 },
  { day: "Wed", uploads: 18, views: 42, verifications: 11 },
  { day: "Thu", uploads: 31, views: 78, verifications: 20 },
  { day: "Fri", uploads: 27, views: 63, verifications: 17 },
  { day: "Sat", uploads: 8, views: 21, verifications: 4 },
  { day: "Sun", uploads: 5, views: 14, verifications: 2 },
];

const reportTrendData = [
  { month: "Apr", cases: 18, evidence: 142 },
  { month: "May", cases: 22, evidence: 168 },
  { month: "Jun", cases: 19, evidence: 155 },
  { month: "Jul", cases: 28, evidence: 201 },
  { month: "Aug", cases: 24, evidence: 187 },
  { month: "Sep", cases: 31, evidence: 248 },
];

const cases = [
  { id: "CASE-2048", name: "Operation Silent Storm", type: "Cybercrime", investigator: "Sarah Mitchell", investigators: 4, evidence: 126, progress: 84, lastActivity: "2 hours ago", status: "Under Investigation", priority: "Critical", risk: "HIGH" },
  { id: "CASE-2024-002", name: "Financial Fraud — TechCorp", type: "Financial Crime", investigator: "James Rodriguez", investigators: 3, evidence: 127, progress: 61, lastActivity: "4 hours ago", status: "Active", priority: "High", risk: "MEDIUM" },
  { id: "CASE-2024-003", name: "Insider Threat — Alpha Inc", type: "Corporate", investigator: "Priya Sharma", investigators: 2, evidence: 34, progress: 42, lastActivity: "Yesterday", status: "Review", priority: "Medium", risk: "LOW" },
  { id: "CASE-2024-004", name: "Document Forgery Ring", type: "Forgery", investigator: "Alex Chen", investigators: 3, evidence: 89, progress: 73, lastActivity: "2 days ago", status: "Active", priority: "High", risk: "HIGH" },
  { id: "CASE-2024-005", name: "Data Exfiltration — Gov", type: "Cybercrime", investigator: "Sarah Mitchell", investigators: 5, evidence: 215, progress: 38, lastActivity: "3 days ago", status: "Under Investigation", priority: "Critical", risk: "CRITICAL" },
  { id: "CASE-2024-006", name: "Money Laundering — Beta LLC", type: "Financial Crime", investigator: "Marcus Wilson", investigators: 2, evidence: 61, progress: 100, lastActivity: "1 week ago", status: "Closed", priority: "Low", risk: "NONE" },
];

const evidenceItems = [
  { id: "EVD-2048-001", type: "Document", caseId: "CASE-2048", uploadedBy: "Sarah Mitchell", date: "Sep 9, 2024", status: "Verified", custody: 5, classification: "Confidential", hash: "a3f7b2c9..." },
  { id: "EVD-2048-002", type: "Image", caseId: "CASE-2024-002", uploadedBy: "James Rodriguez", date: "Sep 8, 2024", status: "Pending", custody: 2, classification: "Secret", hash: "b4e8c3d0..." },
  { id: "EVD-2048-003", type: "Video", caseId: "CASE-2048", uploadedBy: "Priya Sharma", date: "Sep 7, 2024", status: "Under Review", custody: 3, classification: "Top Secret", hash: "c5f9d4e1..." },
  { id: "EVD-2048-004", type: "Audio", caseId: "CASE-2024-003", uploadedBy: "Alex Chen", date: "Sep 6, 2024", status: "Verified", custody: 4, classification: "Confidential", hash: "d6a0e5f2..." },
  { id: "EVD-2048-005", type: "Document", caseId: "CASE-2024-004", uploadedBy: "Marcus Wilson", date: "Sep 5, 2024", status: "Flagged", custody: 1, classification: "Secret", hash: "e7b1f6a3..." },
  { id: "EVD-2048-006", type: "Device", caseId: "CASE-2024-005", uploadedBy: "Sarah Mitchell", date: "Sep 4, 2024", status: "Verified", custody: 6, classification: "Top Secret", hash: "f8c2a7b4..." },
];

const auditEvents = [
  { time: "10:42", user: "Sarah Mitchell", action: "Evidence EVD-2048-001 verified", target: "EVD-2048-001", ip: "192.168.1.45", device: "Chrome / macOS", result: "success" },
  { time: "10:18", user: "Legal Team", action: "Evidence transferred to Legal Team", target: "EVD-2048-004", ip: "10.0.0.22", device: "Firefox / Windows", result: "success" },
  { time: "09:52", user: "Priya Sharma", action: "New evidence uploaded", target: "EVD-2048-003", ip: "172.16.0.8", device: "Safari / macOS", result: "success" },
  { time: "09:30", user: "Alex Chen", action: "Chain of custody updated", target: "EVD-2048-002", ip: "192.168.1.78", device: "Chrome / Windows", result: "success" },
  { time: "07:41", user: "Unknown", action: "Failed login attempt", target: "admin@siv.gov", ip: "203.145.22.1", device: "Chrome / Linux", result: "failed" },
  { time: "07:20", user: "Marcus Wilson", action: "Report generated", target: "CASE-2024-006", ip: "10.0.0.31", device: "Edge / Windows", result: "success" },
];

const custodyChain = [
  { actor: "Sarah Mitchell", role: "Lead Investigator", action: "Evidence uploaded & initial hash computed", time: "Sep 9, 2024 09:14 AM", device: "Workstation-007", hash: "a3f7b2c9..." },
  { actor: "System", role: "Automated Verification", action: "SHA-256 hash integrity verified successfully", time: "Sep 9, 2024 09:15 AM", device: "SIV-Server-01", hash: "a3f7b2c9..." },
  { actor: "James Rodriguez", role: "Forensic Analyst", action: "Transferred for forensic analysis", time: "Sep 9, 2024 11:00 AM", device: "Lab-Terminal-03", hash: "a3f7b2c9..." },
  { actor: "Legal Team", role: "Senior Counsel", action: "Accessed for legal review", time: "Sep 9, 2024 02:30 PM", device: "Secure-Workstation", hash: "a3f7b2c9..." },
  { actor: "Pending", role: "Court Submission", action: "Awaiting approval for submission", time: "—", device: "—", hash: "—" },
];

// ─── Intro Screen ──────────────────────────────────────────────────────────

function IntroScreen({ onDone }: { onDone: () => void }) {
  const [phase, setPhase] = useState(0);
  const statusLines = [
    "SYSTEM SECURE",
    "ENCRYPTION ACTIVE",
    "INTEGRITY MONITORING ONLINE",
  ];

  useEffect(() => {
    const timers = [
      setTimeout(() => setPhase(1), 200),
      setTimeout(() => setPhase(2), 900),
      setTimeout(() => setPhase(3), 1600),
      setTimeout(() => setPhase(4), 2200),
      setTimeout(() => setPhase(5), 2800),
      setTimeout(() => onDone(), 3800),
    ];
    return () => timers.forEach(clearTimeout);
  }, [onDone]);

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center"
      style={{ background: C.bg, transition: "opacity 0.6s ease", opacity: phase === 5 ? 0 : 1 }}
    >
      {/* Subtle radial glow */}
      <div className="absolute inset-0 pointer-events-none" style={{
        background: "radial-gradient(ellipse 60% 50% at 50% 50%, rgba(0,217,126,0.05) 0%, transparent 70%)"
      }} />

      <div className="text-center relative z-10 flex flex-col items-center gap-6">
        {/* Animated vault SVG */}
        <div style={{ opacity: phase >= 1 ? 1 : 0, transition: "opacity 0.5s ease" }}>
          <svg width="100" height="100" viewBox="0 0 100 100">
            {/* Outer ring */}
            <circle cx="50" cy="50" r="44" fill="none" stroke="rgba(0,217,126,0.15)" strokeWidth="1" />
            {/* Animated path */}
            <circle cx="50" cy="50" r="44" fill="none" stroke={C.green} strokeWidth="1.5"
              strokeDasharray="276" strokeDashoffset={phase >= 1 ? 0 : 276}
              style={{ transition: "stroke-dashoffset 0.8s ease", transformOrigin: "center", transform: "rotate(-90deg)" }}
            />
            {/* Shield */}
            <path d="M50 18 L72 28 L72 50 C72 63 62 73 50 78 C38 73 28 63 28 50 L28 28 Z"
              fill="none" stroke={C.green} strokeWidth="1.5"
              strokeDasharray="180" strokeDashoffset={phase >= 2 ? 0 : 180}
              style={{ transition: "stroke-dashoffset 0.7s ease 0.2s" }}
            />
            {/* Center mark */}
            <circle cx="50" cy="50" r="6" fill={C.green}
              style={{ opacity: phase >= 2 ? 1 : 0, transition: "opacity 0.3s ease 0.5s" }}
            />
            {/* Scan line */}
            {phase >= 2 && <line x1="28" y1="50" x2="72" y2="50" stroke={C.green} strokeWidth="1" opacity="0.4">
              <animateTransform attributeName="transform" type="rotate" from="0 50 50" to="360 50 50" dur="3s" repeatCount="indefinite" />
            </line>}
          </svg>
        </div>

        {/* Brand */}
        <div style={{ opacity: phase >= 3 ? 1 : 0, transform: phase >= 3 ? "translateY(0)" : "translateY(12px)", transition: "all 0.5s ease" }}>
          <div className="mono text-xs tracking-[0.3em] mb-1" style={{ color: C.green }}>SECURE INVESTIGATION VAULT</div>
          <div className="text-2xl font-bold tracking-wide" style={{ color: C.text }}>SIV COMMAND CENTER</div>
        </div>

        {/* Status lines */}
        <div className="space-y-1.5" style={{ opacity: phase >= 4 ? 1 : 0, transition: "opacity 0.5s ease" }}>
          {statusLines.map((line, i) => (
            <div key={i} className="flex items-center gap-2 justify-center"
              style={{ animationDelay: `${i * 150}ms` }}
            >
              <div className="w-1.5 h-1.5 rounded-full pulse-dot" style={{ background: C.green }} />
              <span className="mono text-xs tracking-widest" style={{ color: C.green, opacity: 0.8 }}>{line}</span>
            </div>
          ))}
        </div>

        {/* Progress bar */}
        <div className="w-48 h-px overflow-hidden rounded-full" style={{ background: "rgba(0,101,67,0.12)", opacity: phase >= 4 ? 1 : 0, transition: "opacity 0.3s ease" }}>
          <div className="h-full rounded-full" style={{
            background: C.green,
            width: phase >= 5 ? "100%" : phase >= 4 ? "70%" : "0%",
            transition: "width 0.8s ease",
            boxShadow: `0 0 8px ${C.green}`
          }} />
        </div>
      </div>
    </div>
  );
}

// ─── Login Screen ──────────────────────────────────────────────────────────

function LoginScreen({ onLogin }: { onLogin: (user: { username?: string; email?: string; fullName?: string }) => void }) {
  const [mode, setMode] = useState<"login" | "register">("login");
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if ((mode === "register" && !fullName.trim()) || !email.trim() || !password.trim()) {
      setError(mode === "register" ? "Complete your officer profile to register." : "Enter your authorized credentials to continue.");
      return;
    }
    if (password.length < 6 || !/[A-Z]/.test(password) || !/[0-9]/.test(password) || !/[^A-Za-z0-9]/.test(password)) {
      setError("Password must be 6+ characters with an uppercase letter, number, and symbol.");
      return;
    }
    if (mode === "register" && password !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    setError("");
    setLoading(true);

    try {
      const username = mode === "register" ? (fullName.trim() || email.split("@")[0]) : email.trim();
      const result = mode === "login"
        ? await api.auth.login({ username, password })
        : await api.auth.register({
            username,
            email: email.trim(),
            password,
            fullName: fullName.trim(),
            role: "VIEWER",
          });

      const user = result?.user ?? { username, email: email.trim(), fullName: fullName.trim() };
      onLogin(user);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to connect to the secure backend.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-40 overflow-y-auto" style={{ background: "#F4F6FA" }}>
      <div className="absolute inset-0 tech-grid opacity-60" />
      <div className="absolute inset-0 pointer-events-none" style={{
        background: "radial-gradient(ellipse 70% 80% at 50% 45%, rgba(0,130,83,0.08) 0%, transparent 68%)"
      }} />

      <div className="relative z-10 min-h-full flex items-center justify-center px-6 py-12">
        <div className="w-full max-w-5xl grid lg:grid-cols-[1.05fr_0.95fr] gap-12 items-center">
          <div className="hidden lg:block anim-slide-left">
            <div className="flex items-center gap-3 mb-8">
              <div className="w-11 h-11 rounded-2xl flex items-center justify-center glow-pulse"
                style={{ background: "rgba(0,130,83,0.16)", border: "1px solid rgba(31,157,104,0.5)" }}>
                <FileLock2 size={20} style={{ color: "#087B52" }} />
              </div>
              <div>
                <div className="mono text-xs font-semibold tracking-[0.28em]" style={{ color: "#087B52" }}>SIV COMMAND</div>
                <div className="mono text-[10px] tracking-[0.2em]" style={{ color: "#435C50" }}>SECURE INVESTIGATION VAULT</div>
              </div>
            </div>
            <div className="mono text-[10px] tracking-[0.22em] mb-4" style={{ color: "#176B45" }}>AUTHORIZED ACCESS ONLY</div>
            <h1 className="text-5xl font-bold leading-tight tracking-tight mb-5" style={{ color: "#1D2935" }}>
              Enter the<br /><span style={{ color: "#176B45" }}>investigation vault.</span>
            </h1>
            <p className="max-w-md text-base leading-relaxed" style={{ color: "#435C50" }}>
              Access evidence integrity, active investigations and chain-of-custody intelligence from one secure command center.
            </p>
            <div className="flex items-center gap-6 mt-10 mono text-[9px] tracking-widest" style={{ color: "#739487" }}>
              <span className="flex items-center gap-2"><span className="w-1.5 h-1.5 rounded-full pulse-dot" style={{ background: "#087B52" }} />ENCRYPTED</span>
              <span className="flex items-center gap-2"><span className="w-1.5 h-1.5 rounded-full pulse-dot" style={{ background: "#087B52" }} />AUDIT READY</span>
            </div>
          </div>

          <div className="anim-slide-right rounded-3xl p-7 sm:p-9" style={{
            background: "rgba(255,255,255,0.9)",
            border: "1px solid rgba(0,101,67,0.16)",
            boxShadow: "0 24px 70px rgba(34,58,48,0.14)",
            backdropFilter: "blur(18px)"
          }}>
            <div className="lg:hidden flex items-center gap-3 mb-8">
              <div className="w-10 h-10 rounded-xl flex items-center justify-center" style={{ background: "rgba(0,130,83,0.16)", border: "1px solid rgba(31,157,104,0.5)" }}>
                <FileLock2 size={17} style={{ color: "#087B52" }} />
              </div>
              <div className="mono text-xs tracking-[0.2em]" style={{ color: "#087B52" }}>SIV COMMAND</div>
            </div>
            <div className="flex p-1 rounded-xl mb-7" style={{ background: "#EEF3F0", border: "1px solid rgba(0,101,67,0.12)" }}>
              {(["login", "register"] as const).map(option => (
                <button key={option} type="button" onClick={() => { setMode(option); setError(""); }}
                  className="flex-1 rounded-lg py-2 mono text-[11px] tracking-widest transition-all"
                  style={mode === option ? { background: "#FFFFFF", color: "#176B45", boxShadow: "0 3px 10px rgba(24,70,52,0.1)" } : { color: "#435C50" }}>
                  {option === "login" ? "SIGN IN" : "REGISTER"}
                </button>
              ))}
            </div>
            <div className="mono text-[9px] tracking-[0.2em] mb-2" style={{ color: "#176B45" }}>{mode === "login" ? "SECURE SIGN IN" : "OFFICER REGISTRATION"}</div>
            <h2 className="text-2xl font-bold mb-2" style={{ color: "#1D2935" }}>{mode === "login" ? "Welcome back" : "Create your access"}</h2>
            <p className="text-base mb-7" style={{ color: "#435C50" }}>{mode === "login" ? "Authenticate to enter the command center." : "Register your authorized investigator profile."}</p>

            <form onSubmit={submit} className="space-y-5">
              {mode === "register" && <label className="block">
                <span className="mono block text-[10px] tracking-[0.16em] mb-2" style={{ color: "#264838" }}>FULL NAME</span>
                <input type="text" value={fullName} onChange={e => setFullName(e.target.value)} placeholder="Officer full name"
                  className="w-full rounded-xl px-4 py-3 text-sm outline-none transition-all"
                  style={{ background: "#FFFFFF", border: "1px solid rgba(0,101,67,0.2)", color: "#1D2935" }} />
              </label>}
              <label className="block">
                <span className="mono block text-[10px] tracking-[0.16em] mb-2" style={{ color: "#264838" }}>OFFICIAL EMAIL</span>
                <input type="email" value={email} onChange={e => setEmail(e.target.value)} placeholder="officer@siv.gov"
                  className="w-full rounded-xl px-4 py-3 text-sm outline-none transition-all"
                  style={{ background: "#FFFFFF", border: "1px solid rgba(0,101,67,0.2)", color: "#1D2935" }} />
              </label>

              {mode === "register" && <label className="block">
                <span className="mono block text-[10px] tracking-[0.16em] mb-2" style={{ color: "#264838" }}>CONFIRM PASSWORD</span>
                <input type={showPassword ? "text" : "password"} value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)} placeholder="Re-enter secure password"
                  className="w-full rounded-xl px-4 py-3 text-sm outline-none transition-all"
                  style={{ background: "#FFFFFF", border: "1px solid rgba(0,101,67,0.2)", color: "#1D2935" }} />
              </label>}
              <label className="block">
                <span className="mono block text-[10px] tracking-[0.16em] mb-2" style={{ color: "#264838" }}>PASSWORD</span>
                <div className="relative">
                  <input type={showPassword ? "text" : "password"} value={password} onChange={e => setPassword(e.target.value)} placeholder="Enter secure password" minLength={6}
                    className="w-full rounded-xl px-4 py-3 pr-11 text-sm outline-none transition-all"
                    style={{ background: "#FFFFFF", border: "1px solid rgba(0,101,67,0.2)", color: "#1D2935" }} />
                  <button type="button" aria-label={showPassword ? "Hide password" : "Show password"} onClick={() => setShowPassword(value => !value)}
                    className="absolute right-3 top-1/2 -translate-y-1/2" style={{ color: "#435C50" }}>
                    <Eye size={16} />
                  </button>
                </div>
                <span className="block mt-2 text-xs" style={{ color: "#435C50" }}>Minimum 6 characters · one uppercase · one number · one symbol</span>
              </label>

              {error && <div className="text-xs rounded-lg px-3 py-2" style={{ color: "#FF9AA4", background: "rgba(255,68,85,0.1)", border: "1px solid rgba(255,68,85,0.25)" }}>{error}</div>}

              <button type="submit" disabled={loading} className="w-full rounded-xl py-3.5 btn-solid flex items-center justify-center gap-2 text-sm disabled:opacity-70 disabled:cursor-not-allowed">
                <ShieldCheck size={16} /> {loading ? "AUTHENTICATING..." : mode === "login" ? "ENTER COMMAND CENTER" : "REQUEST SECURE ACCESS"} <ArrowUpRight size={14} />
              </button>
            </form>

            <div className="flex items-center justify-between mt-6 pt-5" style={{ borderTop: "1px solid rgba(0,101,67,0.14)" }}>
              <span className="mono text-[10px] tracking-widest" style={{ color: "#435C50" }}>SYSTEM SECURE</span>
              <span className="flex items-center gap-1.5 mono text-[9px]" style={{ color: "#087B52" }}><span className="w-1.5 h-1.5 rounded-full pulse-dot" style={{ background: "#087B52" }} />TLS 1.3</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

// ─── Page Transition ───────────────────────────────────────────────────────

function PageTransition({ page, children }: { page: Page; children: React.ReactNode }) {
  const [visible, setVisible] = useState(false);
  const [key, setKey] = useState(0);

  useEffect(() => {
    setVisible(false);
    setKey(k => k + 1);
    const t = setTimeout(() => setVisible(true), 80);
    return () => clearTimeout(t);
  }, [page]);

  return (
    <div key={key} style={{
      opacity: visible ? 1 : 0,
      filter: visible ? "blur(0)" : "blur(4px)",
      transform: visible ? "scale(1) translateY(0)" : "scale(0.99) translateY(6px)",
      transition: "all 0.45s cubic-bezier(0.16, 1, 0.3, 1)",
      flex: 1, minHeight: 0, display: "flex", flexDirection: "column", overflow: "hidden"
    }}>
      {children}
    </div>
  );
}

// ─── Status Badge ──────────────────────────────────────────────────────────

function Badge({ status, size = "sm" }: { status: string; size?: "sm" | "xs" }) {
  const map: Record<string, { bg: string; color: string; border: string }> = {
    "Verified":           { bg: "rgba(0,217,126,0.1)", color: "#087B52", border: "rgba(0,217,126,0.3)" },
    "Pending":            { bg: "rgba(255,184,0,0.1)", color: "#FFB800", border: "rgba(255,184,0,0.3)" },
    "Under Review":       { bg: "rgba(68,136,255,0.1)", color: "#4488FF", border: "rgba(68,136,255,0.3)" },
    "Flagged":            { bg: "rgba(255,68,85,0.1)", color: "#FF4455", border: "rgba(255,68,85,0.3)" },
    "Active":             { bg: "rgba(0,217,126,0.1)", color: "#087B52", border: "rgba(0,217,126,0.3)" },
    "Under Investigation":{ bg: "rgba(68,136,255,0.1)", color: "#4488FF", border: "rgba(68,136,255,0.3)" },
    "Review":             { bg: "rgba(255,184,0,0.1)", color: "#FFB800", border: "rgba(255,184,0,0.3)" },
    "Closed":             { bg: "rgba(255,255,255,0.05)", color: "#4A7060", border: "rgba(255,255,255,0.1)" },
    "Critical":           { bg: "rgba(255,68,85,0.1)", color: "#FF4455", border: "rgba(255,68,85,0.3)" },
    "High":               { bg: "rgba(255,120,0,0.1)", color: "#FF7800", border: "rgba(255,120,0,0.3)" },
    "Medium":             { bg: "rgba(255,184,0,0.1)", color: "#FFB800", border: "rgba(255,184,0,0.3)" },
    "Low":                { bg: "rgba(255,255,255,0.05)", color: "#4A7060", border: "rgba(255,255,255,0.1)" },
    "Confidential":       { bg: "rgba(180,100,255,0.1)", color: "#B464FF", border: "rgba(180,100,255,0.3)" },
    "Secret":             { bg: "rgba(255,120,0,0.1)", color: "#FF7800", border: "rgba(255,120,0,0.3)" },
    "Top Secret":         { bg: "rgba(255,68,85,0.1)", color: "#FF4455", border: "rgba(255,68,85,0.3)" },
    "success":            { bg: "rgba(0,217,126,0.1)", color: "#087B52", border: "rgba(0,217,126,0.3)" },
    "failed":             { bg: "rgba(255,68,85,0.1)", color: "#FF4455", border: "rgba(255,68,85,0.3)" },
    "CRITICAL":           { bg: "rgba(255,68,85,0.15)", color: "#FF4455", border: "rgba(255,68,85,0.4)" },
    "HIGH":               { bg: "rgba(255,120,0,0.1)", color: "#FF7800", border: "rgba(255,120,0,0.3)" },
    "MEDIUM":             { bg: "rgba(255,184,0,0.1)", color: "#FFB800", border: "rgba(255,184,0,0.3)" },
    "LOW":                { bg: "rgba(0,217,126,0.1)", color: "#087B52", border: "rgba(0,217,126,0.3)" },
    "NONE":               { bg: "rgba(255,255,255,0.05)", color: "#4A7060", border: "rgba(255,255,255,0.1)" },
    "Inactive":           { bg: "rgba(255,255,255,0.05)", color: "#4A7060", border: "rgba(255,255,255,0.1)" },
  };
  const s = map[status] ?? { bg: "rgba(255,255,255,0.05)", color: "#4A7060", border: "rgba(255,255,255,0.1)" };
  const pad = size === "xs" ? "px-1.5 py-0.5 text-[9px]" : "px-2 py-0.5 text-[10px]";
  return (
    <span className={`inline-flex items-center rounded-full font-semibold tracking-wide mono ${pad}`}
      style={{ background: s.bg, color: s.color, border: `1px solid ${s.border}` }}>
      {status}
    </span>
  );
}

// ─── Evidence Network SVG ──────────────────────────────────────────────────

function EvidenceNetwork() {
  const nodes = [
    { x: 300, y: 110, label: "CASE-2048", type: "case", size: 18 },
    { x: 160, y: 230, label: "DOCS", type: "evidence", size: 12 },
    { x: 440, y: 230, label: "IMAGES", type: "evidence", size: 12 },
    { x: 90, y: 340, label: "DEVICE", type: "evidence", size: 9 },
    { x: 230, y: 340, label: "AUDIO", type: "evidence", size: 9 },
    { x: 370, y: 340, label: "VIDEO", type: "evidence", size: 9 },
    { x: 510, y: 340, label: "FORENSIC", type: "evidence", size: 9 },
    { x: 510, y: 150, label: "CASE-2049", type: "case2", size: 13 },
    { x: 100, y: 170, label: "CASE-2047", type: "case2", size: 13 },
  ];
  const edges = [
    [0,1],[0,2],[1,3],[1,4],[2,5],[2,6],[0,7],[0,8]
  ];

  return (
    <svg width="100%" height="100%" viewBox="0 0 600 420" style={{ overflow: "visible" }}>
      <defs>
        <radialGradient id="nodeGlow" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stopColor="#075E70" stopOpacity="0.65" />
          <stop offset="100%" stopColor="#075E70" stopOpacity="0" />
        </radialGradient>
        <filter id="glow">
          <feGaussianBlur stdDeviation="2" result="blur" />
          <feMerge><feMergeNode in="blur" /><feMergeNode in="SourceGraphic" /></feMerge>
        </filter>
      </defs>
      {/* Edges */}
      {edges.map(([a, b], i) => (
        <line key={i}
          x1={nodes[a].x} y1={nodes[a].y} x2={nodes[b].x} y2={nodes[b].y}
          stroke="rgba(7,94,112,0.72)" strokeWidth="4.5"
          strokeDasharray="300" strokeDashoffset="0"
          style={{ animation: `drawPath 1.2s ease ${i * 100}ms both` }}
        >
          <animate attributeName="stroke-opacity" values="0.1;0.3;0.1" dur={`${3 + i * 0.3}s`} repeatCount="indefinite" />
        </line>
      ))}
      {/* Nodes */}
      {nodes.map((n, i) => (
        <g key={i} style={{ animation: `nodeReveal 0.4s ease ${300 + i * 80}ms both` }}>
          <circle cx={n.x} cy={n.y} r={n.size + 8} fill={n.type === "case" ? "rgba(7,94,112,0.12)" : "transparent"}>
            <animate attributeName="r" values={`${n.size+4};${n.size+10};${n.size+4}`} dur="3s" repeatCount="indefinite" />
          </circle>
          <circle cx={n.x} cy={n.y} r={n.size}
            fill={n.type === "case" ? "rgba(7,94,112,0.24)" : n.type === "case2" ? "rgba(46,76,143,0.2)" : "rgba(7,94,112,0.12)"}
            stroke={n.type === "case" ? "#075E70" : n.type === "case2" ? "#2E4C8F" : "rgba(7,94,112,0.82)"}
            strokeWidth="4"
            filter="url(#glow)"
          />
          <text x={n.x} y={n.y + n.size + 16} textAnchor="middle" fill="#123F4D"
            style={{ fontSize: "11px", fontWeight: 700, fontFamily: "'JetBrains Mono', monospace", letterSpacing: "0.05em" }}>
            {n.label}
          </text>
        </g>
      ))}
      {/* Traveling particles */}
      {edges.slice(0, 4).map(([a, b], i) => (
        <circle key={`p${i}`} r="2" fill="#075E70" opacity="0.9">
          <animateMotion dur={`${2 + i * 0.4}s`} repeatCount="indefinite" path={`M${nodes[a].x},${nodes[a].y} L${nodes[b].x},${nodes[b].y}`} />
        </circle>
      ))}
    </svg>
  );
}

// ─── Sidebar ───────────────────────────────────────────────────────────────

function Sidebar({ current, onNav, collapsed, onToggle }: {
  current: Page; onNav: (p: Page) => void; collapsed: boolean; onToggle: () => void;
}) {
  const navItems = [
    { page: "dashboard" as Page, icon: LayoutDashboard, label: "Command Center" },
    { page: "cases" as Page, icon: FolderOpen, label: "Cases" },
    { page: "evidence" as Page, icon: Shield, label: "Evidence Vault" },
    { page: "upload" as Page, icon: Upload, label: "Upload" },
    { page: "audit" as Page, icon: ClipboardList, label: "Audit Trail", badge: 3 },
    { page: "reports" as Page, icon: BarChart3, label: "Reports" },
    { page: "users" as Page, icon: Users, label: "Users" },
    { page: "settings" as Page, icon: Settings, label: "Settings" },
  ];

  return (
    <aside className="h-full flex flex-col transition-all duration-300 relative z-10"
      style={{
        width: collapsed ? "60px" : "210px",
        minWidth: collapsed ? "60px" : "210px",
        background: "rgba(255,255,255,0.92)",
        borderRight: "1px solid rgba(0,217,126,0.1)",
        backdropFilter: "blur(20px)",
      }}>
      {/* Logo */}
      <div className="h-14 flex items-center px-3 gap-3 flex-shrink-0" style={{ borderBottom: "1px solid rgba(0,217,126,0.1)" }}>
        <div className="w-8 h-8 rounded-xl flex items-center justify-center flex-shrink-0 glow-pulse"
          style={{ background: "rgba(0,217,126,0.15)", border: "1px solid rgba(0,217,126,0.4)" }}>
          <FileLock2 size={14} style={{ color: C.green }} />
        </div>
        {!collapsed && (
          <div className="overflow-hidden flex-1">
            <div className="mono text-[10px] font-semibold tracking-[0.2em]" style={{ color: C.green }}>SIV</div>
            <div className="flex items-center gap-1.5">
              <div className="w-1 h-1 rounded-full pulse-dot" style={{ background: C.green }} />
              <span className="mono text-[9px] tracking-widest" style={{ color: "rgba(0,92,56,0.82)" }}>SECURE</span>
            </div>
          </div>
        )}
        <button onClick={onToggle} className="ml-auto flex-shrink-0 transition-colors" style={{ color: C.muted }}>
          <Menu size={14} />
        </button>
      </div>

      {/* Nav */}
      <nav className="flex-1 py-4 overflow-y-auto px-2 space-y-0.5">
        {!collapsed && <div className="px-2 mb-2 mono text-[9px] tracking-[0.2em] uppercase" style={{ color: "rgba(0,92,56,0.68)" }}>Navigation</div>}
        {navItems.map(({ page, icon: Icon, label, badge }) => {
          const active = current === page;
          return (
            <button key={page} onClick={() => onNav(page)}
              className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-xs font-medium transition-all duration-200 ${collapsed ? "justify-center" : ""} ${active ? "nav-active" : ""}`}
              style={active ? {} : { color: C.muted, border: "1px solid transparent" }}
              onMouseEnter={e => { if (!active) (e.currentTarget as HTMLElement).style.color = C.text; }}
              onMouseLeave={e => { if (!active) (e.currentTarget as HTMLElement).style.color = C.muted; }}
            >
              <Icon size={15} className="flex-shrink-0" />
              {!collapsed && <span className="truncate">{label}</span>}
              {!collapsed && badge && (
                <span className="ml-auto text-[9px] font-bold rounded-full w-4 h-4 flex items-center justify-center"
                  style={{ background: C.red, color: "white" }}>{badge}</span>
              )}
            </button>
          );
        })}
      </nav>

      {/* User */}
      <div className="p-2" style={{ borderTop: "1px solid rgba(0,217,126,0.1)" }}>
        <div className="flex items-center gap-2.5 p-2 rounded-xl cursor-pointer transition-colors"
          style={{ border: "1px solid transparent" }}
          onMouseEnter={e => { (e.currentTarget as HTMLElement).style.borderColor = "rgba(0,217,126,0.15)"; }}
          onMouseLeave={e => { (e.currentTarget as HTMLElement).style.borderColor = "transparent"; }}>
          <div className="w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0"
            style={{ background: "rgba(0,217,126,0.2)", color: C.green, border: "1px solid rgba(0,217,126,0.4)" }}>SM</div>
          {!collapsed && (
            <div className="overflow-hidden">
              <div className="text-xs font-semibold truncate" style={{ color: C.text }}>Sarah Mitchell</div>
              <div className="text-[9px] mono tracking-wide" style={{ color: C.muted }}>Administrator</div>
            </div>
          )}
        </div>
      </div>
    </aside>
  );
}

// ─── Header ────────────────────────────────────────────────────────────────

function Header({ page, onNav }: { page: Page; onNav: (p: Page) => void }) {
  const labels: Record<Page, string> = {
    dashboard: "Command Center", cases: "Cases", evidence: "Evidence Vault",
    "evidence-detail": "Evidence Detail", upload: "Upload Evidence",
    audit: "Audit Trail", reports: "Reports", users: "Users", settings: "Settings"
  };

  return (
    <header className="h-14 flex items-center px-6 gap-4 flex-shrink-0"
      style={{ background: "rgba(255,255,255,0.9)", borderBottom: "1px solid rgba(0,101,67,0.12)", backdropFilter: "blur(20px)" }}>
      <div className="flex items-center gap-2 mono text-xs" style={{ color: C.muted }}>
        <span>SIV</span>
        <ChevronRight size={12} />
        <span style={{ color: C.green }}>{labels[page]}</span>
      </div>

      <div className="ml-auto flex items-center gap-3">
        <div className="relative group">
          <Search size={13} className="absolute left-3 top-1/2 -translate-y-1/2" style={{ color: C.muted }} />
          <input className="pl-8 pr-10 py-1.5 rounded-xl text-xs outline-none transition-all w-48"
            style={{ background: "rgba(0,217,126,0.05)", border: "1px solid rgba(0,217,126,0.15)", color: C.text }}
            onFocus={e => (e.target as HTMLElement).style.borderColor = "rgba(0,217,126,0.4)"}
            onBlur={e => (e.target as HTMLElement).style.borderColor = "rgba(0,217,126,0.15)"}
            placeholder="Search…" />
          <kbd className="absolute right-2.5 top-1/2 -translate-y-1/2 mono text-[9px] px-1 py-0.5 rounded"
            style={{ color: C.muted, background: "rgba(255,255,255,0.05)" }}>⌘K</kbd>
        </div>

        <button className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl mono text-[9px] tracking-widest transition-all btn-primary">
          <ShieldCheck size={11} />SECURE
        </button>

        <button className="relative w-8 h-8 rounded-xl flex items-center justify-center transition-all"
          style={{ background: "rgba(255,255,255,0.03)", border: "1px solid rgba(255,255,255,0.08)", color: C.muted }}>
          <Bell size={14} />
          <span className="absolute top-1 right-1 w-1.5 h-1.5 rounded-full" style={{ background: C.red }} />
        </button>

        <button className="w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold transition-all"
          style={{ background: "rgba(0,217,126,0.2)", color: C.green, border: "1px solid rgba(0,217,126,0.4)" }}>SM</button>
      </div>
    </header>
  );
}

// ─── Dashboard ─────────────────────────────────────────────────────────────

function DashboardPage({ onNav }: { onNav: (p: Page) => void }) {
  const [time, setTime] = useState(new Date());
  const [aiInput, setAiInput] = useState("");
  const [aiMsgs, setAiMsgs] = useState(["Analyzing CASE-2048: 3 evidence items require immediate hash verification. Chain-of-custody review overdue. Risk level elevated."]);

  useEffect(() => {
    const t = setInterval(() => setTime(new Date()), 1000);
    return () => clearInterval(t);
  }, []);

  const metrics = [
    { value: "24", label: "ACTIVE CASES", delay: 0, color: C.green },
    { value: "1,248", label: "EVIDENCE ITEMS", delay: 80, color: C.teal },
    { value: "17", label: "PENDING REVIEWS", delay: 160, color: C.amber },
    { value: "03", label: "SECURITY ALERTS", delay: 240, color: C.red },
  ];

  const securityItems = [
    { label: "Evidence Integrity", ok: true },
    { label: "Chain of Custody", ok: true },
    { label: "Encryption", ok: true },
    { label: "Authentication", ok: true },
    { label: "Audit Monitoring", ok: true },
  ];

  const liveEvents = [
    { time: "10:42", text: "Evidence EVD-2048-001 verified" },
    { time: "10:18", text: "Evidence transferred to Legal Team" },
    { time: "09:52", text: "New evidence uploaded" },
    { time: "09:30", text: "Chain of custody updated" },
  ];

  return (
    <div className="flex-1 overflow-y-auto" style={{ background: C.bg }}>
      {/* Hero */}
      <div className="relative overflow-hidden" style={{ borderBottom: "1px solid rgba(0,217,126,0.08)" }}>
        <div className="absolute inset-0 tech-grid" />
        <div className="absolute inset-0" style={{
          background: "radial-gradient(ellipse 80% 100% at 50% 0%, rgba(0,217,126,0.06) 0%, transparent 60%)"
        }} />
        <div className="relative z-10 px-8 py-10 flex items-start justify-between">
          <div className="anim-fade-up" style={{ animationDelay: "0ms" }}>
            <div className="mono text-xs tracking-[0.2em] mb-3 flex items-center gap-2" style={{ color: "rgba(0,92,56,0.88)" }}>
              <div className="w-1 h-1 rounded-full pulse-dot" style={{ background: C.green }} />
              INVESTIGATION COMMAND CENTER
            </div>
            <h1 className="text-4xl font-bold tracking-tight mb-2" style={{ color: C.text }}>
              Secure Investigation Vault
            </h1>
            <p className="text-sm" style={{ color: C.muted, maxWidth: "420px" }}>
              Monitor evidence integrity, active investigations and security events across all active cases.
            </p>
          </div>
          <div className="text-right anim-slide-right" style={{ animationDelay: "100ms" }}>
            <div className="flex items-center gap-2 justify-end mb-1">
              <div className="w-1.5 h-1.5 rounded-full pulse-dot" style={{ background: C.green }} />
              <span className="mono text-[10px] tracking-widest" style={{ color: C.green }}>ALL SYSTEMS OPERATIONAL</span>
            </div>
            <div className="mono text-xs" style={{ color: C.muted }}>
              {time.toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" })}
              {" · "}{time.toLocaleTimeString()}
            </div>
          </div>
        </div>
      </div>

      {/* Metrics + Network */}
      <div className="grid grid-cols-4 gap-px px-8 py-0" style={{ borderBottom: "1px solid rgba(0,217,126,0.08)" }}>
        {metrics.map((m, i) => (
          <div key={i} className="py-6 px-6 anim-count flex flex-col gap-1"
            style={{ animationDelay: `${m.delay}ms`, borderRight: i < 3 ? "1px solid rgba(0,217,126,0.08)" : undefined }}>
            <div className="text-4xl font-bold mono" style={{ color: m.color }}>{m.value}</div>
            <div className="mono text-[9px] tracking-[0.2em]" style={{ color: C.muted }}>{m.label}</div>
          </div>
        ))}
      </div>

      {/* Main grid */}
      <div className="grid grid-cols-12 gap-0 divide-x" style={{ borderBottom: "1px solid rgba(0,217,126,0.08)", borderColor: "rgba(0,217,126,0.08)" }}>
        {/* Evidence Network */}
        <div className="col-span-5 p-6 anim-fade-up" style={{ animationDelay: "200ms", borderRight: "1px solid rgba(0,217,126,0.08)" }}>
          <div className="mono text-[9px] tracking-[0.2em] mb-3" style={{ color: "rgba(0,92,56,0.82)" }}>EVIDENCE INTELLIGENCE NETWORK</div>
          <div style={{ height: "300px" }}>
            <EvidenceNetwork />
          </div>
        </div>

        {/* Active Investigations */}
        <div className="col-span-4 p-6 anim-fade-up" style={{ animationDelay: "280ms", borderRight: "1px solid rgba(0,217,126,0.08)" }}>
          <div className="mono text-[9px] tracking-[0.2em] mb-4" style={{ color: "rgba(0,92,56,0.82)" }}>ACTIVE INVESTIGATIONS</div>
          <div className="space-y-3">
            {cases.slice(0, 3).map((c, i) => (
              <div key={i} className="p-3 rounded-xl cursor-pointer group transition-all"
                style={{ background: "rgba(0,217,126,0.04)", border: "1px solid rgba(0,217,126,0.1)" }}
                onMouseEnter={e => { (e.currentTarget as HTMLElement).style.borderColor = "rgba(0,217,126,0.3)"; (e.currentTarget as HTMLElement).style.background = "rgba(0,217,126,0.08)"; }}
                onMouseLeave={e => { (e.currentTarget as HTMLElement).style.borderColor = "rgba(0,217,126,0.1)"; (e.currentTarget as HTMLElement).style.background = "rgba(0,217,126,0.04)"; }}>
                <div className="flex items-center justify-between mb-2">
                  <span className="mono text-[9px] font-semibold tracking-wide" style={{ color: C.green }}>{c.id}</span>
                  <Badge status={c.risk} size="xs" />
                </div>
                <div className="text-xs font-semibold mb-2" style={{ color: C.text }}>{c.name}</div>
                <div className="w-full h-1 rounded-full overflow-hidden mb-2" style={{ background: "rgba(0,217,126,0.1)" }}>
                  <div className="h-full rounded-full transition-all" style={{ width: `${c.progress}%`, background: `linear-gradient(90deg, ${C.green}, ${C.teal})` }} />
                </div>
                <div className="flex items-center gap-4 text-[10px]" style={{ color: C.muted }}>
                  <span>Evidence: <span style={{ color: C.text }}>{c.evidence}</span></span>
                  <span>Inv: <span style={{ color: C.text }}>{c.investigators}</span></span>
                  <span style={{ color: C.green }}>{c.progress}%</span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Security Status + Live Feed */}
        <div className="col-span-3 flex flex-col">
          {/* Security */}
          <div className="p-6 anim-fade-up" style={{ animationDelay: "360ms", borderBottom: "1px solid rgba(0,217,126,0.08)" }}>
            <div className="mono text-[9px] tracking-[0.2em] mb-4" style={{ color: "rgba(0,92,56,0.82)" }}>SYSTEM SECURITY</div>
            <div className="space-y-2.5">
              {securityItems.map((s, i) => (
                <div key={i} className="flex items-center justify-between">
                  <span className="text-xs" style={{ color: C.muted }}>{s.label}</span>
                  <div className="flex items-center gap-1.5">
                    <div className="w-1.5 h-1.5 rounded-full pulse-dot" style={{ background: C.green }} />
                    <span className="mono text-[9px]" style={{ color: C.green }}>OK</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
          {/* Live feed */}
          <div className="p-6 flex-1 anim-fade-up" style={{ animationDelay: "440ms" }}>
            <div className="mono text-[9px] tracking-[0.2em] mb-4" style={{ color: "rgba(0,92,56,0.82)" }}>LIVE SECURITY EVENTS</div>
            <div className="space-y-3">
              {liveEvents.map((e, i) => (
                <div key={i} className="flex items-start gap-2.5 group">
                  <span className="mono text-[9px] flex-shrink-0 mt-0.5" style={{ color: C.green }}>{e.time}</span>
                  <span className="text-[10px] leading-relaxed" style={{ color: C.muted }}>{e.text}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Bottom: Chart + Chain of Custody + AI */}
      <div className="grid grid-cols-12 gap-0 divide-x" style={{ borderColor: "rgba(0,217,126,0.08)" }}>
        {/* Activity chart */}
        <div className="col-span-5 p-6 anim-fade-up" style={{ animationDelay: "500ms", borderRight: "1px solid rgba(0,217,126,0.08)" }}>
          <div className="mono text-[9px] tracking-[0.2em] mb-4" style={{ color: "rgba(0,92,56,0.82)" }}>INVESTIGATION ACTIVITY — THIS WEEK</div>
          <ResponsiveContainer width="100%" height={180}>
            <AreaChart data={activityData}>
              <defs>
                <linearGradient id="gUploads" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#00D97E" stopOpacity="0.3" />
                  <stop offset="100%" stopColor="#00D97E" stopOpacity="0" />
                </linearGradient>
                <linearGradient id="gViews" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#00B4D8" stopOpacity="0.2" />
                  <stop offset="100%" stopColor="#00B4D8" stopOpacity="0" />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(0,217,126,0.06)" vertical={false} />
              <XAxis dataKey="day" tick={{ fontSize: 9, fill: C.muted, fontFamily: "'JetBrains Mono'" }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fontSize: 9, fill: C.muted }} axisLine={false} tickLine={false} width={24} />
              <Tooltip contentStyle={{ background: "rgba(255,255,255,0.98)", border: "1px solid rgba(0,101,67,0.2)", borderRadius: "8px", fontSize: "11px", color: C.text }} />
              <Area type="monotone" dataKey="uploads" stroke="#00D97E" strokeWidth={1.5} fill="url(#gUploads)" name="Uploads" />
              <Area type="monotone" dataKey="views" stroke="#00B4D8" strokeWidth={1.5} fill="url(#gViews)" name="Views" />
            </AreaChart>
          </ResponsiveContainer>
        </div>

        {/* Chain of Custody preview */}
        <div className="col-span-4 p-6 anim-fade-up" style={{ animationDelay: "580ms", borderRight: "1px solid rgba(0,217,126,0.08)" }}>
          <div className="mono text-[9px] tracking-[0.2em] mb-4" style={{ color: "rgba(0,92,56,0.82)" }}>CHAIN OF CUSTODY — EVD-2048-001</div>
          <div className="space-y-0">
            {["UPLOADED", "VERIFIED", "INVESTIGATOR", "FORENSICS", "LEGAL REVIEW"].map((step, i) => (
              <div key={i} className="flex items-start gap-3">
                <div className="flex flex-col items-center flex-shrink-0">
                  <div className="w-5 h-5 rounded-full flex items-center justify-center mt-0.5 transition-all"
                    style={{
                      background: i < 4 ? "rgba(0,217,126,0.2)" : "rgba(255,255,255,0.05)",
                      border: `1px solid ${i < 4 ? C.green : "rgba(255,255,255,0.1)"}`,
                      boxShadow: i < 4 ? `0 0 8px rgba(0,217,126,0.3)` : "none",
                      animation: `nodeReveal 0.3s ease ${i * 120}ms both`
                    }}>
                    {i < 4 ? <CheckCircle2 size={9} style={{ color: C.green }} /> : <Clock size={9} style={{ color: C.muted }} />}
                  </div>
                  {i < 4 && <div className="w-px flex-shrink-0 my-0.5" style={{ height: "24px", background: i < 3 ? C.green : "rgba(0,217,126,0.2)", opacity: 0.4 }} />}
                </div>
                <div className="pb-2">
                  <div className="mono text-[9px] font-semibold tracking-widest" style={{ color: i < 4 ? C.green : C.muted }}>{step}</div>
                  {i < 4 && <div className="text-[10px]" style={{ color: C.muted }}>Verified · Hash: a3f7b2c9</div>}
                </div>
              </div>
            ))}
          </div>
          <button onClick={() => onNav("evidence-detail")} className="mt-2 text-[10px] flex items-center gap-1 transition-colors" style={{ color: C.green }}
            onMouseEnter={e => (e.currentTarget as HTMLElement).style.textDecoration = "underline"}
            onMouseLeave={e => (e.currentTarget as HTMLElement).style.textDecoration = ""}>
            Full details <ArrowUpRight size={11} />
          </button>
        </div>

        {/* AI Panel */}
        <div className="col-span-3 p-6 flex flex-col anim-slide-right" style={{ animationDelay: "640ms" }}>
          <div className="flex items-center gap-2 mb-4">
            <div className="w-5 h-5 rounded-lg flex items-center justify-center" style={{ background: "rgba(0,217,126,0.15)", border: "1px solid rgba(0,217,126,0.3)" }}>
              <Zap size={10} style={{ color: C.green }} />
            </div>
            <div className="mono text-[9px] tracking-[0.2em]" style={{ color: "rgba(0,92,56,0.82)" }}>INVESTIGATION INTELLIGENCE</div>
          </div>

          <div className="grid grid-cols-2 gap-2 mb-4">
            {[
              { label: "RISK LEVEL", value: "HIGH", color: C.red },
              { label: "EVIDENCE PENDING", value: "17", color: C.amber },
              { label: "GAPS DETECTED", value: "2", color: C.red },
              { label: "NEW FINDINGS", value: "3", color: C.green },
            ].map((k, i) => (
              <div key={i} className="p-2.5 rounded-lg" style={{ background: "rgba(0,217,126,0.04)", border: "1px solid rgba(0,217,126,0.1)" }}>
                <div className="mono text-[8px] tracking-wide mb-1" style={{ color: C.muted }}>{k.label}</div>
                <div className="mono text-sm font-bold" style={{ color: k.color }}>{k.value}</div>
              </div>
            ))}
          </div>

          <div className="flex-1 space-y-2 mb-3 overflow-y-auto">
            {aiMsgs.map((m, i) => (
              <div key={i} className="text-[10px] p-2.5 rounded-lg leading-relaxed"
                style={{ background: "rgba(0,217,126,0.05)", border: "1px solid rgba(0,217,126,0.1)", color: C.muted }}>
                {m}
              </div>
            ))}
          </div>

          <div className="flex gap-2">
            <input value={aiInput} onChange={e => setAiInput(e.target.value)}
              onKeyDown={e => { if (e.key === "Enter" && aiInput.trim()) { setAiMsgs(m => [...m, `Query: "${aiInput}" — Searching evidence database...`]); setAiInput(""); }}}
              className="flex-1 text-[10px] px-3 py-2 rounded-lg outline-none transition-all"
              style={{ background: "rgba(0,217,126,0.05)", border: "1px solid rgba(0,217,126,0.15)", color: C.text }}
              onFocus={e => (e.target as HTMLElement).style.borderColor = "rgba(0,217,126,0.4)"}
              onBlur={e => (e.target as HTMLElement).style.borderColor = "rgba(0,217,126,0.15)"}
              placeholder="Ask about a case or evidence…" />
            <button onClick={() => { if (aiInput.trim()) { setAiMsgs(m => [...m, `Analyzing "${aiInput}"...`]); setAiInput(""); }}}
              className="w-8 h-8 rounded-lg flex items-center justify-center btn-primary flex-shrink-0">
              <Send size={12} />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

// ─── Cases Page ────────────────────────────────────────────────────────────

function CasesPage({ onNav }: { onNav: (p: Page) => void }) {
  const [filter, setFilter] = useState("ALL");
  const [hovered, setHovered] = useState<number | null>(null);

  const filtered = cases.filter(c => filter === "ALL" ||
    (filter === "ACTIVE" && (c.status === "Active" || c.status === "Under Investigation")) ||
    (filter === "REVIEW" && c.status === "Review") ||
    (filter === "CLOSED" && c.status === "Closed")
  );

  const riskColor = (r: string) => r === "CRITICAL" ? C.red : r === "HIGH" ? "#FF7800" : r === "MEDIUM" ? C.amber : r === "LOW" ? C.green : C.muted;

  return (
    <div className="flex-1 overflow-y-auto p-8" style={{ background: C.bg }}>
      <div className="flex items-start justify-between mb-8 anim-fade-up">
        <div>
          <div className="mono text-[9px] tracking-[0.2em] mb-2" style={{ color: "rgba(0,92,56,0.82)" }}>INVESTIGATION WORKSPACE</div>
          <h1 className="text-3xl font-bold tracking-tight" style={{ color: C.text }}>Active Cases</h1>
          <p className="text-sm mt-1" style={{ color: C.muted }}>24 cases active across 6 investigation types</p>
        </div>
        <div className="flex items-center gap-3">
          {["ALL","ACTIVE","REVIEW","CLOSED"].map(f => (
            <button key={f} onClick={() => setFilter(f)}
              className="mono text-[9px] tracking-widest px-3 py-2 rounded-xl transition-all"
              style={filter === f ? { background: "rgba(0,217,126,0.15)", border: "1px solid rgba(0,217,126,0.4)", color: C.green } :
                { background: "rgba(255,255,255,0.03)", border: "1px solid rgba(255,255,255,0.08)", color: C.muted }}>
              {f}
            </button>
          ))}
          <button className="flex items-center gap-2 px-4 py-2 rounded-xl btn-solid text-xs mono tracking-wide">
            <Plus size={13} /> NEW CASE
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-4">
        {filtered.map((c, i) => (
          <div key={i}
            className="relative overflow-hidden rounded-2xl cursor-pointer transition-all duration-300 anim-fade-up"
            style={{
              animationDelay: `${i * 60}ms`,
              background: "rgba(255,255,255,0.86)",
              border: `1px solid ${hovered === i ? riskColor(c.risk) : "rgba(0,217,126,0.1)"}`,
              transform: hovered === i ? "translateY(-4px) scale(1.01)" : undefined,
              boxShadow: hovered === i ? `0 8px 32px rgba(0,0,0,0.4), 0 0 20px ${riskColor(c.risk)}20` : "none",
            }}
            onMouseEnter={() => setHovered(i)}
            onMouseLeave={() => setHovered(null)}
            onClick={() => onNav("evidence-detail")}
          >
            {/* Top accent line */}
            <div className="h-px w-full" style={{ background: `linear-gradient(90deg, ${riskColor(c.risk)}, transparent)` }} />

            <div className="p-5">
              <div className="flex items-start justify-between mb-3">
                <div>
                  <div className="mono text-[9px] tracking-widest mb-1" style={{ color: riskColor(c.risk) }}>{c.id}</div>
                  <h3 className="text-sm font-bold" style={{ color: C.text }}>{c.name}</h3>
                  <div className="text-[10px] mt-0.5" style={{ color: C.muted }}>{c.type}</div>
                </div>
                <Badge status={c.status} size="xs" />
              </div>

              {/* Progress */}
              <div className="mb-4">
                <div className="flex items-center justify-between mb-1">
                  <span className="mono text-[9px]" style={{ color: C.muted }}>Investigation Progress</span>
                  <span className="mono text-[9px] font-semibold" style={{ color: riskColor(c.risk) }}>{c.progress}%</span>
                </div>
                <div className="w-full h-1 rounded-full overflow-hidden" style={{ background: "rgba(255,255,255,0.06)" }}>
                  <div className="h-full rounded-full transition-all" style={{ width: `${c.progress}%`, background: `linear-gradient(90deg, ${riskColor(c.risk)}, ${riskColor(c.risk)}80)` }} />
                </div>
              </div>

              <div className="grid grid-cols-3 gap-3 text-center">
                {[
                  { label: "EVIDENCE", value: c.evidence },
                  { label: "INVESTIGATORS", value: c.investigators },
                  { label: "LAST ACTIVITY", value: c.lastActivity.split(" ").slice(0,2).join(" ") },
                ].map(({ label, value }) => (
                  <div key={label}>
                    <div className="mono text-[8px] tracking-wide mb-0.5" style={{ color: C.muted }}>{label}</div>
                    <div className="text-xs font-semibold" style={{ color: C.text }}>{value}</div>
                  </div>
                ))}
              </div>

              {hovered === i && (
                <div className="mt-4 pt-3 flex items-center justify-between anim-fade-up"
                  style={{ borderTop: "1px solid rgba(0,217,126,0.15)" }}>
                  <div className="flex items-center gap-1.5">
                    <div className="w-1 h-1 rounded-full pulse-dot" style={{ background: riskColor(c.risk) }} />
                    <span className="mono text-[9px]" style={{ color: riskColor(c.risk) }}>RISK: {c.risk}</span>
                  </div>
                  <div className="flex items-center gap-1 text-[10px]" style={{ color: C.green }}>
                    View case <ChevronRight size={11} />
                  </div>
                </div>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// ─── Evidence Page ─────────────────────────────────────────────────────────

function EvidencePage({ onNav }: { onNav: (p: Page) => void }) {
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [hovered, setHovered] = useState<number | null>(null);

  const filtered = evidenceItems.filter(e => statusFilter === "ALL" || e.status.toUpperCase() === statusFilter);
  const statusColor = (s: string) => s === "Verified" ? C.green : s === "Flagged" ? C.red : s === "Pending" ? C.amber : C.teal;

  return (
    <div className="flex-1 overflow-y-auto p-8" style={{ background: C.bg }}>
      <div className="flex items-start justify-between mb-8 anim-fade-up">
        <div>
          <div className="mono text-[9px] tracking-[0.2em] mb-2" style={{ color: "rgba(0,92,56,0.82)" }}>SECURED DIGITAL ASSETS</div>
          <h1 className="text-3xl font-bold tracking-tight" style={{ color: C.text }}>Evidence Vault</h1>
          <p className="text-sm mt-1" style={{ color: C.muted }}>1,248 secured evidence items · All hashes verified</p>
        </div>
        <div className="flex items-center gap-3">
          {["ALL","VERIFIED","PENDING","FLAGGED"].map(f => (
            <button key={f} onClick={() => setStatusFilter(f)}
              className="mono text-[9px] tracking-widest px-3 py-2 rounded-xl transition-all"
              style={statusFilter === f ? { background: "rgba(0,217,126,0.15)", border: "1px solid rgba(0,217,126,0.4)", color: C.green } :
                { background: "rgba(255,255,255,0.03)", border: "1px solid rgba(255,255,255,0.08)", color: C.muted }}>
              {f}
            </button>
          ))}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-4">
        {filtered.map((e, i) => (
          <div key={i}
            className="relative overflow-hidden rounded-2xl cursor-pointer transition-all duration-300 anim-fade-up"
            style={{
              animationDelay: `${i * 60}ms`,
              background: "rgba(255,255,255,0.86)",
              border: `1px solid ${hovered === i ? statusColor(e.status) : "rgba(0,217,126,0.1)"}`,
              transform: hovered === i ? "translateY(-3px)" : undefined,
              boxShadow: hovered === i ? `0 8px 24px rgba(0,0,0,0.3)` : "none",
            }}
            onMouseEnter={() => setHovered(i)}
            onMouseLeave={() => setHovered(null)}
            onClick={() => onNav("evidence-detail")}
          >
            <div className="h-px" style={{ background: `linear-gradient(90deg, ${statusColor(e.status)}, transparent)` }} />
            <div className="p-5">
              <div className="flex items-center justify-between mb-3">
                <span className="mono text-xs font-bold" style={{ color: statusColor(e.status) }}>{e.id}</span>
                <Badge status={e.status} size="xs" />
              </div>
              <div className="text-sm font-bold mb-1" style={{ color: C.text }}>DIGITAL {e.type.toUpperCase()}</div>
              <div className="mono text-[9px] mb-4" style={{ color: C.muted }}>Case: {e.caseId}</div>

              <div className="space-y-1.5 text-[10px]">
                <div className="flex items-center gap-1.5" style={{ color: C.green }}>
                  <CheckCircle2 size={10} /> HASH VERIFIED
                </div>
                <div className="flex items-center gap-1.5" style={{ color: C.green }}>
                  <Lock size={10} /> ENCRYPTED
                </div>
                <div className="flex items-center gap-1.5" style={{ color: C.muted }}>
                  <Link2 size={10} /> CHAIN OF CUSTODY: {e.custody} EVENTS
                </div>
              </div>

              <div className="mt-4 pt-3" style={{ borderTop: "1px solid rgba(0,217,126,0.1)" }}>
                <div className="flex items-center justify-between">
                  <div className="mono text-[9px]" style={{ color: C.muted }}>Hash: {e.hash}</div>
                  <Badge status={e.classification} size="xs" />
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// ─── Evidence Detail ───────────────────────────────────────────────────────

function EvidenceDetailPage({ onBack }: { onBack: () => void }) {
  const [verifying, setVerifying] = useState(false);
  const [verified, setVerified] = useState(false);
  const [progress, setProgress] = useState(0);
  const [hoveredNode, setHoveredNode] = useState<number | null>(null);

  const startVerify = () => {
    setVerifying(true);
    setVerified(false);
    setProgress(0);
    let p = 0;
    const t = setInterval(() => {
      p += Math.random() * 20;
      if (p >= 100) { p = 100; clearInterval(t); setTimeout(() => { setVerifying(false); setVerified(true); }, 300); }
      setProgress(Math.min(p, 100));
    }, 180);
  };

  return (
    <div className="flex-1 overflow-y-auto p-8" style={{ background: C.bg }}>
      <button onClick={onBack} className="flex items-center gap-2 mb-6 text-xs transition-all anim-fade-up" style={{ color: C.muted }}
        onMouseEnter={e => (e.currentTarget as HTMLElement).style.color = C.green}
        onMouseLeave={e => (e.currentTarget as HTMLElement).style.color = C.muted}>
        <ChevronRight className="rotate-180" size={14} /> Back to Evidence Vault
      </button>

      <div className="grid grid-cols-12 gap-6">
        {/* Left: Preview + metadata */}
        <div className="col-span-7 space-y-4">
          {/* Header */}
          <div className="anim-fade-up">
            <div className="mono text-[9px] tracking-[0.2em] mb-2" style={{ color: "rgba(0,92,56,0.82)" }}>SECURED EVIDENCE ASSET</div>
            <div className="flex items-center gap-3 mb-1">
              <h1 className="text-2xl font-bold" style={{ color: C.text }}>EVD-2048-001</h1>
              <Badge status="Verified" />
              <Badge status="Confidential" />
            </div>
            <div className="text-sm" style={{ color: C.muted }}>Financial_Records_Q3.pdf · PDF Document · 2.4 MB</div>
          </div>

          {/* Preview */}
          <div className="rounded-2xl overflow-hidden relative anim-fade-up" style={{ animationDelay: "100ms", border: "1px solid rgba(0,217,126,0.15)", background: "rgba(0,217,126,0.03)" }}>
            <div className="h-48 flex items-center justify-center relative">
              <div className="text-center">
                <FileText size={40} style={{ color: "rgba(0,217,126,0.3)" }} className="mx-auto mb-3" />
                <div className="text-sm" style={{ color: C.muted }}>Preview requires authorization</div>
              </div>
              {/* Grid overlay */}
              <div className="absolute inset-0 tech-grid opacity-50" />
            </div>
          </div>

          {/* Metadata grid */}
          <div className="rounded-2xl p-5 anim-fade-up" style={{ animationDelay: "150ms", background: "rgba(255,255,255,0.86)", border: "1px solid rgba(0,101,67,0.14)" }}>
            <div className="mono text-[9px] tracking-[0.2em] mb-4" style={{ color: "rgba(0,92,56,0.82)" }}>EVIDENCE METADATA</div>
            <div className="grid grid-cols-2 gap-4">
              {[
                { label: "EVIDENCE ID", value: "EVD-2048-001" },
                { label: "CASE ID", value: "CASE-2048" },
                { label: "FILE TYPE", value: "PDF Document" },
                { label: "UPLOAD DATE", value: "Sep 9, 2024 09:14 AM" },
                { label: "UPLOADED BY", value: "Sarah Mitchell" },
                { label: "CURRENT CUSTODIAN", value: "Legal Team" },
                { label: "INTEGRITY STATUS", value: "INTACT" },
                { label: "CLASSIFICATION", value: "CONFIDENTIAL" },
              ].map(({ label, value }) => (
                <div key={label}>
                  <div className="mono text-[8px] tracking-widest mb-0.5" style={{ color: C.muted }}>{label}</div>
                  <div className="text-xs font-semibold" style={{ color: C.text }}>{value}</div>
                </div>
              ))}
              <div className="col-span-2">
                <div className="mono text-[8px] tracking-widest mb-1" style={{ color: C.muted }}>SHA-256 HASH</div>
                <div className="mono text-[10px] p-2.5 rounded-lg break-all" style={{ color: C.green, background: "rgba(0,217,126,0.05)", border: "1px solid rgba(0,217,126,0.15)" }}>
                  a3f7b2c91d84e56a0f3c8d2e9b1a7c4f2d8e6b0a9c3f5e7b2d4a6c8e0f1b3d5
                </div>
              </div>
            </div>
          </div>

          {/* Verify Integrity Button */}
          <div className="anim-fade-up" style={{ animationDelay: "200ms" }}>
            {!verified ? (
              <button onClick={startVerify} disabled={verifying}
                className="w-full py-3.5 rounded-xl font-semibold text-sm transition-all"
                style={verifying ? { background: "rgba(0,217,126,0.08)", border: "1px solid rgba(0,217,126,0.3)", color: C.green } :
                  { background: "rgba(0,217,126,0.12)", border: "1px solid rgba(0,217,126,0.4)", color: C.green }}>
                {verifying ? (
                  <div className="space-y-2">
                    <div className="flex items-center justify-center gap-2 mb-2">
                      <Fingerprint size={16} style={{ color: C.green }} />
                      <span className="mono text-xs tracking-widest">VERIFYING INTEGRITY… {Math.round(progress)}%</span>
                    </div>
                    <div className="w-full h-1 rounded-full overflow-hidden mx-auto" style={{ background: "rgba(0,217,126,0.1)", maxWidth: "300px" }}>
                      <div className="h-full rounded-full transition-all" style={{ width: `${progress}%`, background: C.green }} />
                    </div>
                  </div>
                ) : (
                  <div className="flex items-center justify-center gap-2">
                    <Shield size={15} /> VERIFY INTEGRITY
                  </div>
                )}
              </button>
            ) : (
              <div className="w-full py-3.5 rounded-xl text-center anim-fade-up"
                style={{ background: "rgba(0,217,126,0.08)", border: "1px solid rgba(0,217,126,0.4)" }}>
                <div className="flex items-center justify-center gap-2 mb-0.5" style={{ color: C.green }}>
                  <CheckCircle2 size={16} />
                  <span className="font-bold">EVIDENCE INTEGRITY VERIFIED</span>
                </div>
                <div className="mono text-[10px]" style={{ color: "rgba(0,92,56,0.9)" }}>SHA-256 MATCH CONFIRMED</div>
              </div>
            )}
          </div>
        </div>

        {/* Right: Chain of Custody */}
        <div className="col-span-5 anim-slide-right" style={{ animationDelay: "200ms" }}>
          <div className="rounded-2xl p-5" style={{ background: "rgba(255,255,255,0.86)", border: "1px solid rgba(0,101,67,0.14)" }}>
            <div className="mono text-[9px] tracking-[0.2em] mb-5" style={{ color: "rgba(0,92,56,0.82)" }}>CHAIN OF CUSTODY — TAMPER EVIDENT</div>
            {custodyChain.map((step, i) => (
              <div key={i} className="flex gap-3 cursor-pointer"
                onMouseEnter={() => setHoveredNode(i)}
                onMouseLeave={() => setHoveredNode(null)}>
                <div className="flex flex-col items-center flex-shrink-0">
                  <div className="w-8 h-8 rounded-full flex items-center justify-center transition-all"
                    style={{
                      background: i < 4 ? (hoveredNode === i ? "rgba(0,217,126,0.3)" : "rgba(0,217,126,0.15)") : "rgba(255,255,255,0.04)",
                      border: `1.5px solid ${i < 4 ? C.green : "rgba(255,255,255,0.1)"}`,
                      boxShadow: hoveredNode === i && i < 4 ? `0 0 16px rgba(0,217,126,0.4)` : "none",
                    }}>
                    {i < 4 ? <CheckCircle2 size={13} style={{ color: C.green }} /> : <Clock size={13} style={{ color: C.muted }} />}
                  </div>
                  {i < custodyChain.length - 1 && (
                    <div className="w-px flex-shrink-0 my-0.5" style={{ height: "28px", background: i < 3 ? "rgba(0,217,126,0.3)" : "rgba(255,255,255,0.06)" }} />
                  )}
                </div>
                <div className="pb-4 flex-1 min-w-0">
                  <div className="flex items-start justify-between gap-2">
                    <div className="text-xs font-bold" style={{ color: i < 4 ? C.text : C.muted }}>{step.actor}</div>
                    {hoveredNode === i && i < 4 && (
                      <div className="mono text-[8px] px-1.5 py-0.5 rounded anim-fade-up" style={{ color: C.green, background: "rgba(0,217,126,0.1)", border: "1px solid rgba(0,217,126,0.2)", whiteSpace: "nowrap" }}>VERIFIED</div>
                    )}
                  </div>
                  <div className="mono text-[9px] mb-0.5" style={{ color: C.muted }}>{step.role}</div>
                  <div className="text-[10px] leading-relaxed" style={{ color: C.muted }}>{step.action}</div>
                  {hoveredNode === i && i < 4 && (
                    <div className="mt-1.5 space-y-0.5 anim-fade-up">
                      <div className="mono text-[9px]" style={{ color: C.green }}>Hash: {step.hash}</div>
                      <div className="mono text-[9px]" style={{ color: C.muted }}>Device: {step.device}</div>
                      <div className="mono text-[9px]" style={{ color: C.muted }}>{step.time}</div>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

// ─── Upload Page ───────────────────────────────────────────────────────────

function UploadPage() {
  const [dragging, setDragging] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadDone, setUploadDone] = useState(false);
  const [uploadStep, setUploadStep] = useState(0);
  const [progress, setProgress] = useState(0);

  const steps = ["ENCRYPTING FILE", "GENERATING SHA-256 HASH", "VERIFYING INTEGRITY", "CREATING CUSTODY RECORD"];

  const startUpload = () => {
    setUploading(true); setUploadDone(false); setUploadStep(0); setProgress(0);
    let p = 0, step = 0;
    const t = setInterval(() => {
      p += 8;
      step = Math.floor((p / 100) * 4);
      setUploadStep(Math.min(step, 3));
      setProgress(Math.min(p, 100));
      if (p >= 100) { clearInterval(t); setTimeout(() => { setUploading(false); setUploadDone(true); }, 400); }
    }, 200);
  };

  return (
    <div className="flex-1 overflow-y-auto p-8 flex flex-col items-center" style={{ background: C.bg }}>
      <div className="w-full max-w-xl anim-fade-up">
        <div className="text-center mb-10">
          <div className="mono text-[9px] tracking-[0.2em] mb-2" style={{ color: "rgba(0,92,56,0.82)" }}>SECURE EVIDENCE VAULT</div>
          <h1 className="text-3xl font-bold tracking-tight" style={{ color: C.text }}>Upload Evidence</h1>
          <p className="text-sm mt-2" style={{ color: C.muted }}>All uploads are encrypted, hashed and audit-logged</p>
        </div>

        {/* Drop Zone */}
        <div
          onDragOver={e => { e.preventDefault(); setDragging(true); }}
          onDragLeave={() => setDragging(false)}
          onDrop={e => { e.preventDefault(); setDragging(false); startUpload(); }}
          onClick={() => !uploading && !uploadDone && startUpload()}
          className="relative rounded-2xl p-12 text-center cursor-pointer overflow-hidden transition-all mb-6"
          style={{
            background: dragging ? "rgba(0,130,83,0.1)" : "rgba(255,255,255,0.86)",
            border: `1.5px dashed ${dragging ? C.green : "rgba(0,217,126,0.25)"}`,
            boxShadow: dragging ? `0 0 40px rgba(0,217,126,0.12)` : "none",
          }}>
          {/* Animated rings */}
          {(dragging || uploading) && [...Array(3)].map((_, i) => (
            <div key={i} className="absolute rounded-full border pointer-events-none"
              style={{
                width: `${120 + i * 60}px`, height: `${120 + i * 60}px`,
                top: "50%", left: "50%", transform: "translate(-50%,-50%)",
                borderColor: `rgba(0,217,126,${0.2 - i * 0.05})`,
                animation: `pulse ${1.5 + i * 0.3}s ease-in-out infinite`,
              }} />
          ))}

          <div className="absolute inset-0 tech-grid opacity-30" />

          <div className="relative z-10">
            <div className="w-16 h-16 rounded-2xl mx-auto mb-5 flex items-center justify-center glow-pulse"
              style={{ background: "rgba(0,217,126,0.1)", border: "1px solid rgba(0,217,126,0.3)" }}>
              {uploadDone ? <CheckCircle2 size={28} style={{ color: C.green }} /> :
                uploading ? <Fingerprint size={28} style={{ color: C.green }} /> :
                  <Upload size={28} style={{ color: C.green }} />}
            </div>

            {uploadDone ? (
              <div>
                <div className="text-xl font-bold mb-1" style={{ color: C.green }}>EVIDENCE SECURED</div>
                <div className="mono text-[10px] tracking-widest" style={{ color: "rgba(0,92,56,0.9)" }}>SHA-256 VERIFIED · CUSTODY RECORD CREATED</div>
              </div>
            ) : uploading ? (
              <div>
                <div className="mono text-sm font-semibold mb-4" style={{ color: C.green }}>{steps[uploadStep]}</div>
                <div className="w-48 h-1 rounded-full overflow-hidden mx-auto mb-2" style={{ background: "rgba(0,217,126,0.1)" }}>
                  <div className="h-full rounded-full transition-all" style={{ width: `${progress}%`, background: C.green }} />
                </div>
                <div className="space-y-1 mt-4">
                  {steps.map((s, i) => (
                    <div key={i} className="flex items-center justify-center gap-2 mono text-[9px]"
                      style={{ color: i <= uploadStep ? C.green : C.muted, opacity: i <= uploadStep ? 1 : 0.4 }}>
                      {i < uploadStep ? <CheckCircle2 size={9} /> : i === uploadStep ? <RefreshCw size={9} style={{ animation: "spin 1s linear infinite" }} /> : <Circle size={9} />}
                      {s}
                    </div>
                  ))}
                </div>
              </div>
            ) : (
              <div>
                <div className="text-xl font-bold mb-1" style={{ color: C.text }}>DROP EVIDENCE</div>
                <p className="text-sm mb-4" style={{ color: C.muted }}>Upload documents, images, videos, audio and forensic files securely</p>
                <button className="px-5 py-2.5 rounded-xl btn-primary text-xs mono tracking-widest">
                  BROWSE FILES
                </button>
                <p className="mono text-[9px] mt-4" style={{ color: "rgba(0,92,56,0.76)" }}>PDF · DOCX · JPG · PNG · MP4 · MP3 · Max 500 MB</p>
              </div>
            )}
          </div>
        </div>

        {/* Security badges */}
        <div className="grid grid-cols-3 gap-3">
          {[
            { icon: Lock, label: "AES-256 ENCRYPTED" },
            { icon: Fingerprint, label: "AUTO SHA-256 HASH" },
            { icon: ShieldCheck, label: "INTEGRITY VERIFIED" },
          ].map(({ icon: Icon, label }) => (
            <div key={label} className="p-3 rounded-xl text-center" style={{ background: "rgba(0,217,126,0.04)", border: "1px solid rgba(0,217,126,0.1)" }}>
              <Icon size={16} className="mx-auto mb-1.5" style={{ color: C.green }} />
              <div className="mono text-[8px] tracking-widest" style={{ color: C.muted }}>{label}</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// ─── Audit Trail ────────────────────────────────────────────────────────────

function AuditPage() {
  return (
    <div className="flex-1 overflow-y-auto p-8" style={{ background: C.bg }}>
      <div className="flex items-start justify-between mb-8 anim-fade-up">
        <div>
          <div className="mono text-[9px] tracking-[0.2em] mb-2" style={{ color: "rgba(0,92,56,0.82)" }}>IMMUTABLE FORENSIC LOG</div>
          <h1 className="text-3xl font-bold tracking-tight" style={{ color: C.text }}>Audit Trail</h1>
          <p className="text-sm mt-1" style={{ color: C.muted }}>Tamper-evident record of all system events and user actions</p>
        </div>
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2 mono text-[10px] px-3 py-2 rounded-xl"
            style={{ background: "rgba(0,217,126,0.08)", border: "1px solid rgba(0,217,126,0.2)", color: C.green }}>
            <div className="w-1.5 h-1.5 rounded-full pulse-dot" style={{ background: C.green }} />
            LIVE MONITORING
          </div>
          <button className="flex items-center gap-2 px-4 py-2 rounded-xl btn-primary mono text-[9px] tracking-widest">
            <Download size={12} /> EXPORT LOG
          </button>
        </div>
      </div>

      <div className="space-y-2">
        {auditEvents.map((e, i) => (
          <div key={i}
            className="relative flex items-start gap-4 p-4 rounded-xl transition-all anim-fade-up"
            style={{
              animationDelay: `${i * 60}ms`,
              background: e.result === "failed" ? "rgba(255,68,85,0.06)" : "rgba(255,255,255,0.86)",
              border: `1px solid ${e.result === "failed" ? "rgba(255,68,85,0.3)" : "rgba(0,217,126,0.1)"}`,
            }}>
            {/* Left accent */}
            <div className="w-1 self-stretch rounded-full flex-shrink-0"
              style={{ background: e.result === "failed" ? C.red : C.green, opacity: 0.6 }} />

            {/* Time */}
            <div className="mono text-xs font-bold flex-shrink-0 mt-0.5 w-12" style={{ color: e.result === "failed" ? C.red : C.green }}>
              {e.time}
            </div>

            {/* Content */}
            <div className="flex-1 min-w-0">
              <div className="text-sm font-semibold mb-1" style={{ color: e.result === "failed" ? "#FF6677" : C.text }}>{e.action}</div>
              <div className="flex items-center gap-4 text-[10px]" style={{ color: C.muted }}>
                <span><span style={{ color: C.text }}>{e.user}</span></span>
                <span className="mono">{e.target}</span>
                <span className="mono">{e.ip}</span>
                <span>{e.device}</span>
              </div>
            </div>

            {/* Result */}
            <Badge status={e.result} size="xs" />
          </div>
        ))}
      </div>
    </div>
  );
}

// ─── Reports Page ──────────────────────────────────────────────────────────

function ReportsPage() {
  return (
    <div className="flex-1 overflow-y-auto p-8" style={{ background: C.bg }}>
      <div className="flex items-start justify-between mb-8 anim-fade-up">
        <div>
          <div className="mono text-[9px] tracking-[0.2em] mb-2" style={{ color: "rgba(0,92,56,0.82)" }}>COMMAND CENTER ANALYTICS</div>
          <h1 className="text-3xl font-bold tracking-tight" style={{ color: C.text }}>Reports</h1>
          <p className="text-sm mt-1" style={{ color: C.muted }}>Forensic analytics and exportable intelligence reports</p>
        </div>
        <button className="flex items-center gap-2 px-4 py-2.5 rounded-xl btn-solid text-xs mono tracking-widest">
          <Download size={13} /> EXPORT REPORT
        </button>
      </div>

      <div className="grid grid-cols-4 gap-4 mb-6">
        {[
          { label: "TOTAL CASES", value: "24", icon: FolderOpen, color: C.green },
          { label: "EVIDENCE ITEMS", value: "1,248", icon: Shield, color: C.teal },
          { label: "VERIFICATIONS", value: "3,812", icon: CheckCircle2, color: C.green },
          { label: "CUSTODY EVENTS", value: "9,204", icon: Link2, color: C.amber },
        ].map(({ label, value, icon: Icon, color }, i) => (
          <div key={i} className="p-5 rounded-2xl anim-count glass-card" style={{ animationDelay: `${i * 60}ms` }}>
            <div className="w-8 h-8 rounded-xl flex items-center justify-center mb-3" style={{ background: `${color}18`, border: `1px solid ${color}40` }}>
              <Icon size={14} style={{ color }} />
            </div>
            <div className="text-3xl font-bold mono mb-1" style={{ color }}>{value}</div>
            <div className="mono text-[9px] tracking-widest" style={{ color: C.muted }}>{label}</div>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-2 gap-6">
        <div className="p-5 rounded-2xl glass-card anim-fade-up" style={{ animationDelay: "200ms" }}>
          <div className="mono text-[9px] tracking-[0.2em] mb-4" style={{ color: "rgba(0,92,56,0.82)" }}>CASE & EVIDENCE TREND</div>
          <ResponsiveContainer width="100%" height={200}>
            <LineChart data={reportTrendData}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(0,217,126,0.06)" vertical={false} />
              <XAxis dataKey="month" tick={{ fontSize: 9, fill: C.muted, fontFamily: "'JetBrains Mono'" }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fontSize: 9, fill: C.muted }} axisLine={false} tickLine={false} width={28} />
              <Tooltip contentStyle={{ background: "rgba(255,255,255,0.98)", border: "1px solid rgba(0,101,67,0.2)", borderRadius: "8px", fontSize: "11px", color: C.text }} />
              <Line type="monotone" dataKey="cases" stroke={C.green} strokeWidth={2} dot={false} name="Cases" />
              <Line type="monotone" dataKey="evidence" stroke={C.teal} strokeWidth={2} dot={false} name="Evidence" />
            </LineChart>
          </ResponsiveContainer>
        </div>

        <div className="p-5 rounded-2xl glass-card anim-fade-up" style={{ animationDelay: "260ms" }}>
          <div className="mono text-[9px] tracking-[0.2em] mb-4" style={{ color: "rgba(0,92,56,0.82)" }}>WEEKLY ACTIVITY</div>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={activityData}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(0,217,126,0.06)" vertical={false} />
              <XAxis dataKey="day" tick={{ fontSize: 9, fill: C.muted, fontFamily: "'JetBrains Mono'" }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fontSize: 9, fill: C.muted }} axisLine={false} tickLine={false} width={24} />
              <Tooltip contentStyle={{ background: "rgba(255,255,255,0.98)", border: "1px solid rgba(0,101,67,0.2)", borderRadius: "8px", fontSize: "11px", color: C.text }} />
              <Bar dataKey="uploads" fill={C.green} radius={[3,3,0,0]} fillOpacity={0.8} name="Uploads" />
              <Bar dataKey="verifications" fill={C.teal} radius={[3,3,0,0]} fillOpacity={0.8} name="Verifications" />
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="p-5 rounded-2xl glass-card col-span-2 anim-fade-up" style={{ animationDelay: "320ms" }}>
          <div className="mono text-[9px] tracking-[0.2em] mb-4" style={{ color: "rgba(0,92,56,0.82)" }}>SECURITY INCIDENTS — LAST 30 DAYS</div>
          <div className="space-y-2">
            {[
              { event: "Failed login attempts", count: 7, color: C.red },
              { event: "Unauthorized access attempts", count: 2, color: C.red },
              { event: "Hash verification failures", count: 0, color: C.green },
              { event: "Chain of custody violations", count: 1, color: C.amber },
              { event: "Suspicious download patterns", count: 3, color: C.amber },
            ].map((s, i) => (
              <div key={i} className="flex items-center justify-between py-2.5 px-4 rounded-xl transition-colors"
                style={{ background: "rgba(0,217,126,0.03)", border: "1px solid rgba(0,217,126,0.08)" }}>
                <span className="text-sm" style={{ color: C.muted }}>{s.event}</span>
                <div className="flex items-center gap-3">
                  <div className="w-24 h-1 rounded-full overflow-hidden" style={{ background: "rgba(255,255,255,0.06)" }}>
                    <div className="h-full rounded-full" style={{ width: `${(s.count / 7) * 100}%`, background: s.color }} />
                  </div>
                  <span className="mono text-xs font-bold" style={{ color: s.color }}>{s.count}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

// ─── Users Page ────────────────────────────────────────────────────────────

function UsersPage() {
  const users = [
    { name: "Sarah Mitchell", email: "s.mitchell@siv.gov", role: "Administrator", cases: 8, lastLogin: "Today, 9:00 AM", status: "Active" },
    { name: "James Rodriguez", email: "j.rodriguez@siv.gov", role: "Investigator", cases: 5, lastLogin: "Today, 8:52 AM", status: "Active" },
    { name: "Priya Sharma", email: "p.sharma@siv.gov", role: "Forensic Analyst", cases: 4, lastLogin: "Yesterday", status: "Active" },
    { name: "Alex Chen", email: "a.chen@siv.gov", role: "Investigator", cases: 6, lastLogin: "2 days ago", status: "Active" },
    { name: "Marcus Wilson", email: "m.wilson@siv.gov", role: "Legal Counsel", cases: 3, lastLogin: "3 days ago", status: "Inactive" },
  ];

  return (
    <div className="flex-1 overflow-y-auto p-8" style={{ background: C.bg }}>
      <div className="flex items-start justify-between mb-8 anim-fade-up">
        <div>
          <div className="mono text-[9px] tracking-[0.2em] mb-2" style={{ color: "rgba(0,92,56,0.82)" }}>ACCESS MANAGEMENT</div>
          <h1 className="text-3xl font-bold tracking-tight" style={{ color: C.text }}>Users</h1>
          <p className="text-sm mt-1" style={{ color: C.muted }}>Manage team access and investigation permissions</p>
        </div>
        <button className="flex items-center gap-2 px-4 py-2.5 rounded-xl btn-solid text-xs mono tracking-widest">
          <Plus size={13} /> INVITE USER
        </button>
      </div>

      <div className="space-y-3">
        {users.map((u, i) => (
          <div key={i} className="flex items-center gap-5 p-4 rounded-xl transition-all anim-fade-up glass-card" style={{ animationDelay: `${i * 60}ms` }}>
            <div className="w-10 h-10 rounded-full flex items-center justify-center text-sm font-bold flex-shrink-0"
              style={{ background: "rgba(0,217,126,0.15)", color: C.green, border: "1px solid rgba(0,217,126,0.3)" }}>
              {u.name.split(" ").map(n => n[0]).join("")}
            </div>
            <div className="flex-1 min-w-0">
              <div className="text-sm font-semibold" style={{ color: C.text }}>{u.name}</div>
              <div className="mono text-[10px]" style={{ color: C.muted }}>{u.email}</div>
            </div>
            <div className="text-xs" style={{ color: C.muted }}>{u.role}</div>
            <div className="text-center">
              <div className="text-sm font-bold" style={{ color: C.text }}>{u.cases}</div>
              <div className="mono text-[8px]" style={{ color: C.muted }}>CASES</div>
            </div>
            <div className="mono text-[10px]" style={{ color: C.muted }}>{u.lastLogin}</div>
            <Badge status={u.status} size="xs" />
            <div className="flex items-center gap-2">
              <button className="w-8 h-8 rounded-xl flex items-center justify-center transition-all"
                style={{ background: "rgba(255,255,255,0.04)", border: "1px solid rgba(255,255,255,0.08)", color: C.muted }}>
                <Edit3 size={13} />
              </button>
              <button className="w-8 h-8 rounded-xl flex items-center justify-center transition-all"
                style={{ background: "rgba(255,68,85,0.06)", border: "1px solid rgba(255,68,85,0.2)", color: C.red }}>
                <Trash2 size={13} />
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// ─── Settings Page ─────────────────────────────────────────────────────────

function SettingsPage({ onLogout }: { onLogout: () => void }) {
  const groups = [
    { title: "GENERAL", items: ["Workspace Name", "Time Zone", "Language", "Date Format"] },
    { title: "SECURITY", items: ["Two-Factor Authentication", "Session Timeout", "IP Allowlist", "Password Policy"] },
    { title: "EVIDENCE", items: ["Auto Hash Algorithm", "Encryption Standard", "Retention Policy", "Max Upload Size"] },
    { title: "NOTIFICATIONS", items: ["Email Alerts", "Security Events", "Evidence Updates", "Case Assignments"] },
  ];

  return (
    <div className="flex-1 overflow-y-auto p-8" style={{ background: C.bg }}>
      <div className="mb-8 anim-fade-up flex items-start justify-between gap-6">
        <div>
          <div className="mono text-[9px] tracking-[0.2em] mb-2" style={{ color: "rgba(0,92,56,0.82)" }}>SYSTEM CONFIGURATION</div>
          <h1 className="text-3xl font-bold tracking-tight" style={{ color: "#176B45" }}>Settings</h1>
          <p className="text-sm mt-1" style={{ color: "#435C50" }}>Configure workspace preferences and security parameters</p>
        </div>
        <button type="button" onClick={onLogout} className="flex items-center gap-2 rounded-xl px-4 py-2.5 text-xs font-semibold transition-all"
          style={{ color: "#A8323D", background: "rgba(255,68,85,0.06)", border: "1px solid rgba(168,50,61,0.25)" }}
          onMouseEnter={e => { (e.currentTarget as HTMLElement).style.background = "rgba(255,68,85,0.12)"; }}
          onMouseLeave={e => { (e.currentTarget as HTMLElement).style.background = "rgba(255,68,85,0.06)"; }}>
          <LogOut size={14} /> LOG OUT
        </button>
      </div>

      <div className="grid grid-cols-2 gap-5 max-w-3xl">
        {groups.map(({ title, items }, si) => (
          <div key={si} className="rounded-2xl overflow-hidden glass-card anim-fade-up" style={{ animationDelay: `${si * 80}ms` }}>
            <div className="px-5 py-3 mono text-[9px] tracking-widest" style={{ color: "#176B45", borderBottom: "1px solid rgba(0,101,67,0.14)", background: "rgba(0,130,83,0.04)" }}>
              {title}
            </div>
            <div className="divide-y" style={{ borderColor: "rgba(0,101,67,0.08)" }}>
              {items.map((item, ii) => (
                <div key={ii} className="flex items-center justify-between px-5 py-3.5 cursor-pointer transition-all group"
                  style={{ color: "#334D41" }}
                  onMouseEnter={e => { (e.currentTarget as HTMLElement).style.background = "rgba(0,130,83,0.05)"; (e.currentTarget as HTMLElement).style.color = "#176B45"; }}
                  onMouseLeave={e => { (e.currentTarget as HTMLElement).style.background = "transparent"; (e.currentTarget as HTMLElement).style.color = "#334D41"; }}>
                  <span className="text-sm">{item}</span>
                  <ChevronRight size={13} style={{ color: "#176B45" }} />
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// ─── App ───────────────────────────────────────────────────────────────────

export default function App() {
  const [showIntro, setShowIntro] = useState(true);
  const [authenticated, setAuthenticated] = useState(false);
  const [page, setPage] = useState<Page>("dashboard");
  const [collapsed, setCollapsed] = useState(false);

  useEffect(() => {
    try {
      const stored = getStoredAuthSession();
      if (stored?.accessToken) {
        setAuthenticated(true);
      }
    } catch {
      // ignore invalid local storage state
    }
  }, []);

  return (
    <div className="h-full flex overflow-hidden" style={{ background: C.bg }}>
      {showIntro && <IntroScreen onDone={() => setShowIntro(false)} />}

      {!showIntro && !authenticated && <LoginScreen onLogin={() => setAuthenticated(true)} />}

      {!showIntro && authenticated && (
        <>
          <Sidebar current={page} onNav={setPage} collapsed={collapsed} onToggle={() => setCollapsed(c => !c)} />
          <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
            <Header page={page} onNav={setPage} />
            <PageTransition page={page}>
              {page === "dashboard" && <DashboardPage onNav={setPage} />}
              {page === "cases" && <CasesPage onNav={setPage} />}
              {page === "evidence" && <EvidencePage onNav={setPage} />}
              {page === "evidence-detail" && <EvidenceDetailPage onBack={() => setPage("evidence")} />}
              {page === "upload" && <UploadPage />}
              {page === "audit" && <AuditPage />}
              {page === "reports" && <ReportsPage />}
              {page === "users" && <UsersPage />}
              {page === "settings" && <SettingsPage onLogout={async () => { setPage("dashboard"); setAuthenticated(false); await api.auth.logout(); clearStoredAuthSession(); }} />}
            </PageTransition>
          </div>
        </>
      )}
    </div>
  );
}
