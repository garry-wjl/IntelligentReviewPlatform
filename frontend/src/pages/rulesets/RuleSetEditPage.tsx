import { Navigate, useParams } from 'react-router-dom';

export default function RuleSetEditPage() {
  const { typeId, ruleSetId } = useParams();
  const search = ruleSetId ? `?version=${encodeURIComponent(ruleSetId)}` : '';
  return <Navigate to={`/rule-sets/${typeId}${search}`} replace />;
}
