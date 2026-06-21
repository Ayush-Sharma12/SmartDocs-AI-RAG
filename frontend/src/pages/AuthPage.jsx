import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { login, register, storeAuth } from "../services/api";
import Toast from "../components/Toast";

const INITIAL_FORM = {
  username: "",
  email: "",
  password: "",
};

const SUCCESS_MESSAGES = {
  login: "Welcome back!",
  register: "Account created successfully!",
};

function validateForm(form, isLogin) {
  const errors = {};

  if (!form.username.trim()) {
    errors.username = "Username is required";
  } else if (form.username.length < 3) {
    errors.username = "Username must be at least 3 characters";
  }

  if (!isLogin) {
    if (!form.email.trim()) {
      errors.email = "Email is required";
    } else if (!/\S+@\S+\.\S+/.test(form.email)) {
      errors.email = "Please enter a valid email address";
    }
  }

  if (!form.password) {
    errors.password = "Password is required";
  } else if (form.password.length < 6) {
    errors.password = "Password must be at least 6 characters";
  }

  return errors;
}

function resolveErrorMessage(error) {
  return (
    error?.response?.data?.error ||
    error?.message ||
    "Something went wrong"
  );
}

function FormField({
  label,
  type,
  value,
  placeholder,
  error,
  onChange,
}) {
  return (
    <label>
      {label}
      <input
        type={type}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        className={error ? "input-error" : ""}
        required
      />
      {error && <span className="field-error">{error}</span>}
    </label>
  );
}

export default function AuthPage({ mode }) {
  const isLogin = mode === "login";

  const navigate = useNavigate();
  const location = useLocation();

  const [form, setForm] = useState(INITIAL_FORM);
  const [validationErrors, setValidationErrors] = useState({});
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState(null);

  const nextPath =
    location.state?.from?.pathname || "/dashboard";

  const updateField = (field, value) => {
    setForm((prev) => ({
      ...prev,
      [field]: value,
    }));

    if (validationErrors[field]) {
      setValidationErrors((prev) => ({
        ...prev,
        [field]: "",
      }));
    }
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    setError("");

    const errors = validateForm(form, isLogin);

    if (Object.keys(errors).length) {
      setValidationErrors(errors);
      return;
    }

    setValidationErrors({});
    setLoading(true);

    try {
      const response = isLogin
        ? await login(form.username, form.password)
        : await register(
            form.username,
            form.email,
            form.password
          );

      storeAuth(response);

      setToast({
        message: SUCCESS_MESSAGES[mode],
        type: "success",
      });

      setTimeout(() => {
        navigate(nextPath, { replace: true });
      }, 1000);
    } catch (err) {
      const message = resolveErrorMessage(err);

      setError(message);

      setToast({
        message,
        type: "error",
      });
    } finally {
      setLoading(false);
    }
  };

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

          <h1>
            {isLogin
              ? "Welcome back."
              : "Create your SmartDocs workspace."}
          </h1>

          <p>
            {isLogin
              ? "Sign in to manage your PDFs and ask authenticated questions."
              : "Register a private account to upload documents and keep your retrieval flow user-scoped."}
          </p>
        </div>

        <form
          className="auth-card"
          onSubmit={handleSubmit}
        >
          <h2>{isLogin ? "Sign in" : "Register"}</h2>

          {!isLogin && (
            <FormField
              label="Email"
              type="email"
              value={form.email}
              placeholder="you@example.com"
              error={validationErrors.email}
              onChange={(e) =>
                updateField("email", e.target.value)
              }
            />
          )}

          <FormField
            label="Username"
            type="text"
            value={form.username}
            placeholder="ayush"
            error={validationErrors.username}
            onChange={(e) =>
              updateField("username", e.target.value)
            }
          />

          <FormField
            label="Password"
            type="password"
            value={form.password}
            placeholder="Minimum 6 characters"
            error={validationErrors.password}
            onChange={(e) =>
              updateField("password", e.target.value)
            }
          />

          {error && (
            <p className="error-text">
              ❌ {error}
            </p>
          )}

          <button
            className="button primary wide"
            type="submit"
            disabled={loading}
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
            {isLogin
              ? "Need an account?"
              : "Already have an account?"}{" "}
            <Link
              to={isLogin ? "/register" : "/login"}
            >
              {isLogin ? "Register" : "Sign in"}
            </Link>
          </p>
        </form>
      </section>
    </main>
  );
}

