/**
 * HMS Backend - k6 Load Test Script (Configurable VUs)
 * 
 * Based on test_plan.md - Implements realistic workflows with POST, PUT operations
 * 
 * Usage:
 *   k6 run load-test.js                         # Default 50 VUs
 *   k6 run -e VUS=100 load-test.js              # 100 VUs
 *   k6 run -e VUS=200 load-test.js              # 200 VUs
 *   k6 run -e VUS=200 -e DEBUG=true load-test.js # With error logging
 *   k6 run -e VUS=200 -e DURATION=10m load-test.js
 * 
 * Debug Options:
 *   DEBUG=true     - Log all errors with status codes
 *   DEBUG=sample   - Log only 10% of errors (for high load)
 *   DEBUG=false    - No error logging (default, best performance)
 * 
 * VU Distribution (proportional):
 * - 55% Patient - Booking, View Records
 * - 18% Doctor  - Examine, Prescribe
 * - 14% Nurse   - Vital Signs, Lab Results
 * - 12% Receptionist - Walk-in, Billing
 * - 1%  Admin   - Management, Reports
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// ============================================================================
// Configuration - Read from environment variables
// ============================================================================

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TOTAL_VUS = parseInt(__ENV.VUS) || 50;
const DURATION = __ENV.DURATION || '10m';
const DEBUG = __ENV.DEBUG || 'false';  // 'true', 'sample', or 'false'

// Calculate VU distribution (maintaining percentages)
const PATIENT_VUS = Math.max(1, Math.round(TOTAL_VUS * 0.55));
const DOCTOR_VUS = Math.max(1, Math.round(TOTAL_VUS * 0.18));
const NURSE_VUS = Math.max(1, Math.round(TOTAL_VUS * 0.14));
const RECEPTIONIST_VUS = Math.max(1, Math.round(TOTAL_VUS * 0.12));
const ADMIN_VUS = Math.max(1, Math.round(TOTAL_VUS * 0.01));

// Account counts (must be seeded) - defaults for 1000 VU scale
const PATIENT_ACCOUNTS = parseInt(__ENV.PATIENTS) || 600;
const DOCTOR_ACCOUNTS = parseInt(__ENV.DOCTORS) || 200;
const NURSE_ACCOUNTS = parseInt(__ENV.NURSES) || 150;
const RECEPTIONIST_ACCOUNTS = parseInt(__ENV.RECEPTIONISTS) || 125;

export const options = {
  scenarios: {
    patients: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: PATIENT_VUS },  // Ramp up
        { duration: '8m', target: PATIENT_VUS },  // Hold
        { duration: '1m', target: 0 },            // Ramp down
      ],
      exec: 'patientScenario',
    },
    doctors: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: DOCTOR_VUS },
        { duration: '8m', target: DOCTOR_VUS },
        { duration: '1m', target: 0 },
      ],
      exec: 'doctorScenario',
    },
    nurses: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: NURSE_VUS },
        { duration: '8m', target: NURSE_VUS },
        { duration: '1m', target: 0 },
      ],
      exec: 'nurseScenario',
    },
    receptionists: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: RECEPTIONIST_VUS },
        { duration: '8m', target: RECEPTIONIST_VUS },
        { duration: '1m', target: 0 },
      ],
      exec: 'receptionistScenario',
    },
    admin: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: ADMIN_VUS },
        { duration: '8m', target: ADMIN_VUS },
        { duration: '1m', target: 0 },
      ],
      exec: 'adminScenario',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<3000'],
    http_req_failed: ['rate<0.10'],
    'login_success': ['rate>0.95'],
    'errors_4xx': ['count<1000'],
    'errors_5xx': ['count<100'],
  },
};

// Log configuration at startup
export function setup() {
  console.log(`
================================================================================
HMS Backend Load Test Configuration
================================================================================
Total VUs: ${TOTAL_VUS}
Duration: ${DURATION}
Base URL: ${BASE_URL}
Debug Mode: ${DEBUG}

VU Distribution:
  - Patients:      ${PATIENT_VUS} VUs (55%)
  - Doctors:       ${DOCTOR_VUS} VUs (18%)
  - Nurses:        ${NURSE_VUS} VUs (14%)
  - Receptionists: ${RECEPTIONIST_VUS} VUs (12%)
  - Admins:        ${ADMIN_VUS} VUs (1%)

Account Pool:
  - ${PATIENT_ACCOUNTS} patient accounts
  - ${DOCTOR_ACCOUNTS} doctor accounts
  - ${NURSE_ACCOUNTS} nurse accounts
  - ${RECEPTIONIST_ACCOUNTS} receptionist accounts
================================================================================
  `);
}

// ============================================================================
// Custom Metrics
// ============================================================================

// Success metrics
const loginSuccess = new Rate('login_success');
const loginDuration = new Trend('login_duration');
const appointmentCreated = new Counter('appointments_created');
const appointmentsCompleted = new Counter('appointments_completed');
const appointmentsCancelled = new Counter('appointments_cancelled');
const examsCreated = new Counter('exams_created');
const invoicesCreated = new Counter('invoices_created');
const paymentsRecorded = new Counter('payments_recorded');

// Error tracking metrics (by status code category)
const errors4xx = new Counter('errors_4xx');
const errors5xx = new Counter('errors_5xx');

// Error tracking by endpoint (for summary)
const endpointErrors = new Counter('endpoint_errors');

// Response time by endpoint type
const loginTime = new Trend('login_time');
const readTime = new Trend('read_time');
const writeTime = new Trend('write_time');

// ============================================================================
// Helper Functions
// ============================================================================

/**
 * Check with automatic error logging.
 * Logs endpoint, status code, and response body when check fails.
 * This helps debugging by showing exactly what went wrong.
 */
function checkAndLog(res, checkName, expectedStatus = 200, endpoint = '') {
  const isSuccess = res.status === expectedStatus || 
                    (expectedStatus === 201 && (res.status === 200 || res.status === 201));
  
  const passed = check(res, { [checkName]: () => isSuccess });
  
  if (!passed) {
    // Always log errors (not just in DEBUG mode) for easy copy-paste debugging
    let body = '';
    try {
      body = res.body ? res.body.substring(0, 300) : 'empty';
    } catch (e) {
      body = 'unable to read body';
    }
    console.log(`[FAIL] ${checkName} | Endpoint: ${endpoint} | Status: ${res.status} | Expected: ${expectedStatus} | Body: ${body}`);
    
    // Track error metrics
    if (res.status >= 400 && res.status < 500) {
      errors4xx.add(1, { endpoint: endpoint });
    } else if (res.status >= 500) {
      errors5xx.add(1, { endpoint: endpoint });
    }
  }
  
  return passed;
}

// ============================================================================
// Original Helper Functions
// ============================================================================

// Conditional debug logging (only logs errors, and can sample for high load)
function debugLog(message, res, endpoint) {
  if (DEBUG === 'false') return;
  
  // Sample mode: only log 10% of errors
  if (DEBUG === 'sample' && Math.random() > 0.1) return;
  
  // Always log
  console.log(`[ERROR] ${endpoint} | Status: ${res.status} | VU: ${__VU} | ${message}`);
}

// Track error by status code
function trackError(res, endpoint) {
  if (res.status >= 400 && res.status < 500) {
    errors4xx.add(1, { endpoint: endpoint });
  } else if (res.status >= 500) {
    errors5xx.add(1, { endpoint: endpoint });
  }
}

// Enhanced API call with error tracking
function apiCall(method, endpoint, body, headers, checkName) {
  const url = `${BASE_URL}${endpoint}`;
  let res;
  
  if (method === 'GET') {
    res = http.get(url, headers);
    readTime.add(res.timings.duration);
  } else if (method === 'POST') {
    res = http.post(url, JSON.stringify(body), headers);
    writeTime.add(res.timings.duration);
  } else if (method === 'PUT') {
    res = http.put(url, JSON.stringify(body), headers);
    writeTime.add(res.timings.duration);
  }
  
  // Track errors
  if (res.status >= 400) {
    trackError(res, endpoint);
    endpointErrors.add(1, { endpoint: endpoint, status: res.status.toString() });
    
    // Debug log if enabled
    debugLog(
      `Body: ${res.body ? res.body.substring(0, 200) : 'empty'}`,
      res,
      endpoint
    );
  }
  
  return res;
}

function login(email, password) {
  const payload = JSON.stringify({ email, password });
  const params = { headers: { 'Content-Type': 'application/json' } };
  
  const res = http.post(`${BASE_URL}/api/auth/login`, payload, params);
  loginDuration.add(res.timings.duration);
  loginTime.add(res.timings.duration);
  
  const success = res.status === 200 && res.json('code') === 1000;
  loginSuccess.add(success);
  
  if (!success) {
    trackError(res, '/api/auth/login');
    debugLog(`Login failed for ${email}`, res, '/api/auth/login');
  }
  
  check(res, { 'login success': (r) => r.status === 200 && r.json('code') === 1000 });
  
  if (success) {
    return {
      token: res.json('data.accessToken'),
      accountId: res.json('data.account.id'),
    };
  }
  return null;
}

function authHeaders(token) {
  return {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
  };
}

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randomChoice(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

// ============================================================================
// PATIENT Workflow - Appointment Booking & View Records
// ============================================================================

export function patientScenario() {
  const patientNum = (__VU % PATIENT_ACCOUNTS) + 1;
  const email = `patient${patientNum}@email.com`;
  const password = 'Patient123!@';
  
  const auth = login(email, password);
  if (!auth) { sleep(5); return; }
  
  const headers = authHeaders(auth.token);
  sleep(randomInt(2, 4));
  
  // 50% chance: Book appointment, 50% chance: View records
  if (Math.random() < 0.5) {
    // Appointment Booking Flow
    group('Patient - Book Appointment', () => {
      // 1. Get my profile
      const meRes = http.get(`${BASE_URL}/api/patients/me`, headers);
      check(meRes, { 'get profile': (r) => r.status === 200 });
      
      let patientId = null;
      if (meRes.status === 200) {
        try { patientId = meRes.json('data.id'); } catch (e) {}
      }
      
      sleep(randomInt(2, 4));
      
      // 2. Browse available doctors
      const doctorsRes = http.get(`${BASE_URL}/api/hr/employees/all?filter=role==DOCTOR&page=0&size=10`, headers);
      check(doctorsRes, { 'get doctors': (r) => r.status === 200 });
      
      sleep(randomInt(3, 5));
      
      // 3. View doctor schedules (use /doctors endpoint with date params)
      const today = new Date();
      const nextWeek = new Date();
      nextWeek.setDate(nextWeek.getDate() + 7);
      const startDate = today.toISOString().split('T')[0];
      const endDate = nextWeek.toISOString().split('T')[0];
      
      const schedulesRes = http.get(
        `${BASE_URL}/api/hr/schedules/doctors?startDate=${startDate}&endDate=${endDate}&page=0&size=20`,
        headers
      );
      check(schedulesRes, { 'get schedules': (r) => r.status === 200 });
      
      // Get a doctorId from employees response
      let doctorId = null;
      if (doctorsRes.status === 200) {
        try {
          const doctors = doctorsRes.json('data.content');
          if (doctors && doctors.length > 0) {
            doctorId = doctors[randomInt(0, Math.min(doctors.length - 1, 9))].id;
          }
        } catch (e) {}
      }
      
      sleep(randomInt(3, 5));
      
      // 4. Create appointment (needs patientId, doctorId, appointmentTime, type)
      if (patientId && doctorId) {
        // VU-aware slot assignment: Each VU gets unique time slots
        // Use VU ID + iteration to distribute across days/hours/minutes
        const dayOffset = 1 + (__VU % 7); // Day 1-7 based on VU ID
        const hourOffset = (__VU % 8); // Hour 0-7 (09:00-16:00)
        const minuteSlot = (__ITER % 4) * 15; // 0, 15, 30, 45 based on iteration
        
        const futureDay = new Date();
        futureDay.setDate(futureDay.getDate() + dayOffset);
        futureDay.setHours(9 + hourOffset);
        futureDay.setMinutes(minuteSlot);
        futureDay.setSeconds(__VU % 60); // Add VU-based seconds for uniqueness
        futureDay.setMilliseconds(0);
        const appointmentTime = futureDay.toISOString();
        
        const appointmentPayload = {
          patientId: patientId,
          doctorId: doctorId,
          appointmentTime: appointmentTime,
          type: 'CONSULTATION',
          reason: `Routine checkup - VU${__VU}`,
        };
        
        const createRes = http.post(
          `${BASE_URL}/api/appointments`,
          JSON.stringify(appointmentPayload),
          headers
        );
        
        // Accept 201 (created) or 400 (slot taken/outside schedule - expected at scale)
        const success = createRes.status === 201;
        check(createRes, { 'create appointment': (r) => r.status === 201 || r.status === 400 });
        if (success) {
          appointmentCreated.add(1);
        }
      }
      
      sleep(randomInt(3, 5));
      
      // 5. View my appointments (use /by-patient endpoint)
      if (patientId) {
        const myApptRes = http.get(`${BASE_URL}/api/appointments/by-patient/${patientId}?page=0&size=10`, headers);
        checkAndLog(myApptRes, 'get my appointments', 200, `/api/appointments/by-patient/${patientId}`);
      }
    });
  } else {
    // View Medical Records Flow
    group('Patient - View Records', () => {
      const meRes = http.get(`${BASE_URL}/api/patients/me`, headers);
      check(meRes, { 'get profile': (r) => r.status === 200 });
      
      sleep(randomInt(3, 5));
      
      // Use /api/appointments/all - backend auto-filters for PATIENT role
      const apptRes = http.get(`${BASE_URL}/api/appointments/all?filter=status==SCHEDULED&page=0&size=10`, headers);
      check(apptRes, { 'get appointments': (r) => r.status === 200 });
      
      // 10% of the time: cancel an existing appointment
      if (Math.random() < 0.10 && apptRes.status === 200) {
        try {
          const appointments = apptRes.json('data.content');
          if (appointments && appointments.length > 0) {
            const appointmentToCancel = appointments[randomInt(0, appointments.length - 1)];
            
            const cancelPayload = {
              cancelReason: `Load test cancellation - VU${__VU} - ${new Date().toISOString()}`
            };
            
            const cancelRes = http.patch(
              `${BASE_URL}/api/appointments/${appointmentToCancel.id}/cancel`,
              JSON.stringify(cancelPayload),
              headers
            );
            
            if (checkAndLog(cancelRes, 'cancel appointment', 200, `/api/appointments/${appointmentToCancel.id}/cancel`)) {
              appointmentsCancelled.add(1);
            }
          }
        } catch (e) {}
      }
      
      sleep(randomInt(3, 5));
      
      const medsRes = http.get(`${BASE_URL}/api/medicines?page=0&size=20`, headers);
      check(medsRes, { 'get medicines': (r) => r.status === 200 });
    });
  }
  
  sleep(randomInt(5, 10));
}

// ============================================================================
// DOCTOR Workflow - Examine Patients, Create Exams
// ============================================================================

export function doctorScenario() {
  const doctorNum = (__VU % DOCTOR_ACCOUNTS) + 1;
  const email = `doctor${doctorNum}@hms.com`;
  const password = 'Doctor123!@';
  
  const auth = login(email, password);
  if (!auth) { sleep(5); return; }
  
  const headers = authHeaders(auth.token);
  sleep(randomInt(2, 4));
  
  group('Doctor - Patient Examination', () => {
    // 1. View today's appointments (find ones for completing)
    // Use simple filter without OR to avoid encoding issues
    const apptRes = http.get(`${BASE_URL}/api/appointments/all?page=0&size=10`, headers);
    check(apptRes, { 'get appointments': (r) => r.status === 200 });
    
    let scheduledAppointment = null;
    if (apptRes.status === 200) {
      try {
        const appointments = apptRes.json('data.content');
        if (appointments && appointments.length > 0) {
          scheduledAppointment = appointments[randomInt(0, Math.min(appointments.length - 1, 4))];
        }
      } catch (e) {}
    }
    
    sleep(randomInt(3, 5));
    
    // 2. View patients
    const patientsRes = http.get(`${BASE_URL}/api/patients?page=0&size=20`, headers);
    check(patientsRes, { 'get patients': (r) => r.status === 200 });
    
    let patientId = null;
    if (patientsRes.status === 200) {
      try {
        const patients = patientsRes.json('data.content');
        if (patients && patients.length > 0) {
          patientId = patients[randomInt(0, Math.min(patients.length - 1, 9))].id;
        }
      } catch (e) {}
    }
    
    sleep(randomInt(3, 5));
    
    // 3. Doctor workflow: Complete appointment + Create exam (30% of iterations)
    if (Math.random() < 0.3 && scheduledAppointment) {
      // 3a. Complete the appointment (may fail if another VU already completed it)
      const completeRes = http.patch(
        `${BASE_URL}/api/appointments/${scheduledAppointment.id}/complete`,
        null,
        headers
      );
      
      // Accept 200 (success) or 400 (already completed - race condition)
      const completeSuccess = completeRes.status === 200;
      check(completeRes, { 'complete appointment': (r) => r.status === 200 || r.status === 400 });
      
      if (completeSuccess) {
        appointmentsCompleted.add(1);
        
        // 3b. Create medical exam for the completed appointment
        const examPayload = {
          appointmentId: scheduledAppointment.id,
          diagnosis: `Load test diagnosis - VU${__VU}`,
          symptoms: 'Headache, fatigue',
          treatment: 'Rest and hydration',
          temperature: 36.5 + Math.random() * 1.5,
          bloodPressureSystolic: 110 + randomInt(0, 30),
          bloodPressureDiastolic: 70 + randomInt(0, 20),
          heartRate: 60 + randomInt(0, 40),
          weight: 50 + randomInt(0, 30),
          height: 155 + randomInt(0, 30),
          notes: `Routine checkup - Load test VU${__VU}`,
          hasPrescription: false,
        };
        
        const examRes = http.post(
          `${BASE_URL}/api/exams`,
          JSON.stringify(examPayload),
          headers
        );
        
        if (checkAndLog(examRes, 'create exam', 200, '/api/exams')) {
          examsCreated.add(1);
        }
      }
    } else {
      // Just view existing exams (70% of iterations)
      const examsRes = http.get(`${BASE_URL}/api/exams/all?page=0&size=10`, headers);
      check(examsRes, { 'get exams': (r) => r.status === 200 });
    }
    
    sleep(randomInt(5, 10));
    
    // 4. View medicines
    const medsRes = http.get(`${BASE_URL}/api/medicines?page=0&size=20`, headers);
    check(medsRes, { 'get medicines': (r) => r.status === 200 });
    
    sleep(randomInt(3, 5));
    
    // 5. View my schedule (doctor's own schedule)
    const today = new Date();
    const nextWeek = new Date();
    nextWeek.setDate(nextWeek.getDate() + 7);
    const startDate = today.toISOString().split('T')[0];
    const endDate = nextWeek.toISOString().split('T')[0];
    
    const scheduleRes = http.get(
      `${BASE_URL}/api/hr/schedules/me?startDate=${startDate}&endDate=${endDate}`,
      headers
    );
    check(scheduleRes, { 'get schedule': (r) => r.status === 200 });
  });
  
  sleep(randomInt(10, 20));
}

// ============================================================================
// NURSE Workflow - View Patients, Record Vital Signs, Update Lab Results
// ============================================================================

export function nurseScenario() {
  const nurseNum = (__VU % NURSE_ACCOUNTS) + 1;
  const email = `nurse${nurseNum}@hms.com`;
  const password = 'Nurse123!@';
  
  const auth = login(email, password);
  if (!auth) { sleep(5); return; }
  
  const headers = authHeaders(auth.token);
  sleep(randomInt(2, 4));
  
  group('Nurse - Patient Care', () => {
    // 1. View patients
    const patientsRes = http.get(`${BASE_URL}/api/patients?page=0&size=20`, headers);
    check(patientsRes, { 'get patients': (r) => r.status === 200 });
    
    let patientId = null;
    if (patientsRes.status === 200) {
      try {
        const patients = patientsRes.json('data.content');
        if (patients && patients.length > 0) {
          patientId = patients[randomInt(0, Math.min(patients.length - 1, 9))].id;
        }
      } catch (e) {}
    }
    
    sleep(randomInt(3, 5));
    
    // 2. View medical exams (correct endpoint: /api/exams/all)
    const examsRes = http.get(`${BASE_URL}/api/exams/all?page=0&size=20`, headers);
    check(examsRes, { 'get exams': (r) => r.status === 200 });
    
    let examId = null;
    if (examsRes.status === 200) {
      try {
        const exams = examsRes.json('data.content');
        if (exams && exams.length > 0) {
          examId = exams[randomInt(0, Math.min(exams.length - 1, 4))].id;
        }
      } catch (e) {}
    }
    
    sleep(randomInt(3, 5));
    
    // 3. View lab results (update vitals endpoint doesn't exist - skipped)
    // Nurses view lab results instead
    sleep(randomInt(2, 4));
    
    sleep(randomInt(5, 10));
    
    // 4. View lab test results (correct endpoint: /exams/lab-results/all)
    const labRes = http.get(`${BASE_URL}/api/exams/lab-results/all?page=0&size=20`, headers);
    checkAndLog(labRes, 'get lab results', 200, '/api/exams/lab-results/all');
    
    sleep(randomInt(3, 5));
    
    // 5. View appointments for context
    const apptRes = http.get(`${BASE_URL}/api/appointments/all?page=0&size=10`, headers);
    check(apptRes, { 'get appointments': (r) => r.status === 200 });
  });
  
  sleep(randomInt(5, 15));
}

// ============================================================================
// RECEPTIONIST Workflow - Walk-in Registration, Billing
// ============================================================================

export function receptionistScenario() {
  const receptionistNum = (__VU % RECEPTIONIST_ACCOUNTS) + 1;
  const email = `receptionist${receptionistNum}@hms.com`;
  const password = 'Receptionist123!@';
  
  const auth = login(email, password);
  if (!auth) { sleep(5); return; }
  
  const headers = authHeaders(auth.token);
  sleep(randomInt(2, 4));
  
  // 33% Walk-in, 33% Booking, 33% Billing
  const scenario = randomInt(1, 3);
  
  if (scenario === 1) {
    group('Receptionist - Walk-in', () => {
      const searchRes = http.get(`${BASE_URL}/api/patients?page=0&size=10`, headers);
      check(searchRes, { 'search patients': (r) => r.status === 200 });
      
      sleep(randomInt(2, 4));
      
      const doctorsRes = http.get(`${BASE_URL}/api/hr/employees/all?filter=role==DOCTOR&page=0&size=10`, headers);
      check(doctorsRes, { 'get doctors': (r) => r.status === 200 });
      
      sleep(randomInt(2, 4));
      
      let patientId = null;
      if (searchRes.status === 200) {
        try {
          const patients = searchRes.json('data.content');
          if (patients && patients.length > 0) {
            patientId = patients[randomInt(0, Math.min(patients.length - 1, 4))].id;
          }
        } catch (e) {}
      }
      
      // Get a doctor for walk-in
      let doctorId = null;
      if (doctorsRes.status === 200) {
        try {
          const doctors = doctorsRes.json('data.content');
          if (doctors && doctors.length > 0) {
            doctorId = doctors[randomInt(0, Math.min(doctors.length - 1, 9))].id;
          }
        } catch (e) {}
      }
      
      if (patientId && doctorId) {
        // Walk-in uses special endpoint with WalkInRequest DTO
        const walkInPayload = {
          patientId: patientId,
          doctorId: doctorId,
          reason: `Walk-in - VU${__VU}`,
        };
        
        const createRes = http.post(
          `${BASE_URL}/api/appointments/walk-in`,
          JSON.stringify(walkInPayload),
          headers
        );
        
        if (check(createRes, { 'create walk-in': (r) => r.status === 200 || r.status === 201 })) {
          appointmentCreated.add(1);
        }
      }
    });
  } else if (scenario === 2) {
    group('Receptionist - Booking', () => {
      const searchRes = http.get(`${BASE_URL}/api/patients?page=0&size=20`, headers);
      check(searchRes, { 'search patients': (r) => r.status === 200 });
      
      sleep(randomInt(2, 4));
      
      // Use /doctors endpoint with proper date params
      const today = new Date();
      const nextWeek = new Date();
      nextWeek.setDate(nextWeek.getDate() + 7);
      const startDate = today.toISOString().split('T')[0];
      const endDate = nextWeek.toISOString().split('T')[0];
      
      const schedulesRes = http.get(
        `${BASE_URL}/api/hr/schedules/doctors?startDate=${startDate}&endDate=${endDate}&page=0&size=20`,
        headers
      );
      check(schedulesRes, { 'get schedules': (r) => r.status === 200 });
      
      sleep(randomInt(2, 4));
      
      const doctorsRes = http.get(`${BASE_URL}/api/hr/employees/all?filter=role==DOCTOR&page=0&size=10`, headers);
      check(doctorsRes, { 'get doctors': (r) => r.status === 200 });
    });
  } else {
    group('Receptionist - Billing', () => {
      const apptRes = http.get(`${BASE_URL}/api/appointments/all?page=0&size=20`, headers);
      check(apptRes, { 'get appointments': (r) => r.status === 200 });
      
      sleep(randomInt(2, 4));
      
      // Get PENDING invoices for payment processing
      const invoicesRes = http.get(`${BASE_URL}/api/invoices?status=PENDING&page=0&size=20`, headers);
      checkAndLog(invoicesRes, 'get invoices', 200, '/api/invoices');
      
      let pendingInvoice = null;
      if (invoicesRes.status === 200) {
        try {
          const invoices = invoicesRes.json('data.content');
          if (invoices && invoices.length > 0) {
            pendingInvoice = invoices[randomInt(0, Math.min(invoices.length - 1, 4))];
          }
        } catch (e) {}
      }
      
      sleep(randomInt(2, 4));
      
      // 50% of the time: record cash payment for pending invoice
      if (Math.random() < 0.5 && pendingInvoice) {
        const cashPaymentRes = http.post(
          `${BASE_URL}/api/payments/${pendingInvoice.id}/cash`,
          null,
          headers
        );
        
        if (checkAndLog(cashPaymentRes, 'record payment', 200, `/api/payments/${pendingInvoice.id}/cash`)) {
          paymentsRecorded.add(1);
        }
      } else {
        // Just list all invoices
        const invoiceListRes = http.get(`${BASE_URL}/api/invoices/all?page=0&size=10`, headers);
        check(invoiceListRes, { 'list invoices': (r) => r.status === 200 });
      }
    });
  }
  
  sleep(randomInt(3, 8));
}

// ============================================================================
// ADMIN Workflow - Management, Reports
// ============================================================================

export function adminScenario() {
  const email = 'admin1@hms.com';
  const password = 'Admin123!@';
  
  const auth = login(email, password);
  if (!auth) { sleep(5); return; }
  
  const headers = authHeaders(auth.token);
  sleep(randomInt(2, 4));
  
  const scenario = randomInt(1, 4);
  
  if (scenario === 1) {
    group('Admin - Staff Management', () => {
      const empRes = http.get(`${BASE_URL}/api/hr/employees/all?page=0&size=50`, headers);
      check(empRes, { 'get employees': (r) => r.status === 200 });
      
      sleep(randomInt(3, 5));
      
      const deptRes = http.get(`${BASE_URL}/api/hr/departments/all?page=0&size=20`, headers);
      check(deptRes, { 'get departments': (r) => r.status === 200 });
    });
  } else if (scenario === 2) {
    group('Admin - Schedule Management', () => {
      const schedRes = http.get(`${BASE_URL}/api/hr/schedules/all?page=0&size=50`, headers);
      checkAndLog(schedRes, 'get schedules', 200, '/api/hr/schedules/all');
      
      sleep(randomInt(3, 5));
      
      const doctorsRes = http.get(`${BASE_URL}/api/hr/employees/all?filter=role==DOCTOR&page=0&size=20`, headers);
      check(doctorsRes, { 'get doctors': (r) => r.status === 200 });
    });
  } else if (scenario === 3) {
    group('Admin - Medicine Inventory', () => {
      const medsRes = http.get(`${BASE_URL}/api/medicines?page=0&size=50`, headers);
      check(medsRes, { 'get medicines': (r) => r.status === 200 });
      
      sleep(randomInt(3, 5));
      
      const catRes = http.get(`${BASE_URL}/api/medicines/categories?all=true`, headers);
      check(catRes, { 'get categories': (r) => r.status === 200 });
    });
  } else {
    group('Admin - Reports', () => {
      const patientsRes = http.get(`${BASE_URL}/api/patients?page=0&size=100`, headers);
      check(patientsRes, { 'get patients': (r) => r.status === 200 });
      
      sleep(randomInt(3, 5));
      
      const apptRes = http.get(`${BASE_URL}/api/appointments/all?page=0&size=50`, headers);
      check(apptRes, { 'get appointments': (r) => r.status === 200 });
      
      sleep(randomInt(3, 5));
      
      const accRes = http.get(`${BASE_URL}/api/auth/accounts/all?page=0&size=50`, headers);
      checkAndLog(accRes, 'get accounts', 200, '/api/auth/accounts/all');
    });
  }
  
  sleep(randomInt(10, 30));
}
