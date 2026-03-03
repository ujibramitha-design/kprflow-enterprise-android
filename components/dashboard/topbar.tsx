"use client"

import { Search, Bell, Command, MessageSquare } from "lucide-react"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { ThemeToggle } from "@/components/dashboard/theme-toggle"
import { LiveClock } from "@/components/dashboard/live-clock"

export function DashboardTopbar() {
  return (
    <header className="flex h-[64px] items-center justify-between bg-card/50 px-8 backdrop-blur-xl border-b border-border/30">
      {/* Left: Breadcrumb + Clock */}
      <div className="flex items-center gap-4">
        <nav className="flex items-center gap-1.5" aria-label="Breadcrumb">
          <span className="text-[13px] font-bold text-foreground">Dashboard</span>
          <span className="text-muted-foreground/40 select-none">/</span>
          <span className="text-[13px] font-medium text-muted-foreground">Ringkasan</span>
        </nav>
        <div className="hidden h-5 w-px bg-border/40 lg:block" />
        <div className="hidden lg:block">
          <LiveClock />
        </div>
      </div>

      {/* Right */}
      <div className="flex items-center gap-3">
        {/* Search */}
        <div className="relative hidden md:flex">
          <Search className="absolute left-3 top-1/2 size-[15px] -translate-y-1/2 text-muted-foreground/60" />
          <input
            type="text"
            placeholder="Cari data..."
            className="h-9 w-52 rounded-xl border-0 bg-secondary/60 pl-9 pr-16 text-[13px] text-foreground placeholder:text-muted-foreground/50 outline-none transition-all duration-300 focus:bg-secondary focus:ring-2 focus:ring-primary/15 focus:shadow-sm"
          />
          <div className="absolute right-2.5 top-1/2 flex -translate-y-1/2 items-center gap-0.5 rounded-md bg-background/80 px-1.5 py-0.5 border border-border/40">
            <Command className="size-3 text-muted-foreground/50" />
            <span className="text-[10px] font-semibold text-muted-foreground/50">K</span>
          </div>
        </div>

        {/* Divider */}
        <div className="hidden h-6 w-px bg-border/50 md:block" />

        {/* Theme toggle */}
        <ThemeToggle />

        {/* Messages */}
        <button
          className="relative flex size-9 items-center justify-center rounded-xl bg-secondary/50 text-muted-foreground transition-all duration-300 hover:bg-secondary hover:text-foreground hover:shadow-sm"
          aria-label="Pesan masuk"
        >
          <MessageSquare className="size-[15px]" strokeWidth={1.8} />
        </button>

        {/* Notifications */}
        <button
          className="relative flex size-9 items-center justify-center rounded-xl bg-secondary/50 text-muted-foreground transition-all duration-300 hover:bg-secondary hover:text-foreground hover:shadow-sm"
          aria-label="Notifikasi"
        >
          <Bell className="size-[15px]" strokeWidth={1.8} />
          <span className="absolute right-1.5 top-1.5 flex size-2 items-center justify-center rounded-full bg-red-400 ring-2 ring-card">
            <span className="absolute size-full rounded-full bg-red-400" style={{ animation: "dot-ping 2s cubic-bezier(0, 0, 0.2, 1) infinite" }} />
          </span>
        </button>

        {/* Divider */}
        <div className="h-6 w-px bg-border/50" />

        {/* Profile */}
        <button className="flex items-center gap-2.5 rounded-xl px-2 py-1.5 transition-all duration-300 hover:bg-secondary/60">
          <div className="hidden text-right sm:block">
            <p className="text-[13px] font-bold text-foreground leading-tight">Analis Kredit</p>
            <p className="text-[11px] font-medium text-muted-foreground leading-tight">admin@kprflow.id</p>
          </div>
          <Avatar className="size-9 ring-2 ring-primary/10 ring-offset-2 ring-offset-card">
            <AvatarImage
              src="https://api.dicebear.com/9.x/notionists/svg?seed=Felix"
              alt="Avatar pengguna"
            />
            <AvatarFallback className="bg-gradient-to-br from-primary/20 to-primary/10 text-primary text-[11px] font-bold">
              AK
            </AvatarFallback>
          </Avatar>
        </button>
      </div>
    </header>
  )
}
