// app/src/main/resources/static/js/components/doctorCard.js
// Module file (import it with type="module" where you render cards)

import { deleteDoctor } from "../services/doctorServices.js";
import { getPatientData } from "../services/patientServices.js";

/**
 * doctor = {
 *   id, name, specialty, email, phone,
 *   availableTimes: ["09:00-10:00", ...]
 * }
 */
export function createDoctorCard(doctor) {
  // card container
  const card = document.createElement("div");
  card.className = "card doctor-card";

  // info
  const info = document.createElement("div");
  info.className = "doctor-info";

  const title = document.createElement("h3");
  title.className = "card__title";
  title.textContent = doctor.name ?? "Unknown Doctor";

  const spec = document.createElement("div");
  spec.className = "card__meta";
  spec.textContent = doctor.specialty ? doctor.specialty : "—";

  const email = document.createElement("div");
  email.className = "card__meta";
  email.textContent = doctor.email ? doctor.email : "—";

  const times = document.createElement("div");
  times.className = "card__meta";
  const slots = Array.isArray(doctor.availableTimes) ? doctor.availableTimes.join(", ") : "No availability";
  times.textContent = `Available: ${slots}`;

  info.appendChild(title);
  info.appendChild(spec);
  info.appendChild(email);
  info.appendChild(times);

  // actions
  const actions = document.createElement("div");
  actions.className = "card-actions";

  const role = localStorage.getItem("userRole");

  if (role === "admin") {
    const btn = document.createElement("button");
    btn.className = "btn";
    btn.textContent = "Delete";

    btn.addEventListener("click", async () => {
      const ok = confirm(`Delete Dr. ${doctor.name}?`);
      if (!ok) return;

      const token = localStorage.getItem("token");
      if (!token) {
        alert("Admin session expired. Please log in again.");
        window.location.href = "/";
        return;
      }

      try {
        await deleteDoctor(doctor.id, token);
        card.remove();
        alert("Doctor deleted.");
      } catch (err) {
        console.error(err);
        alert("Failed to delete doctor.");
      }
    });

    actions.appendChild(btn);
  } else if (role === "patient") {
    const btn = document.createElement("button");
    btn.className = "btn";
    btn.textContent = "Book Now";
    btn.addEventListener("click", () => {
      alert("Please log in to book an appointment.");
    });
    actions.appendChild(btn);
  } else if (role === "loggedPatient") {
    const btn = document.createElement("button");
    btn.className = "btn";
    btn.textContent = "Book Now";

    btn.addEventListener("click", async (e) => {
      const token = localStorage.getItem("token");
      if (!token) {
        alert("Session expired. Please log in again.");
        window.location.href = "/";
        return;
      }

      try {
        const patient = await getPatientData(token);

        // show booking overlay from modals.js if available
        if (typeof window.showBookingOverlay === "function") {
          window.showBookingOverlay(e, doctor, patient);
        } else {
          // try dynamic import if modals.js exports it
          try {
            const mod = await import("./modals.js");
            if (typeof mod.showBookingOverlay === "function") {
              mod.showBookingOverlay(e, doctor, patient);
            } else {
              alert("Booking UI not available.");
            }
          } catch {
            alert("Booking UI not available.");
          }
        }
      } catch (err) {
        console.error(err);
        alert("Could not load patient info. Please try again.");
      }
    });

    actions.appendChild(btn);
  }

  // assemble
  card.appendChild(info);
  card.appendChild(actions);

  return card;
}


