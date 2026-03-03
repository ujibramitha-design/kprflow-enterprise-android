"use client"

import { Wallet, TrendingUp, Percent, Clock3 } from "lucide-react"

const summaryItems = [
  {
    label: "Total Plafon Disetujui",
    value: "Rp 128,4 M",
    sub: "Bulan Maret 2026",
    icon: Wallet,
    accentClass: "from-primary/15 to-primary/5 text-primary",
  },
  {
    label: "Rata-rata Approval Rate",
    value: "68,6%",
    sub: "+4.2% dari bulan lalu",
    icon: Percent,
    accentClass: "from-emerald-500/15 to-emerald-500/5 text-emerald-600 dark:text-emerald-400",
  },
  {
    label: "Rata-rata Waktu Proses",
    value: "3,2 Hari",
    sub: "-0.5 hari dari bulan lalu",
    icon: Clock3,
    accentClass: "from-amber-500/15 to-amber-500/5 text-amber-600 dark:text-amber-400",
  },
  {
    label: "Target Kuartal",
    value: "82%",
    sub: "1.024 dari 1.248 target",
    icon: TrendingUp,
    accentClass: "from-sky-500/15 to-sky-500/5 text-sky-600 dark:text-sky-400",
  },
]

export function QuickStats() {
  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {summaryItems.map((item, index) => (
        <div
          key={item.label}
          className="flex items-center gap-4 rounded-2xl bg-card p-4 shadow-sm ring-1 ring-border/30 transition-all duration-300 hover:shadow-md animate-scale-in"
          style={{ animationDelay: `${index * 80}ms` }}
        >
          <div
            className={`flex size-12 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br ${item.accentClass}`}
          >
            <item.icon className="size-5" strokeWidth={2} />
          </div>
          <div className="min-w-0">
            <p className="text-[11px] font-semibold text-muted-foreground/70 uppercase tracking-wide truncate">
              {item.label}
            </p>
            <p className="mt-0.5 text-lg font-extrabold text-foreground leading-tight">
              {item.value}
            </p>
            <p className="mt-0.5 text-[11px] text-muted-foreground">
              {item.sub}
            </p>
          </div>
        </div>
      ))}
    </div>
  )
}
