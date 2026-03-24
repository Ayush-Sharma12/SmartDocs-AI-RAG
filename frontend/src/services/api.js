import axios from "axios";

const API_BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8080/api";
const STORAGE_KEY = "smartdocs_auth";

const client = axios.create({
  baseURL: API_BASE_URL
});

client.interceptors.request.use((config) => {
  const auth = getStoredAuth();
  if (auth?.token) {
    config.headers.Authorization = `Bearer ${auth.token}`;
  }
  return config;
});

export async function register(username, email, password) {
  const response = await client.post("/auth/register", { username, email, password });
  return response.data;
}

export async function login(username, password) {
  const response = await client.post("/auth/login", { username, password });
  return response.data;
}

export async function getDocuments() {
  const response = await client.get("/documents");
  return response.data;
}

export async function uploadDocument(file) {
  const formData = new FormData();
  formData.append("file", file);
  const response = await client.post("/documents/upload", formData, {
    headers: {
      "Content-Type": "multipart/form-data"
    }
  });
  return response.data;
}

export async function deleteDocument(documentId) {
  const response = await client.delete(`/documents/${documentId}`);
  return response.data;
}

export async function askQuestion(question, documentId) {
  const response = await client.post("/qa/ask", { question, documentId });
  return response.data;
}

export function storeAuth(auth) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
}

export function getStoredAuth() {
  const raw = localStorage.getItem(STORAGE_KEY);
  return raw ? JSON.parse(raw) : null;
}

export function clearStoredAuth() {
  localStorage.removeItem(STORAGE_KEY);
}
