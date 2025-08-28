import {API_BASE_URL} from "../config/config.js";

const DOCTOR_API = API_BASE_URL + "/doctor";

// Fetch all doctors
export async function getDoctors() {
  try {
    const response = await fetch(DOCTOR_API);
    if (!response.ok) throw new Error("Failed to fetch doctors");
    const data = await response.json();
    return data.doctors || [];
  } catch (error) {
    console.error("Error fetching doctors:", error);
    return [];
  }
}

// Delete a doctor by ID (requires auth token)
export async function deleteDoctor(id, token) {
  try {
    const url = `${DOCTOR_API}/${id}?token=${encodeURIComponent(token)}`;
    const response = await fetch(url, {method: "DELETE"});
    const data = await response.json();
    return {
      success: response.ok,
      message:
        data.message ||
        (response.ok ? "Doctor deleted." : "Failed to delete doctor."),
    };
  } catch (error) {
    console.error("Error deleting doctor:", error);
    return {success: false, message: "Error deleting doctor."};
  }
}

// Save (add) a new doctor (requires auth token)
export async function saveDoctor(doctor, token) {
  try {
    const url = `${DOCTOR_API}?token=${encodeURIComponent(token)}`;
    const response = await fetch(url, {
      method: "POST",
      headers: {"Content-Type": "application/json"},
      body: JSON.stringify(doctor),
    });
    const data = await response.json();
    return {
      success: response.ok,
      message:
        data.message ||
        (response.ok ? "Doctor saved." : "Failed to save doctor."),
    };
  } catch (error) {
    console.error("Error saving doctor:", error);
    return {success: false, message: "Error saving doctor."};
  }
}

// Filter doctors by name, time, and specialty
export async function filterDoctors(name, time, specialty) {
  try {
    // Build query params only for non-empty filters
    const params = [];
    if (name) params.push(`name=${encodeURIComponent(name)}`);
    if (time) params.push(`time=${encodeURIComponent(time)}`);
    if (specialty) params.push(`specialty=${encodeURIComponent(specialty)}`);
    const url = `${DOCTOR_API}/filter${
      params.length > 0 ? "?" + params.join("&") : ""
    }`;
    const response = await fetch(url);
    if (!response.ok) throw new Error("Failed to filter doctors");
    const data = await response.json();
    return data.doctors || [];
  } catch (error) {
    console.error("Error filtering doctors:", error);
    alert("Failed to filter doctors. Please try again.");
    return [];
  }
}
