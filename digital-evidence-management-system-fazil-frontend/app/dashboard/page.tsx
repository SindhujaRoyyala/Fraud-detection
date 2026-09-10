import DashboardLayout from "@/components/layout/DashboardLayout";
import StatsCards from "@/components/dashboard/StatsCards";
import { DocumentActivityChart, DocumentTypeChart } from "@/components/dashboard/Charts";
import Card from "@/components/ui/Card";

const recentActivity = [
  { id: "1", action: "Document uploaded", user: "James Rodriguez", time: "2 min ago", type: "document" as const },
  { id: "2", action: "Case updated", user: "Sarah Mitchell", time: "15 min ago", type: "case" as const },
  { id: "3", action: "Evidence verified", user: "David Kim", time: "32 min ago", type: "evidence" as const },
  { id: "4", action: "User logged in", user: "Emily Chen", time: "1 hr ago", type: "auth" as const },
  { id: "5", action: "Report generated", user: "Michael Brown", time: "2 hr ago", type: "report" as const },
];

export default function DashboardPage() {
  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
          <p className="text-gray-500 mt-1">Overview of your secure document management system</p>
        </div>

        <StatsCards />

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Card>
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Document Activity</h3>
            <DocumentActivityChart />
          </Card>
          <Card>
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Document Types</h3>
            <DocumentTypeChart />
          </Card>
        </div>

        <Card>
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Recent Activity</h3>
          <div className="space-y-3">
            {recentActivity.map((activity) => (
              <div key={activity.id} className="flex items-center justify-between p-3 rounded-lg hover:bg-gray-50 transition-colors">
                <div className="flex items-center gap-3">
                  <div className="h-2 w-2 rounded-full bg-primary-500" />
                  <div>
                    <div className="text-sm font-medium text-gray-900">{activity.action}</div>
                    <div className="text-xs text-gray-500">by {activity.user}</div>
                  </div>
                </div>
                <span className="text-xs text-gray-400">{activity.time}</span>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </DashboardLayout>
  );
}
