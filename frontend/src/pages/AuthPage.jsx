import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { login, register, storeAuth } from "../services/api";
import Toast from "../components/Toast";

export default function AuthPage({ mode }) {
  const isLogin = mode === "login";
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({
    username: "",
    email: "",
    password: ""
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState(null);
  const [validationErrors, setValidationErrors] = useState({});

  const nextPath = location.state?.from?.pathname || "/dashboard";

  function validateForm() {
    const errors = {};
    if (!form.username.trim()) {
      errors.username = "Username is required";
    } else if (form.username.length < 3) {
      errors.username = "Username must be at least 3 characters";
    }
    if (!isLogin && !form.email.trim()) {
      errors.email = "Email is required";
    } else if (!isLogin && !form.email.includes("@")) {
      errors.email = "Please enter a valid email address";
    }
    if (!form.password) {
      errors.password = "Password is required";
    } else if (form.password.length < 6) {
      errors.password = "Password must be at least 6 characters";
    }
    return errors;
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    
    const errors = validateForm();
    if (Object.keys(errors).length > 0) {
      setValidationErrors(errors);
      return;
    }
    
    setValidationErrors({});
    setLoading(true);

    try {
      const response = isLogin
        ? await login(form.username, form.password)
        : await register(form.username, form.email, form.password);
      storeAuth(response);
      setToast({ 
        message: isLogin ? "Welcome back!" : "Account created successfully!", 
        type: "success" 
      });
      setTimeout(() => {
        navigate(nextPath, { replace: true });
      }, 1000);
    } catch (submissionError) {
      const errorMsg = resolveErrorMessage(submissionError);
      setError(errorMsg);
      setToast({ message: errorMsg, type: "error" });
    } finally {
      setLoading(false);
    }
  }

  function updateField(field, value) {
    setForm((current) => ({ ...current, [field]: value }));
    if (validationErrors[field]) {
      setValidationErrors((current) => ({ ...current, [field]: "" }));
    }
  }

  return (
    <main className="page auth-page">
      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onClose={() => setToast(null)}
        />
      )}

      <section className="auth-layout">
        <div className="auth-brand">
          <p className="eyebrow">Secure access</p>
          <h1>{isLogin ? "Welcome back." : "Create your SmartDocs workspace."}</h1>
          <p>
            {isLogin
              ? "Sign in to manage your PDFs and ask authenticated questions."
              : "Register a private account to upload documents and keep your retrieval flow user-scoped."}
          </p>
        </div>

        <form className="auth-card" onSubmit={handleSubmit}>
          <h2>{isLogin ? "Sign in" : "Register"}</h2>
          {!isLogin && (
            <label>
              Email
              <input
                type="email"
                value={form.email}
                onChange={(event) => updateField("email", event.target.value)}
                placeholder="you@example.com"
                className={validationErrors.email ? "input-error" : ""}
                required
              />
              {validationErrors.email && (
                <span className="field-error">{validationErrors.email}</span>
              )}
            </label>
          )}
          <label>
            Username
            <input
              type="text"
              value={form.username}
              onChange={(event) => updateField("username", event.target.value)}
              placeholder="ayush"
              className={validationErrors.username ? "input-error" : ""}
              required
            />
            {validationErrors.username && (
              <span className="field-error">{validationErrors.username}</span>
            )}
          </label>
          <label>
            Password
            <input
              type="password"
              value={form.password}
              onChange={(event) => updateField("password", event.target.value)}
              placeholder="Minimum 6 characters"
              className={validationErrors.password ? "input-error" : ""}
              required
            />
            {validationErrors.password && (
              <span className="field-error">{validationErrors.password}</span>
            )}
          </label>

          {error ? <p className="error-text">❌ {error}</p> : null}

          <button 
            className="button primary wide" 
            type="submit" 
            disabled={loading || Object.keys(validationErrors).length > 0}
          >
            {loading ? (
              <>
                <span className="spinner"></span>
                Please wait...
              </>
            ) : isLogin ? (
              "Sign in"
            ) : (
              "Create account"
            )}
          </button>

          <p className="muted-link">
            {isLogin ? "Need an account?" : "Already have an account?"}{" "}
            <Link to={isLogin ? "/register" : "/login"}>
              {isLogin ? "Register" : "Sign in"}
            </Link>
          </p>
        </form>
      </section>
    </main>
  );
}

function resolveErrorMessage(error) {
  return error?.response?.data?.error || error?.message || "Something went wrong";
}
