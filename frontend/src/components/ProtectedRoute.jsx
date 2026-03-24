import { Navigate, useLocation } from "react-router-dom";
import { getStoredAuth } from "../services/api";

export default function ProtectedRoute({ children }) {
  const location = useLocation();
  const auth = getStoredAuth();

  if (!auth?.token) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return children;
}
