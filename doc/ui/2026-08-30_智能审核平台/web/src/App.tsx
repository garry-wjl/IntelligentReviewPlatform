import { Navigate, Route, Routes } from 'react-router-dom';
import BasicLayout from './layout/BasicLayout';
import DashboardPage from './pages/DashboardPage';
import TypeListPage from './pages/types/TypeListPage';
import TypeDetailPage from './pages/types/TypeDetailPage';
import RuleSetEditPage from './pages/rulesets/RuleSetEditPage';
import TrialPage from './pages/trial/TrialPage';
import TaskListPage from './pages/tasks/TaskListPage';
import TaskDetailPage from './pages/tasks/TaskDetailPage';
import PlaygroundPage from './pages/playground/PlaygroundPage';
import SettingsPage from './pages/settings/SettingsPage';
import AuditorListPage from './pages/auditors/AuditorListPage';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<BasicLayout />}>
        <Route index element={<DashboardPage />} />
        <Route path="rule-sets" element={<TypeListPage />} />
        <Route path="rule-sets/:typeId" element={<TypeDetailPage />} />
        <Route path="rule-sets/:typeId/versions/:ruleSetId" element={<RuleSetEditPage />} />
        <Route path="rule-sets/:typeId/versions/:ruleSetId/trial" element={<TrialPage />} />
        <Route path="auditors" element={<AuditorListPage />} />
        <Route path="tasks" element={<TaskListPage />} />
        <Route path="tasks/create" element={<PlaygroundPage />} />
        <Route path="tasks/:taskId" element={<TaskDetailPage />} />
        <Route path="types" element={<Navigate to="/rule-sets" replace />} />
        <Route path="types/:typeId" element={<Navigate to="/rule-sets" replace />} />
        <Route path="playground" element={<Navigate to="/tasks/create" replace />} />
        <Route path="settings" element={<SettingsPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
