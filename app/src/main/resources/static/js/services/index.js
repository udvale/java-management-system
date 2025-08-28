// app/src/main/resources/static/js/services/index.js
// Role-Based Login Handling (Admin / Doctor)

import { openModal } from "../components/modals.js";
import { API_BASE_URL } from "../config/config.js";

// API endpoints
const ADMIN_API = `${API_BASE_URL}/admin`;
const DOCTOR_API = `${API_BASE_URL}/doctor/login`;

// ---- helpers ----
function selectRole(role) {
  // render.js usually exposes selectRole()/setRole;
  // fall back to localStorage if not present
  if (typeof window.selectRole === "function") {
    window.selectRole(role);
  } else {
    localStorage.setItem("userRole", role);
  }
}

function getInputValue(id) {
  const el = document.getElementById(id);
  return el ? el.value.trim() : "";
}

async function toJsonSafe(resp) {
  // Some backends return empty body on errors; guard it
  const text = await resp.text();
  try {
    return text ? JSON.parse(text) : {};
  } catch {
    return {};
  }
}

// ---- wire up buttons after DOM is ready ----
window.onload = function () {
  const adminBtn = document.getElementById("adminLogin");
  if (adminBtn) {
    adminBtn.addEventListener("click", () => openModal("adminLogin"));
  }

  const doctorBtn = document.getElementById("doctorLogin");
  if (doctorBtn) {
    doctorBtn.addEventListener("click", () => openModal("doctorLogin"));
  }
};

// ---- ADMIN LOGIN ----
window.adminLoginHandler = async function adminLoginHandler() {
  try {
    const username = getInputValue("adminUsername");
    const password = getInputValue("adminPassword");

    if (!username || !password) {
      alert("Please enter both username and password.");
      return;
    }

    const admin = { username, password };

    const resp = await fetch(ADMIN_API, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(admin),
    });

    const data = await toJsonSafe(resp);

    if (!resp.ok) {
      alert(data?.message || "Invalid credentials!");
      return;
    }

    const token = data?.token || data?.accessToken || data?.jwt;
    if (!token) {
      alert("Login succeeded but no token was returned.");
      return;
    }

    localStorage.setItem("token", token);
    selectRole("admin");
    // optional redirect to admin dashboard
    window.location.href = "/templates/admin/adminDashboard";
  } catch (err) {
    console.error("Admin login error:", err);
    alert("Something went wrong. Please try again.");
  }
};

// ---- DOCTOR LOGIN ----
window.doctorLoginHandler = async function doctorLoginHandler() {
  try {
    const email = getInputValue("doctorEmail");
    const password = getInputValue("doctorPassword");

    if (!email || !password) {
      alert("Please enter both email and password.");
      return;
    }

    const doctor = { email, password };

    const resp = await fetch(DOCTOR_API, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(doctor),
    });

    const data = await toJsonSafe(resp);

    if (!resp.ok) {
      alert(data?.message || "Invalid credentials!");
      return;
    }

    const token = data?.token || data?.accessToken || data?.jwt;
    if (!token) {
      alert("Login succeeded but no token was returned.");
      return;
    }

    localStorage.setItem("token", token);
    selectRole("doctor");
    // optional redirect to doctor dashboard
    window.location.href = "/templates/doctor/doctorDashboard";
  } catch (err) {
    console.error("Doctor login error:", err);
    alert("Something went wrong. Please try again.");
  }
};
