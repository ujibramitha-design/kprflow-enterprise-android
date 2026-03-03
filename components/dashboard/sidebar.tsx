"use client"

import { useState } from "react"
import {
  LayoutDashboard,
  FileText,
  Building2,
  Users,
  Landmark,
  Settings,
  ChevronLeft,
  ChevronRight,
  HelpCircle,
  LogOut,
} from "lucide-react"
import { cn } from "@/lib/utils"
import { Logo3D } from "@/components/dashboard/logo-3d"

const menuItems = [
  { icon: LayoutDashboard, label: "Dashboard", active: true },
  { icon: FileText, label: "Aplikasi KPR", active: false },
  { icon: Building2, label: "Data Unit", active: false },
  { icon: Users, label: "Customer", active: false },
  { icon: Landmark, label: "Bank Mitra", active: false },
  { icon: Settings, label: "Pengaturan", active: false },
]

export function DashboardSidebar() {
  const [collapsed, setCollapsed] = useState(false)

  return (
    <aside
      className={cn(
        "relative flex h-screen flex-col border-r border-border/40 transition-all duration-500 ease-in-out",
        "bg-gradient-to-b from-card via-card to-secondary/30 dark:from-[oklch(0.15_0.018_260)] dark:via-[oklch(0.15_0.018_260)] dark:to-[oklch(0.13_0.02_260)]",
        collapsed ? "w-[76px]" : "w-[260px]"
      )}
    >
      {/* Logo area */}
      <div className={cn(
        "flex items-center gap-3 px-5 pt-7 pb-6",
        collapsed && "justify-center px-0"
      )}>
        <Logo3D collapsed={collapsed} />
      </div>

      {/* Section label */}
      {!collapsed && (
        <div className="px-5 pb-2">
          <p className="text-[10px] font-bold tracking-[0.18em] uppercase text-muted-foreground/50">
            Menu Utama
          </p>
        </div>
      )}

      {/* Navigation */}
      <nav className={cn("flex flex-1 flex-col gap-1", collapsed ? "px-3" : "px-3")}>
        {menuItems.map((item, index) => (
          <button
            key={item.label}
            className={cn(
              "group relative flex items-center gap-3 rounded-xl px-3 py-2.5 text-[13px] font-semibold transition-all duration-300 animate-slide-in-right",
              collapsed && "justify-center px-0",
              item.active
                ? "bg-primary/[0.08] text-primary"
                : "text-muted-foreground hover:bg-secondary/80 hover:text-foreground"
            )}
            style={{ animationDelay: `${index * 60}ms` }}
          >
            {/* Active indicator bar */}
            {item.active && (
              <div className="absolute left-0 top-1/2 h-5 w-[3px] -translate-y-1/2 rounded-r-full bg-primary" />
            )}
            <div
              className={cn(
                "flex size-8 shrink-0 items-center justify-center rounded-lg transition-all duration-300",
                item.active
                  ? "bg-primary/10"
                  : "group-hover:bg-secondary"
              )}
            >
              <item.icon
                className={cn(
                  "size-[18px] transition-all duration-300",
                  item.active
                    ? "text-primary"
                    : "text-muted-foreground/70 group-hover:text-foreground"
                )}
                strokeWidth={item.active ? 2.2 : 1.8}
              />
            </div>
            {!collapsed && <span>{item.label}</span>}
          </button>
        ))}
      </nav>

      {/* Collapse toggle */}
      <button
        onClick={() => setCollapsed(!collapsed)}
        className="absolute -right-3.5 top-[72px] z-10 flex size-7 items-center justify-center rounded-full border border-border/60 bg-card text-muted-foreground shadow-sm transition-all duration-300 hover:bg-secondary hover:text-foreground hover:shadow-md"
      >
        {collapsed ? (
          <ChevronRight className="size-3.5" />
        ) : (
          <ChevronLeft className="size-3.5" />
        )}
      </button>

      {/* Bottom section */}
      <div className={cn("px-3 pb-4", collapsed && "px-2")}>
        {/* Help card */}
        {!collapsed && (
          <div className="mb-3 rounded-2xl bg-gradient-to-br from-primary/[0.06] to-primary/[0.02] p-4">
            <div className="flex size-8 items-center justify-center rounded-xl bg-primary/10 mb-2.5">
              <HelpCircle className="size-4 text-primary" strokeWidth={2} />
            </div>
            <p className="text-xs font-bold text-foreground leading-tight">Pusat Bantuan</p>
            <p className="mt-1 text-[11px] text-muted-foreground leading-relaxed">
              Ada pertanyaan? Tim kami siap membantu 24/7.
            </p>
            <button className="mt-3 flex h-8 w-full items-center justify-center rounded-xl bg-primary text-primary-foreground text-[11px] font-bold tracking-wide transition-all duration-300 hover:bg-primary/90 hover:shadow-md hover:shadow-primary/20">
              Hubungi Support
            </button>
          </div>
        )}

        {/* Logout */}
        <button
          className={cn(
            "flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-[13px] font-semibold text-muted-foreground transition-all duration-300 hover:bg-destructive/5 hover:text-destructive",
            collapsed && "justify-center px-0"
          )}
        >
          <div className="flex size-8 shrink-0 items-center justify-center rounded-lg">
            <LogOut className="size-[18px]" strokeWidth={1.8} />
          </div>
          {!collapsed && <span>Keluar</span>}
        </button>
      </div>
    </aside>
  )
}
