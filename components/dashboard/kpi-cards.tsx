import {
  FileText,
  Clock,
  CheckCircle2,
  XCircle,
  TrendingUp,
  TrendingDown,
  ArrowUpRight,
} from "lucide-react"

const metrics = [
  {
    title: "Total Pengajuan",
    value: "1.248",
    change: "+12.5%",
    period: "vs bulan lalu",
    trend: "up" as const,
    icon: FileText,
    gradientFrom: "from-primary/15",
    gradientTo: "to-primary/5",
    iconColor: "text-primary",
    sparkColor: "bg-primary/60",
  },
  {
    title: "Dalam Review",
    value: "342",
    change: "+8.2%",
    period: "vs bulan lalu",
    trend: "up" as const,
    icon: Clock,
    gradientFrom: "from-amber-500/15",
    gradientTo: "to-amber-500/5",
    iconColor: "text-amber-600 dark:text-amber-400",
    sparkColor: "bg-amber-500/60",
  },
  {
    title: "Disetujui",
    value: "856",
    change: "+18.3%",
    period: "vs bulan lalu",
    trend: "up" as const,
    icon: CheckCircle2,
    gradientFrom: "from-emerald-500/15",
    gradientTo: "to-emerald-500/5",
    iconColor: "text-emerald-600 dark:text-emerald-400",
    sparkColor: "bg-emerald-500/60",
  },
  {
    title: "Ditolak",
    value: "50",
    change: "-5.1%",
    period: "vs bulan lalu",
    trend: "down" as const,
    icon: XCircle,
    gradientFrom: "from-rose-500/15",
    gradientTo: "to-rose-500/5",
    iconColor: "text-rose-500 dark:text-rose-400",
    sparkColor: "bg-rose-500/60",
  },
]

/** Mini spark visualization -- animated bar chart */
function MiniSpark({ color }: { color: string }) {
  const bars = [35, 55, 45, 70, 60, 80, 65, 90, 75, 85]
  return (
    <div className="flex items-end gap-[3px] h-8" aria-hidden="true">
      {bars.map((h, i) => (
        <div
          key={i}
          className={`w-[3px] rounded-full ${color} animate-bar-grow`}
          style={{
            height: `${h}%`,
            opacity: 0.4 + (i / bars.length) * 0.6,
            animationDelay: `${i * 60}ms`,
          }}
        />
      ))}
    </div>
  )
}

export function KpiCards() {
  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {metrics.map((metric, index) => (
        <div
          key={metric.title}
          className="group relative overflow-hidden rounded-2xl bg-card p-5 shadow-sm ring-1 ring-border/30 transition-all duration-500 hover:shadow-lg hover:shadow-primary/[0.04] hover:-translate-y-0.5 animate-fade-in-up"
          style={{ animationDelay: `${index * 100}ms` }}
        >
          {/* Subtle gradient overlay */}
          <div
            className={`absolute inset-0 bg-gradient-to-br ${metric.gradientFrom} ${metric.gradientTo} opacity-0 transition-opacity duration-500 group-hover:opacity-100`}
          />

          {/* Shimmer on hover */}
          <div className="absolute inset-0 overflow-hidden opacity-0 group-hover:opacity-100 pointer-events-none">
            <div className="absolute inset-y-0 -left-full w-1/2 bg-gradient-to-r from-transparent via-primary/[0.04] to-transparent animate-shimmer" />
          </div>

          {/* Content */}
          <div className="relative">
            <div className="flex items-start justify-between">
              {/* Icon */}
              <div
                className={`flex size-11 items-center justify-center rounded-xl bg-gradient-to-br ${metric.gradientFrom} ${metric.gradientTo}`}
              >
                <metric.icon
                  className={`size-5 ${metric.iconColor}`}
                  strokeWidth={2}
                />
              </div>
              {/* Mini spark */}
              <MiniSpark color={metric.sparkColor} />
            </div>

            {/* Value */}
            <div className="mt-4">
              <p className="text-[28px] font-extrabold tracking-tight text-foreground leading-none">
                {metric.value}
              </p>
              <p className="mt-1.5 text-[12px] font-semibold text-muted-foreground">
                {metric.title}
              </p>
            </div>

            {/* Trend badge */}
            <div className="mt-3 flex items-center gap-2">
              <div
                className={`flex items-center gap-1 rounded-lg px-2 py-1 text-[11px] font-bold ${
                  metric.trend === "up"
                    ? "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
                    : "bg-rose-500/10 text-rose-500 dark:text-rose-400"
                }`}
              >
                {metric.trend === "up" ? (
                  <TrendingUp className="size-3" />
                ) : (
                  <TrendingDown className="size-3" />
                )}
                <span>{metric.change}</span>
              </div>
              <span className="text-[11px] text-muted-foreground/70">
                {metric.period}
              </span>
            </div>
          </div>

          {/* Hover action */}
          <div className="absolute right-3 top-3 flex size-7 items-center justify-center rounded-lg bg-primary/0 text-primary/0 transition-all duration-300 group-hover:bg-primary/10 group-hover:text-primary">
            <ArrowUpRight className="size-3.5" strokeWidth={2.5} />
          </div>
        </div>
      ))}
    </div>
  )
}
