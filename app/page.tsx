import { DashboardSidebar } from "@/components/dashboard/sidebar"
import { DashboardTopbar } from "@/components/dashboard/topbar"
import { DashboardHeader } from "@/components/dashboard/header"
import { KpiCards } from "@/components/dashboard/kpi-cards"
import { QuickStats } from "@/components/dashboard/quick-stats"
import { RecentApplications } from "@/components/dashboard/recent-applications"

export default function DashboardPage() {
  return (
    <div className="flex h-screen bg-background">
      {/* Sidebar */}
      <DashboardSidebar />

      {/* Main area */}
      <div className="flex flex-1 flex-col overflow-hidden">
        {/* Topbar */}
        <DashboardTopbar />

        {/* Scrollable content */}
        <main className="flex-1 overflow-y-auto">
          <div className="mx-auto flex max-w-[1280px] flex-col gap-6 px-8 py-7">
            <div style={{ animationDelay: "0ms" }} className="animate-fade-in-up">
              <DashboardHeader />
            </div>
            <div style={{ animationDelay: "120ms" }} className="animate-fade-in-up">
              <KpiCards />
            </div>
            <div style={{ animationDelay: "240ms" }} className="animate-fade-in-up">
              <QuickStats />
            </div>
            <div style={{ animationDelay: "360ms" }} className="animate-fade-in-up">
              <RecentApplications />
            </div>
          </div>
        </main>
      </div>
    </div>
  )
}
