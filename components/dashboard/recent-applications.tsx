"use client"

import { Eye, MoreHorizontal, ArrowUpRight, Filter } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { cn } from "@/lib/utils"

type Status = "Disetujui" | "Review" | "Ditolak" | "Proses Verifikasi"

const statusConfig: Record<Status, { bg: string; text: string; dot: string }> = {
  Disetujui: {
    bg: "bg-emerald-500/10 dark:bg-emerald-500/15",
    text: "text-emerald-700 dark:text-emerald-400",
    dot: "bg-emerald-500",
  },
  Review: {
    bg: "bg-amber-500/10 dark:bg-amber-500/15",
    text: "text-amber-700 dark:text-amber-400",
    dot: "bg-amber-500",
  },
  Ditolak: {
    bg: "bg-rose-500/10 dark:bg-rose-500/15",
    text: "text-rose-600 dark:text-rose-400",
    dot: "bg-rose-500",
  },
  "Proses Verifikasi": {
    bg: "bg-sky-500/10 dark:bg-sky-500/15",
    text: "text-sky-700 dark:text-sky-400",
    dot: "bg-sky-500",
  },
}

const applications = [
  {
    name: "Ahmad Fauzi",
    initials: "AF",
    nik: "3201****5678",
    plafon: "Rp 450.000.000",
    tenor: "20 Tahun",
    status: "Disetujui" as Status,
    tanggal: "28 Feb 2026",
    avatarSeed: "Ahmad",
  },
  {
    name: "Siti Nurhaliza",
    initials: "SN",
    nik: "3202****9012",
    plafon: "Rp 620.000.000",
    tenor: "25 Tahun",
    status: "Review" as Status,
    tanggal: "27 Feb 2026",
    avatarSeed: "Siti",
  },
  {
    name: "Budi Santoso",
    initials: "BS",
    nik: "3203****3456",
    plafon: "Rp 380.000.000",
    tenor: "15 Tahun",
    status: "Proses Verifikasi" as Status,
    tanggal: "26 Feb 2026",
    avatarSeed: "Budi",
  },
  {
    name: "Dewi Kartika",
    initials: "DK",
    nik: "3204****7890",
    plafon: "Rp 550.000.000",
    tenor: "20 Tahun",
    status: "Ditolak" as Status,
    tanggal: "25 Feb 2026",
    avatarSeed: "Dewi",
  },
  {
    name: "Reza Mahendra",
    initials: "RM",
    nik: "3205****2345",
    plafon: "Rp 780.000.000",
    tenor: "25 Tahun",
    status: "Disetujui" as Status,
    tanggal: "24 Feb 2026",
    avatarSeed: "Reza",
  },
  {
    name: "Anisa Rahma",
    initials: "AR",
    nik: "3206****6789",
    plafon: "Rp 420.000.000",
    tenor: "15 Tahun",
    status: "Review" as Status,
    tanggal: "23 Feb 2026",
    avatarSeed: "Anisa",
  },
]

const avatarColors = [
  "from-primary/30 to-primary/10",
  "from-amber-400/30 to-amber-400/10",
  "from-emerald-400/30 to-emerald-400/10",
  "from-rose-400/30 to-rose-400/10",
  "from-sky-400/30 to-sky-400/10",
  "from-primary/20 to-primary/5",
]

export function RecentApplications() {
  return (
    <div className="rounded-2xl bg-card shadow-sm ring-1 ring-border/30">
      {/* Header */}
      <div className="flex items-center justify-between px-6 py-5">
        <div>
          <h3 className="text-[15px] font-bold text-foreground">
            Antrian Pengajuan Terbaru
          </h3>
          <p className="mt-0.5 text-[12px] text-muted-foreground">
            Menampilkan 6 pengajuan terakhir yang masuk
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button className="flex h-8 items-center gap-1.5 rounded-lg bg-secondary/60 px-3 text-[12px] font-semibold text-muted-foreground transition-all duration-300 hover:bg-secondary hover:text-foreground">
            <Filter className="size-3.5" strokeWidth={1.8} />
            Filter
          </button>
          <button className="flex h-8 items-center gap-1.5 rounded-lg bg-primary/[0.08] px-3 text-[12px] font-bold text-primary transition-all duration-300 hover:bg-primary/15">
            Lihat Semua
            <ArrowUpRight className="size-3.5" strokeWidth={2.2} />
          </button>
        </div>
      </div>

      {/* Table */}
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead>
            <tr className="border-t border-border/30">
              <th className="px-6 py-3 text-left text-[11px] font-bold uppercase tracking-[0.1em] text-muted-foreground/60">
                Nasabah
              </th>
              <th className="px-6 py-3 text-left text-[11px] font-bold uppercase tracking-[0.1em] text-muted-foreground/60">
                NIK
              </th>
              <th className="px-6 py-3 text-left text-[11px] font-bold uppercase tracking-[0.1em] text-muted-foreground/60">
                Plafon
              </th>
              <th className="px-6 py-3 text-left text-[11px] font-bold uppercase tracking-[0.1em] text-muted-foreground/60">
                Tenor
              </th>
              <th className="px-6 py-3 text-left text-[11px] font-bold uppercase tracking-[0.1em] text-muted-foreground/60">
                Tanggal
              </th>
              <th className="px-6 py-3 text-left text-[11px] font-bold uppercase tracking-[0.1em] text-muted-foreground/60">
                Status
              </th>
              <th className="px-6 py-3 text-right text-[11px] font-bold uppercase tracking-[0.1em] text-muted-foreground/60">
                Aksi
              </th>
            </tr>
          </thead>
          <tbody>
            {applications.map((app, index) => {
              const status = statusConfig[app.status]
              return (
                <tr
                  key={app.nik}
                  className={cn(
                    "group transition-all duration-300 hover:bg-secondary/40 animate-fade-in-up",
                    index !== applications.length - 1 &&
                      "border-b border-border/20"
                  )}
                  style={{ animationDelay: `${index * 80}ms` }}
                >
                  {/* Name with avatar */}
                  <td className="px-6 py-3.5">
                    <div className="flex items-center gap-3">
                      <div
                        className={`flex size-8 shrink-0 items-center justify-center rounded-lg bg-gradient-to-br ${avatarColors[index]} text-[11px] font-bold text-foreground/80`}
                      >
                        {app.initials}
                      </div>
                      <span className="text-[13px] font-bold text-foreground">
                        {app.name}
                      </span>
                    </div>
                  </td>
                  {/* NIK */}
                  <td className="px-6 py-3.5">
                    <span className="font-mono text-[13px] text-muted-foreground">
                      {app.nik}
                    </span>
                  </td>
                  {/* Plafon */}
                  <td className="px-6 py-3.5">
                    <span className="text-[13px] font-semibold text-foreground">
                      {app.plafon}
                    </span>
                  </td>
                  {/* Tenor */}
                  <td className="px-6 py-3.5">
                    <span className="text-[13px] text-muted-foreground">
                      {app.tenor}
                    </span>
                  </td>
                  {/* Date */}
                  <td className="px-6 py-3.5">
                    <span className="text-[13px] text-muted-foreground">
                      {app.tanggal}
                    </span>
                  </td>
                  {/* Status */}
                  <td className="px-6 py-3.5">
                    <Badge
                      variant="outline"
                      className={cn(
                        "gap-1.5 rounded-full border-0 px-2.5 py-1 text-[11px] font-bold",
                        status.bg,
                        status.text
                      )}
                    >
                      <span
                        className={cn(
                          "size-1.5 rounded-full",
                          status.dot
                        )}
                      />
                      {app.status}
                    </Badge>
                  </td>
                  {/* Actions */}
                  <td className="px-6 py-3.5 text-right">
                    <div className="flex items-center justify-end gap-1 opacity-0 transition-opacity duration-300 group-hover:opacity-100">
                      <button className="flex size-8 items-center justify-center rounded-lg text-muted-foreground transition-all duration-200 hover:bg-primary/10 hover:text-primary">
                        <Eye className="size-4" strokeWidth={1.8} />
                        <span className="sr-only">Lihat detail</span>
                      </button>
                      <button className="flex size-8 items-center justify-center rounded-lg text-muted-foreground transition-all duration-200 hover:bg-secondary hover:text-foreground">
                        <MoreHorizontal className="size-4" strokeWidth={1.8} />
                        <span className="sr-only">Opsi lainnya</span>
                      </button>
                    </div>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      {/* Footer */}
      <div className="flex items-center justify-between border-t border-border/20 px-6 py-3.5">
        <p className="text-[12px] text-muted-foreground">
          Menampilkan <span className="font-bold text-foreground">6</span> dari{" "}
          <span className="font-bold text-foreground">1.248</span> pengajuan
        </p>
        <div className="flex items-center gap-1.5">
          <button className="flex size-8 items-center justify-center rounded-lg text-[12px] font-bold text-primary bg-primary/[0.08]">
            1
          </button>
          <button className="flex size-8 items-center justify-center rounded-lg text-[12px] font-medium text-muted-foreground hover:bg-secondary">
            2
          </button>
          <button className="flex size-8 items-center justify-center rounded-lg text-[12px] font-medium text-muted-foreground hover:bg-secondary">
            3
          </button>
        </div>
      </div>
    </div>
  )
}
