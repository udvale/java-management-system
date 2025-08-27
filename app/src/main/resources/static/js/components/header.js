// app/src/main/resources/static/js/components/header.js

(function () {
  function selectRole(role) {
    localStorage.setItem("userRole", role);
  }

  function logout() {
    localStorage.removeItem("token");
    localStorage.removeItem("userRole");
    window.location.href = "/";
  }

  function logoutPatient() {
    localStorage.removeItem("token");
    localStorage.setItem("userRole", "patient"); // keep base role so login/sign-up show
    window.location.href = "/pages/patientDashboard.html";
  }

  function openModalSafe(kind) {
    if (typeof window.openModal === "function") {
      window.openModal(kind);
    } else {
      console.warn("openModal() not found. Ensure js/components/modals.js is loaded.");
      alert(kind === "addDoctor" ? "Add Doctor modal not available." : "Modal not available.");
    }
  }

  function attachHeaderButtonListeners() {
    const addDocBtn = document.getElementById("addDocBtn");
    if (addDocBtn) addDocBtn.addEventListener("click", () => openModalSafe("addDoctor"));

    const patientLogin = document.getElementById("patientLogin");
    if (patientLogin) patientLogin.addEventListener("click", () => openModalSafe("patientLogin"));

    const patientSignup = document.getElementById("patientSignup");
    if (patientSignup) patientSignup.addEventListener("click", () => openModalSafe("patientSignup"));

    const adminHome = document.getElementById("adminHome");
    if (adminHome) adminHome.addEventListener("click", () => { selectRole("admin"); window.location.href = "/templates/admin/adminDashboard"; });

    const doctorHome = document.getElementById("doctorHome");
    if (doctorHome) doctorHome.addEventListener("click", () => { selectRole("doctor"); window.location.href = "/templates/doctor/doctorDashboard"; });

    const lpHome = document.getElementById("lpHome");
    if (lpHome) lpHome.addEventListener("click", () => { window.location.href = "/pages/loggedPatientDashboard.html"; });

    const lpAppt = document.getElementById("lpAppointments");
    if (lpAppt) lpAppt.addEventListener("click", () => { window.location.href = "/pages/patientAppointments.html"; });

    const logoutBtn = document.getElementById("logoutBtn");
    if (logoutBtn) logoutBtn.addEventListener("click", logout);

    const logoutPatientBtn = document.getElementById("logoutPatientBtn");
    if (logoutPatientBtn) logoutPatientBtn.addEventListener("click", logoutPatient);
  }

  function isRoot() {
    const p = window.location.pathname;
    return p === "/" || p === "" || p.endsWith("/index.html");
  }

  function renderHeader() {
    const headerDiv = document.getElementById("header");
    if (!headerDiv) return;

    if (isRoot()) {
      localStorage.removeItem("userRole");
      localStorage.removeItem("token");
      headerDiv.innerHTML = `
        <header class="header">
          <div class="logo-section">
            <img src="/assets/images/logo/logo.png" alt="Hospital CMS Logo" class="logo-img">
            <span class="logo-title">Hospital CMS</span>
          </div>
        </header>`;
      return;
    }

    const role = localStorage.getItem("userRole");
    const token = localStorage.getItem("token");

    if ((role === "loggedPatient" || role === "admin" || role === "doctor") && !token) {
      localStorage.removeItem("userRole");
      alert("Session expired or invalid login. Please log in again.");
      window.location.href = "/";
      return;
    }

    let headerContent = `
      <header class="header">
        <div class="logo-section">
          <img src="/assets/images/logo/logo.png" alt="Hospital CMS Logo" class="logo-img">
          <span class="logo-title">Hospital CMS</span>
        </div>
        <nav>`;

    if (role === "admin") {
      headerContent += `
          <button id="addDocBtn" class="adminBtn" type="button">Add Doctor</button>
          <button id="logoutBtn" class="adminBtn" type="button">Logout</button>`;
    } else if (role === "doctor") {
      headerContent += `
          <button id="doctorHome" class="adminBtn" type="button">Home</button>
          <button id="logoutBtn" class="adminBtn" type="button">Logout</button>`;
    } else if (role === "loggedPatient") {
      headerContent += `
          <button id="lpHome" class="adminBtn" type="button">Home</button>
          <button id="lpAppointments" class="adminBtn" type="button">Appointments</button>
          <button id="logoutPatientBtn" class="adminBtn" type="button">Logout</button>`;
    } else {
      // default: patient (not logged in) or unknown
      headerContent += `
          <button id="patientLogin" class="adminBtn" type="button">Login</button>
          <button id="patientSignup" class="adminBtn" type="button">Sign Up</button>`;
    }

    headerContent += `
        </nav>
      </header>`;

    headerDiv.innerHTML = headerContent;
    attachHeaderButtonListeners();
  }

  // expose & run
  window.renderHeader = renderHeader;
  // If this file is included with "defer", DOM is parsed already
  try { renderHeader(); } catch (e) { console.error(e); }
})();
