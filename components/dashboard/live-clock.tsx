"use client"

import { useEffect, useState } from "react"
import { Clock } from "lucide-react"

export function LiveClock() {
  const [time, setTime] = useState<Date | null>(null)
  const [prevSecond, setPrevSecond] = useState(-1)

  useEffect(() => {
    setTime(new Date())
    const interval = setInterval(() => {
      setTime(new Date())
    }, 1000)
    return () => clearInterval(interval)
  }, [])

  if (!time) {
    return (
      <div className="flex items-center gap-2 rounded-xl bg-secondary/40 px-3 py-1.5">
        <Clock className="size-3.5 text-muted-foreground/50" strokeWidth={1.8} />
        <div className="h-4 w-16 rounded bg-muted/50 animate-pulse" />
      </div>
    )
  }

  const hours = time.getHours().toString().padStart(2, "0")
  const minutes = time.getMinutes().toString().padStart(2, "0")
  const seconds = time.getSeconds().toString().padStart(2, "0")
  const currentSecond = time.getSeconds()
  const shouldAnimate = currentSecond !== prevSecond

  if (shouldAnimate && prevSecond !== currentSecond) {
    // Trigger re-render tracking
    if (prevSecond !== currentSecond) {
      // We use a ref-like pattern via setState
      setTimeout(() => setPrevSecond(currentSecond), 0)
    }
  }

  const dayName = time.toLocaleDateString("id-ID", { weekday: "short" })
  const dateStr = time.toLocaleDateString("id-ID", {
    day: "numeric",
    month: "short",
  })

  return (
    <div className="flex items-center gap-2.5 rounded-xl bg-secondary/40 px-3 py-1.5 transition-colors duration-300 hover:bg-secondary/60">
      {/* Animated clock icon */}
      <div className="relative">
        <Clock className="size-3.5 text-primary/70" strokeWidth={2} />
        <div
          className="absolute inset-0 rounded-full bg-primary/20"
          style={{
            animation: "glow-pulse 2s ease-in-out infinite",
          }}
        />
      </div>

      {/* Time display */}
      <div className="flex items-baseline gap-0.5 font-mono">
        <span className="text-[13px] font-bold text-foreground tabular-nums">
          {hours}
        </span>
        <span
          className="text-[13px] font-bold text-primary/60"
          style={{ animation: "glow-pulse 1s ease-in-out infinite" }}
        >
          :
        </span>
        <span className="text-[13px] font-bold text-foreground tabular-nums">
          {minutes}
        </span>
        <span
          className="text-[13px] font-bold text-primary/60"
          style={{ animation: "glow-pulse 1s ease-in-out infinite" }}
        >
          :
        </span>
        <span
          key={seconds}
          className="text-[13px] font-bold text-foreground tabular-nums animate-tick-in"
        >
          {seconds}
        </span>
      </div>

      {/* Divider */}
      <div className="h-3.5 w-px bg-border/50" />

      {/* Date */}
      <span className="text-[11px] font-semibold text-muted-foreground">
        {dayName}, {dateStr}
      </span>
    </div>
  )
}
