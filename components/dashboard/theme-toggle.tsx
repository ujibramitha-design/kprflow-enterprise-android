"use client"

import { useTheme } from "next-themes"
import { useEffect, useState } from "react"
import { Moon, Sun } from "lucide-react"
import { cn } from "@/lib/utils"

export function ThemeToggle() {
  const { theme, setTheme } = useTheme()
  const [mounted, setMounted] = useState(false)

  useEffect(() => {
    setMounted(true)
  }, [])

  if (!mounted) {
    return (
      <div className="flex h-9 w-[72px] items-center rounded-xl bg-secondary/50 p-1">
        <div className="size-7 rounded-lg" />
      </div>
    )
  }

  const isDark = theme === "dark"

  return (
    <button
      onClick={() => setTheme(isDark ? "light" : "dark")}
      className="group relative flex h-9 w-[72px] items-center rounded-xl bg-secondary/60 p-1 transition-all duration-500 hover:bg-secondary"
      aria-label={isDark ? "Aktifkan mode terang" : "Aktifkan mode gelap"}
    >
      {/* Sliding pill */}
      <div
        className={cn(
          "absolute top-1 flex size-7 items-center justify-center rounded-lg bg-card shadow-sm transition-all duration-500 ease-in-out",
          isDark ? "left-[calc(100%-32px)]" : "left-1"
        )}
      >
        {isDark ? (
          <Moon className="size-3.5 text-primary" strokeWidth={2.2} />
        ) : (
          <Sun className="size-3.5 text-amber-500" strokeWidth={2.2} />
        )}
      </div>

      {/* Background icons */}
      <div className="flex w-full items-center justify-between px-2">
        <Sun
          className={cn(
            "size-3 transition-all duration-500",
            isDark ? "text-muted-foreground/40" : "text-transparent"
          )}
          strokeWidth={2}
        />
        <Moon
          className={cn(
            "size-3 transition-all duration-500",
            isDark ? "text-transparent" : "text-muted-foreground/40"
          )}
          strokeWidth={2}
        />
      </div>
    </button>
  )
}
