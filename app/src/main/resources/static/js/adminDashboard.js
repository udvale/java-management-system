import {openModal} from "./components/modals.js";
import {
  getDoctors,
  filterDoctors,
  saveDoctor,
} from "./services/doctorServices.js";
import {createDoctorCard} from "./components/doctorCard.js";

// Add Doctor button event
const addDocBtn = document.getElementById("addDocBtn");
if (addDocBtn) {
  addDocBtn.addEventListener("click", () => openModal("addDoctor"));
}

// Load doctor cards on page load
document.addEventListener("DOMContentLoaded", () => {
  loadDoctorCards();
});

// Load all doctors and render
async function loadDoctorCards() {
  try {
    const doctors = await getDoctors();
    renderDoctorCards(doctors);
  } catch (error) {
    console.error("Failed to load doctors:", error);
  }
}

// Render doctor cards utility
function renderDoctorCards(doctors) {
  const contentDiv = document.getElementById("content");
  contentDiv.innerHTML = "";
  if (doctors && doctors.length > 0) {
    doctors.forEach((doctor) => {
      const card = createDoctorCard(doctor);
      contentDiv.appendChild(card);
    });
  } else {
    contentDiv.innerHTML = "<p>No doctors found.</p>";
  }
}

// Filter event listeners
const searchBar = document.getElementById("searchBar");
const filterTime = document.getElementById("filterTime");
const filterSpecialty = document.getElementById("filterSpecialty");
if (searchBar) searchBar.addEventListener("input", filterDoctorsOnChange);
if (filterTime) filterTime.addEventListener("change", filterDoctorsOnChange);
if (filterSpecialty)
  filterSpecialty.addEventListener("change", filterDoctorsOnChange);

// Filter doctors on change
async function filterDoctorsOnChange() {
  try {
    const name =
      searchBar && searchBar.value.trim() ? searchBar.value.trim() : null;
    const time = filterTime && filterTime.value ? filterTime.value : null;
    const specialty =
      filterSpecialty && filterSpecialty.value ? filterSpecialty.value : null;
    const doctors = await filterDoctors(name, time, specialty);
    if (doctors && doctors.length > 0) {
      renderDoctorCards(doctors);
    } else {
      const contentDiv = document.getElementById("content");
      contentDiv.innerHTML = "<p>No doctors found with the given filters.</p>";
    }
  } catch (error) {
    alert("Error filtering doctors.");
    console.error("Error filtering doctors:", error);
  }
}

// Add doctor form handler (should be called on form submit)
window.adminAddDoctor = async function () {
  try {
    const name = document.getElementById("doctorName").value;
    const email = document.getElementById("doctorEmail").value;
    const password = document.getElementById("doctorPassword").value;
    const phone = document.getElementById("doctorPhone").value;
    const specialty = document.getElementById("doctorSpecialty").value;
    // Collect availability from checkboxes (if any)
    const availabilityNodes = document.querySelectorAll(
      'input[name="doctorAvailability"]:checked'
    );
    const availability = Array.from(availabilityNodes).map((cb) => cb.value);

    const token = localStorage.getItem("token");
    if (!token) {
      alert("You must be logged in as admin to add a doctor.");
      return;
    }

    const doctor = {name, email, password, phone, specialty, availability};
    const {success, message} = await saveDoctor(doctor, token);
    if (success) {
      alert(message || "Doctor added successfully.");
      document.getElementById("modal").style.display = "none";
      loadDoctorCards();
    } else {
      alert(message || "Failed to add doctor.");
    }
  } catch (error) {
    alert("Error adding doctor.");
    console.error("Error adding doctor:", error);
  }
};
