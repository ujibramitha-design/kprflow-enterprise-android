"use client"

import { cn } from "@/lib/utils"

interface Logo3DProps {
  collapsed?: boolean
}

export function Logo3D({ collapsed = false }: Logo3DProps) {
  return (
    <div
      className={cn(
        "flex items-center gap-3",
        collapsed && "justify-center"
      )}
    >
      {/* 3D Logo Container */}
      <div className="relative" style={{ perspective: "600px" }}>
        {/* Glow layer */}
        <div className="absolute inset-0 rounded-2xl bg-primary/30 blur-xl animate-glow-pulse" />

        {/* Main 3D cube */}
        <div className="animate-float relative">
          <div
            className="relative flex size-11 items-center justify-center rounded-2xl shadow-lg shadow-primary/25 dark:shadow-primary/40"
            style={{
              background: "linear-gradient(135deg, var(--primary), oklch(0.45 0.12 260))",
              transformStyle: "preserve-3d",
            }}
          >
            {/* Top face reflection */}
            <div
              className="absolute inset-x-0 top-0 h-1/2 rounded-t-2xl"
              style={{
                background: "linear-gradient(to bottom, rgba(255,255,255,0.25), transparent)",
              }}
            />

            {/* Side edge (3D depth illusion) */}
            <div
              className="absolute -bottom-[3px] -right-[3px] size-11 rounded-2xl"
              style={{
                background: "oklch(0.35 0.10 260)",
                zIndex: -1,
              }}
            />

            {/* Letter */}
            <span className="relative text-[18px] font-black tracking-tighter text-primary-foreground drop-shadow-sm">
              K
            </span>

            {/* Corner sparkle */}
            <div className="absolute -right-1 -top-1 flex size-3.5 items-center justify-center">
              <div className="absolute size-2 rounded-full bg-accent/80 animate-glow-pulse" />
              <div className="size-1.5 rounded-full bg-accent" />
            </div>
          </div>
        </div>

        {/* Status indicator */}
        <div className="absolute -bottom-0.5 -right-0.5 z-10 size-3 rounded-full border-2 border-card bg-emerald-400 dark:bg-emerald-500">
          <div className="absolute inset-0 rounded-full bg-emerald-400 dark:bg-emerald-500" style={{ animation: "dot-ping 2s cubic-bezier(0, 0, 0.2, 1) infinite" }} />
        </div>
      </div>

      {/* Brand text */}
      {!collapsed && (
        <div className="overflow-hidden animate-slide-in-right">
          <h1 className="text-[16px] font-black tracking-tight text-foreground leading-none">
            KPRFLOW
          </h1>
          <div className="mt-1 flex items-center gap-1.5">
            <div className="h-px w-3 bg-gradient-to-r from-primary/60 to-transparent" />
            <p className="text-[9px] font-bold tracking-[0.2em] uppercase text-muted-foreground/60">
              Enterprise
            </p>
          </div>
        </div>
      )}
    </div>
  )
}
