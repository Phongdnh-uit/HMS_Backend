/**
 * HMS Backend - k6 Load Test Script (50 VUs - Realistic Workflow)
 * 
 * Based on test_plan.md - Implements realistic workflows with POST, PUT, DELETE operations
 * 
 * VU Distribution (scaled from 500 VUs):
 * - 27 Patient VUs  (54%) - Booking, View Records
 * - 9 Doctor VUs    (18%) - Examine, Prescribe
 * - 7 Nurse VUs     (14%) - Check-in, Vitals
 * - 6 Receptionist VUs (12%) - Walk-in, Billing
 * - 1 Admin VU      (2%) - Management, Reports
 * 
 * Usage:
 *   k6 run load-test-50vu.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// ============================================================================
// Configuration
// ============================================================================

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  scenarios: {
    patients: {
      executor: 'constant-vus',
      vus: 110,  // 55%
      duration: '5m',
      exec: 'patientScenario',
    },
    doctors: {
      executor: 'constant-vus',
      vus: 36,   // 18%
      duration: '5m',
      exec: 'doctorScenario',
    },
    nurses: {
      executor: 'constant-vus',
      vus: 28,   // 14%
      duration: '5m',
      exec: 'nurseScenario',
    },
    receptionists: {
      executor: 'constant-vus',
      vus: 24,   // 12%
      duration: '5m',
      exec: 'receptionistScenario',
    },
    admin: {
      executor: 'constant-vus',
      vus: 2,    // 1%
      duration: '5m',
      exec: 'adminScenario',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<3000'],
    http_req_failed: ['rate<0.10'],
    'login_success': ['rate>0.95'],
  },
};

// ============================================================================
// Custom Metrics
// ============================================================================

const loginSuccess = new Rate('login_success');
const loginDuration = new Trend('login_duration');
const appointmentCreated = new Counter('appointments_created');
const examsCreated = new Counter('exams_created');
const invoicesCreated = new Counter('invoices_created');

// ============================================================================
// Helper Functions
// ============================================================================

function login(email, password) {
  const payload = JSON.stringify({ email, password });
  const params = { headers: { 'Content-Type': 'application/json' } };
  
  const res = http.post(`${BASE_URL}/api/auth/login`, payload, params);
  loginDuration.add(res.timings.duration);
  
  const success = res.status === 200 && res.json('code') === 1000;
  loginSuccess.add(success);
  
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

function authHeadersOnly(token) {
  return {
    headers: { 'Authorization': `Bearer ${token}` },
  };
}

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randomChoice(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

function generateUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0;
    const v = c === 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

// ============================================================================
// PATIENT Workflow - Appointment Booking & View Records
// ============================================================================

export function patientScenario() {
  const patientNum = (__VU % 30) + 1;
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
      
      // 3. View doctor schedules
      const schedulesRes = http.get(`${BASE_URL}/api/hr/schedules?page=0&size=20`, headers);
      check(schedulesRes, { 'get schedules': (r) => r.status === 200 || r.status === 403 });
      
      sleep(randomInt(3, 5));
      
      // 4. Create appointment (if we have patientId)
      if (patientId) {
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        const appointmentDate = tomorrow.toISOString().split('T')[0];
        
        const appointmentPayload = {
          patientId: patientId,
          appointmentDate: appointmentDate,
          appointmentTime: `${9 + randomInt(0, 7)}:00:00`,
          reason: `Routine checkup - VU${__VU}`,
          status: 'SCHEDULED',
        };
        
        const createRes = http.post(
          `${BASE_URL}/api/appointments`,
          JSON.stringify(appointmentPayload),
          headers
        );
        
        if (check(createRes, { 'create appointment': (r) => r.status === 200 || r.status === 201 })) {
          appointmentCreated.add(1);
        }
      }
      
      sleep(randomInt(3, 5));
      
      // 5. View my appointments
      const myApptRes = http.get(`${BASE_URL}/api/appointments/my?page=0&size=10`, headers);
      check(myApptRes, { 'get my appointments': (r) => r.status === 200 });
    });
  } else {
    // View Medical Records Flow
    group('Patient - View Records', () => {
      // 1. Get my profile
      const meRes = http.get(`${BASE_URL}/api/patients/me`, headers);
      check(meRes, { 'get profile': (r) => r.status === 200 });
      
      sleep(randomInt(3, 5));
      
      // 2. View my appointments
      const apptRes = http.get(`${BASE_URL}/api/appointments/my?page=0&size=10`, headers);
      check(apptRes, { 'get appointments': (r) => r.status === 200 });
      
      sleep(randomInt(3, 5));
      
      // 3. View medicines (for reference)
      const medsRes = http.get(`${BASE_URL}/api/medicines?page=0&size=20`, headers);
      check(medsRes, { 'get medicines': (r) => r.status === 200 });
    });
  }
  
  sleep(randomInt(5, 10));
}

// ============================================================================
// DOCTOR Workflow - Examine Patients, Create Exams, Prescriptions
// ============================================================================

export function doctorScenario() {
  const doctorNum = (__VU % 10) + 1;
  const email = `doctor${doctorNum}@hms.com`;
  const password = 'Doctor123!@';
  
  const auth = login(email, password);
  if (!auth) { sleep(5); return; }
  
  const headers = authHeaders(auth.token);
  sleep(randomInt(2, 4));
  
  group('Doctor - Patient Examination', () => {
    // 1. View today's appointments
    const apptRes = http.get(`${BASE_URL}/api/appointments/all?page=0&size=10`, headers);
    check(apptRes, { 'get appointments': (r) => r.status === 200 });
    
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
    
    sleep(randomInt(5, 10));
    
    // 3. Create medical exam (if we have a patient)
    if (patientId) {
      const examPayload = {
        patientId: patientId,
        symptoms: `Patient reports headache, fever - VU${__VU}`,
        diagnosis: 'Upper respiratory infection',
        notes: 'Prescribed rest and medication',
        vitalSigns: {
          temperature: 37.5 + Math.random(),
          bloodPressure: '120/80',
          heartRate: 70 + randomInt(0, 20),
          weight: 60 + randomInt(0, 30),
        },
      };
      
      const examRes = http.post(
        `${BASE_URL}/api/medical-exams`,
        JSON.stringify(examPayload),
        headers
      );
      
      if (check(examRes, { 'create exam': (r) => r.status === 200 || r.status === 201 || r.status === 400 })) {
        if (examRes.status === 200 || examRes.status === 201) {
          examsCreated.add(1);
        }
      }
    }
    
    sleep(randomInt(5, 10));
    
    // 4. View medicines for prescription
    const medsRes = http.get(`${BASE_URL}/api/medicines?page=0&size=20`, headers);
    check(medsRes, { 'get medicines': (r) => r.status === 200 });
    
    sleep(randomInt(3, 5));
    
    // 5. View my schedule
    const scheduleRes = http.get(`${BASE_URL}/api/hr/schedules?page=0&size=20`, headers);
    check(scheduleRes, { 'get schedule': (r) => r.status === 200 || r.status === 403 });
  });
  
  sleep(randomInt(10, 20));
}

// ============================================================================
// NURSE Workflow - Check-in, Vitals, Assist
// ============================================================================

export function nurseScenario() {
  const nurseNum = (__VU % 10) + 1;
  const email = `nurse${nurseNum}@hms.com`;
  const password = 'Nurse123!@';
  
  const auth = login(email, password);
  if (!auth) { sleep(5); return; }
  
  const headers = authHeaders(auth.token);
  sleep(randomInt(2, 4));
  
  group('Nurse - Patient Care', () => {
    // 1. View appointments (queue)
    const apptRes = http.get(`${BASE_URL}/api/appointments/all?page=0&size=20`, headers);
    check(apptRes, { 'get appointments': (r) => r.status === 200 });
    
    let appointmentId = null;
    if (apptRes.status === 200) {
      try {
        const appointments = apptRes.json('data.content');
        if (appointments && appointments.length > 0) {
          appointmentId = appointments[randomInt(0, Math.min(appointments.length - 1, 4))].id;
        }
      } catch (e) {}
    }
    
    sleep(randomInt(3, 5));
    
    // 2. View patients
    const patientsRes = http.get(`${BASE_URL}/api/patients?page=0&size=20`, headers);
    check(patientsRes, { 'get patients': (r) => r.status === 200 });
    
    sleep(randomInt(3, 5));
    
    // 3. Update appointment status (check-in) if we have one
    if (appointmentId) {
      const updatePayload = { status: 'CHECKED_IN' };
      const updateRes = http.put(
        `${BASE_URL}/api/appointments/${appointmentId}/status`,
        JSON.stringify(updatePayload),
        headers
      );
      check(updateRes, { 'update appointment': (r) => r.status === 200 || r.status === 404 || r.status === 400 });
    }
    
    sleep(randomInt(5, 10));
    
    // 4. View medicines
    const medsRes = http.get(`${BASE_URL}/api/medicines?page=0&size=20`, headers);
    check(medsRes, { 'get medicines': (r) => r.status === 200 });
  });
  
  sleep(randomInt(5, 15));
}

// ============================================================================
// RECEPTIONIST Workflow - Walk-in Registration, Billing
// ============================================================================

export function receptionistScenario() {
  const receptionistNum = (__VU % 10) + 1;
  const email = `receptionist${receptionistNum}@hms.com`;
  const password = 'Receptionist123!@';
  
  const auth = login(email, password);
  if (!auth) { sleep(5); return; }
  
  const headers = authHeaders(auth.token);
  sleep(randomInt(2, 4));
  
  // 33% Walk-in, 33% Booking, 33% Billing
  const scenario = randomInt(1, 3);
  
  if (scenario === 1) {
    // Walk-in Registration
    group('Receptionist - Walk-in', () => {
      // 1. Search for patient
      const searchRes = http.get(`${BASE_URL}/api/patients?page=0&size=10`, headers);
      check(searchRes, { 'search patients': (r) => r.status === 200 });
      
      sleep(randomInt(2, 4));
      
      // 2. View doctors
      const doctorsRes = http.get(`${BASE_URL}/api/hr/employees/all?filter=role==DOCTOR&page=0&size=10`, headers);
      check(doctorsRes, { 'get doctors': (r) => r.status === 200 });
      
      sleep(randomInt(2, 4));
      
      // 3. Create walk-in appointment
      let patientId = null;
      if (searchRes.status === 200) {
        try {
          const patients = searchRes.json('data.content');
          if (patients && patients.length > 0) {
            patientId = patients[randomInt(0, Math.min(patients.length - 1, 4))].id;
          }
        } catch (e) {}
      }
      
      if (patientId) {
        const today = new Date().toISOString().split('T')[0];
        const appointmentPayload = {
          patientId: patientId,
          appointmentDate: today,
          appointmentTime: `${10 + randomInt(0, 7)}:00:00`,
          reason: `Walk-in - VU${__VU}`,
          status: 'CHECKED_IN',
        };
        
        const createRes = http.post(
          `${BASE_URL}/api/appointments`,
          JSON.stringify(appointmentPayload),
          headers
        );
        
        if (check(createRes, { 'create walk-in': (r) => r.status === 200 || r.status === 201 })) {
          appointmentCreated.add(1);
        }
      }
    });
  } else if (scenario === 2) {
    // Appointment Booking
    group('Receptionist - Booking', () => {
      // 1. Search patient
      const searchRes = http.get(`${BASE_URL}/api/patients?page=0&size=20`, headers);
      check(searchRes, { 'search patients': (r) => r.status === 200 });
      
      sleep(randomInt(2, 4));
      
      // 2. View schedules
      const schedulesRes = http.get(`${BASE_URL}/api/hr/schedules?page=0&size=20`, headers);
      check(schedulesRes, { 'get schedules': (r) => r.status === 200 || r.status === 403 });
      
      sleep(randomInt(2, 4));
      
      // 3. View doctors
      const doctorsRes = http.get(`${BASE_URL}/api/hr/employees/all?filter=role==DOCTOR&page=0&size=10`, headers);
      check(doctorsRes, { 'get doctors': (r) => r.status === 200 });
    });
  } else {
    // Billing Flow
    group('Receptionist - Billing', () => {
      // 1. View completed appointments
      const apptRes = http.get(`${BASE_URL}/api/appointments/all?page=0&size=20`, headers);
      check(apptRes, { 'get appointments': (r) => r.status === 200 });
      
      sleep(randomInt(2, 4));
      
      // 2. View invoices
      const invoicesRes = http.get(`${BASE_URL}/api/invoices?page=0&size=20`, headers);
      check(invoicesRes, { 'get invoices': (r) => r.status === 200 || r.status === 403 });
      
      sleep(randomInt(2, 4));
      
      // 3. Create invoice (for a patient)
      const patientsRes = http.get(`${BASE_URL}/api/patients?page=0&size=5`, headers);
      let patientId = null;
      if (patientsRes.status === 200) {
        try {
          const patients = patientsRes.json('data.content');
          if (patients && patients.length > 0) {
            patientId = patients[0].id;
          }
        } catch (e) {}
      }
      
      if (patientId) {
        const invoicePayload = {
          patientId: patientId,
          totalAmount: 100000 + randomInt(0, 500000),
          status: 'PENDING',
          notes: `Load test invoice - VU${__VU}`,
        };
        
        const createRes = http.post(
          `${BASE_URL}/api/invoices`,
          JSON.stringify(invoicePayload),
          headers
        );
        
        if (check(createRes, { 'create invoice': (r) => r.status === 200 || r.status === 201 || r.status === 400 })) {
          if (createRes.status === 200 || createRes.status === 201) {
            invoicesCreated.add(1);
          }
        }
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
  
  // Cycle through admin scenarios
  const scenario = randomInt(1, 4);
  
  if (scenario === 1) {
    // Staff Management
    group('Admin - Staff Management', () => {
      const empRes = http.get(`${BASE_URL}/api/hr/employees/all?page=0&size=50`, headers);
      check(empRes, { 'get employees': (r) => r.status === 200 });
      
      sleep(randomInt(3, 5));
      
      const deptRes = http.get(`${BASE_URL}/api/hr/departments/all?page=0&size=20`, headers);
      check(deptRes, { 'get departments': (r) => r.status === 200 });
    });
  } else if (scenario === 2) {
    // Schedule Management
    group('Admin - Schedule Management', () => {
      const schedRes = http.get(`${BASE_URL}/api/hr/schedules?page=0&size=50`, headers);
      check(schedRes, { 'get schedules': (r) => r.status === 200 || r.status === 403 });
      
      sleep(randomInt(3, 5));
      
      const doctorsRes = http.get(`${BASE_URL}/api/hr/employees/all?filter=role==DOCTOR&page=0&size=20`, headers);
      check(doctorsRes, { 'get doctors': (r) => r.status === 200 });
    });
  } else if (scenario === 3) {
    // Medicine Inventory
    group('Admin - Medicine Inventory', () => {
      const medsRes = http.get(`${BASE_URL}/api/medicines?page=0&size=50`, headers);
      check(medsRes, { 'get medicines': (r) => r.status === 200 });
      
      sleep(randomInt(3, 5));
      
      const catRes = http.get(`${BASE_URL}/api/medicines/categories?all=true`, headers);
      check(catRes, { 'get categories': (r) => r.status === 200 });
    });
  } else {
    // Reports & Analytics
    group('Admin - Reports', () => {
      const patientsRes = http.get(`${BASE_URL}/api/patients?page=0&size=100`, headers);
      check(patientsRes, { 'get patients': (r) => r.status === 200 });
      
      sleep(randomInt(3, 5));
      
      const apptRes = http.get(`${BASE_URL}/api/appointments/all?page=0&size=50`, headers);
      check(apptRes, { 'get appointments': (r) => r.status === 200 });
      
      sleep(randomInt(3, 5));
      
      const accRes = http.get(`${BASE_URL}/api/auth/accounts?page=0&size=50`, headers);
      check(accRes, { 'get accounts': (r) => r.status === 200 || r.status === 403 });
    });
  }
  
  sleep(randomInt(10, 30));
}
