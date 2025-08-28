import {getAllAppointments} from "./services/appointmentRecordService.js";
import {createPatientRow} from "./components/patientRows.js";

// Global variables
const tableBody = document.getElementById("patientTableBody");
let selectedDate = new Date().toISOString().split("T")[0];
const token = localStorage.getItem("token");
let patientName = null;

// Search bar event
const searchBar = document.getElementById("searchBar");
if (searchBar) {
  searchBar.addEventListener("input", (e) => {
    const value = e.target.value.trim();
    patientName = value.length > 0 ? value : "null";
    loadAppointments();
  });
}

// Today button event
const todayButton = document.getElementById("todayButton");
if (todayButton) {
  todayButton.addEventListener("click", () => {
    selectedDate = new Date().toISOString().split("T")[0];
    const datePicker = document.getElementById("datePicker");
    if (datePicker) datePicker.value = selectedDate;
    loadAppointments();
  });
}

// Date picker event
const datePicker = document.getElementById("datePicker");
if (datePicker) {
  datePicker.addEventListener("change", (e) => {
    selectedDate = e.target.value;
    loadAppointments();
  });
}

// Load appointments and render
async function loadAppointments() {
  try {
    const appointments = await getAllAppointments(
      selectedDate,
      patientName,
      token
    );
    tableBody.innerHTML = "";
    if (!appointments || appointments.length === 0) {
      const row = document.createElement("tr");
      row.innerHTML = `<td colspan="5">No Appointments found for today.</td>`;
      tableBody.appendChild(row);
      return;
    }
    appointments.forEach((app) => {
      const patient = {
        id: app.patientId,
        name: app.patientName,
        phone: app.patientPhone,
        email: app.patientEmail,
      };
      const row = createPatientRow(patient, app.id, app.doctorId);
      tableBody.appendChild(row);
    });
  } catch (error) {
    tableBody.innerHTML = "";
    const row = document.createElement("tr");
    row.innerHTML = `<td colspan="5">Error loading appointments. Try again later.</td>`;
    tableBody.appendChild(row);
    console.error("Error loading appointments:", error);
  }
}

// Initial render
document.addEventListener("DOMContentLoaded", () => {
  loadAppointments();
});
