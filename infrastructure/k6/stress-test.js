/**
 * HMS Backend - Stress Test Script
 * 
 * Purpose: Find the breaking point and test recovery behavior
 * Pattern: Escalating load until system degrades, then recovery phase
 * 
 * Usage:
 *   k6 run stress-test.js
 *   k6 run -e MAX_VUS=2000 stress-test.js
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const MAX_VUS = parseInt(__ENV.MAX_VUS) || 2000;
const DEBUG = __ENV.DEBUG || 'false';

// Account pools (from seed-1000vu.ps1)
const PATIENT_ACCOUNTS = 600;
const DOCTOR_ACCOUNTS = 200;
const NURSE_ACCOUNTS = 150;
const RECEPTIONIST_ACCOUNTS = 125;

// Metrics
const loginSuccess = new Rate('login_success');
const loginTime = new Trend('login_time');
const errors4xx = new Counter('errors_4xx');
const errors5xx = new Counter('errors_5xx');
const recoveryTime = new Trend('recovery_time');

// Stress test scenario: Escalate until breaking, then recover
export const options = {
  scenarios: {
    stress_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        // Phase 1: Baseline (warm-up)
        { duration: '1m', target: 500 },
        
        // Phase 2: Normal load
        { duration: '2m', target: 1000 },
        
        // Phase 3: Stress (above normal)
        { duration: '2m', target: Math.round(MAX_VUS * 0.75) },
        
        // Phase 4: Peak stress (breaking point?)
        { duration: '2m', target: MAX_VUS },
        
        // Phase 5: RECOVERY - back to normal load
        { duration: '2m', target: 500 },
        
        // Phase 6: Verify recovery
        { duration: '1m', target: 500 },
        
        // Phase 7: Ramp down
        { duration: '1m', target: 0 },
      ],
      exec: 'stressScenario',
    },
  },
  thresholds: {
    // Stress test thresholds are more lenient
    http_req_duration: ['p(95)<10000'],  // 10s (allow degradation)
    http_req_failed: ['rate<0.50'],       // 50% (expect failures at peak)
    'login_success': ['rate>0.80'],       // 80% (allow failures at peak)
    'errors_5xx': ['count<500'],          // Tolerate some 5xx at peak
  },
};

// Log configuration at startup
export function setup() {
  console.log(`
================================================================================
HMS Backend STRESS TEST Configuration
================================================================================
Max VUs: ${MAX_VUS}
Base URL: ${BASE_URL}
Debug Mode: ${DEBUG}

Stress Test Phases:
  1. Baseline (1m):     0 → 500 VUs
  2. Normal (2m):       500 → 1000 VUs
  3. Stress (2m):       1000 → ${Math.round(MAX_VUS * 0.75)} VUs
  4. Peak (2m):         ${Math.round(MAX_VUS * 0.75)} → ${MAX_VUS} VUs
  5. Recovery (2m):     ${MAX_VUS} → 500 VUs
  6. Verify (1m):       500 VUs (should match baseline)
  7. Ramp down (1m):    500 → 0 VUs

What to Watch:
  - Response time increase during phases 3-4
  - Error rate spike at peak
  - Recovery: metrics should return to baseline in phase 5-6
  - No 5xx errors after recovery
================================================================================
`);
  return {};
}

// Helper functions
function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function login(email, password) {
  const start = Date.now();
  const res = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ email, password }),
    { headers: { 'Content-Type': 'application/json' }, timeout: '30s' }
  );
  
  loginTime.add(Date.now() - start);
  
  if (res.status === 200) {
    try {
      const data = res.json('data');
      if (data && data.accessToken) {
        loginSuccess.add(1);
        return { token: data.accessToken };
      }
    } catch (e) {}
  }
  
  loginSuccess.add(0);
  if (res.status >= 400 && res.status < 500) errors4xx.add(1);
  if (res.status >= 500) errors5xx.add(1);
  return null;
}

function authHeaders(token) {
  return { headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` } };
}

// Main stress scenario - simulates mixed workload
export function stressScenario() {
  // Determine role based on VU distribution
  const role = getRole(__VU);
  const credentials = getCredentials(role, __VU);
  
  const auth = login(credentials.email, credentials.password);
  if (!auth) {
    sleep(2);
    return;
  }
  
  const headers = authHeaders(auth.token);
  const recoveryStart = Date.now();
  
  // Execute role-based operations
  group(`${role} - Operations`, () => {
    switch (role) {
      case 'PATIENT':
        patientOps(headers);
        break;
      case 'DOCTOR':
        doctorOps(headers);
        break;
      case 'NURSE':
        nurseOps(headers);
        break;
      case 'RECEPTIONIST':
        receptionistOps(headers);
        break;
      case 'ADMIN':
        adminOps(headers);
        break;
    }
  });
  
  recoveryTime.add(Date.now() - recoveryStart);
  sleep(randomInt(1, 3));
}

function getRole(vu) {
  // 55% Patient, 18% Doctor, 14% Nurse, 12% Receptionist, 1% Admin
  const r = vu % 100;
  if (r < 55) return 'PATIENT';
  if (r < 73) return 'DOCTOR';
  if (r < 87) return 'NURSE';
  if (r < 99) return 'RECEPTIONIST';
  return 'ADMIN';
}

function getCredentials(role, vu) {
  switch (role) {
    case 'PATIENT':
      return { email: `patient${(vu % PATIENT_ACCOUNTS) + 1}@email.com`, password: 'Patient123!@' };
    case 'DOCTOR':
      return { email: `doctor${(vu % DOCTOR_ACCOUNTS) + 1}@hms.com`, password: 'Doctor123!@' };
    case 'NURSE':
      return { email: `nurse${(vu % NURSE_ACCOUNTS) + 1}@hms.com`, password: 'Nurse123!@' };
    case 'RECEPTIONIST':
      return { email: `receptionist${(vu % RECEPTIONIST_ACCOUNTS) + 1}@hms.com`, password: 'Receptionist123!@' };
    case 'ADMIN':
      return { email: `admin${(vu % 10) + 1}@hms.com`, password: 'Admin123!@' };
  }
}

function patientOps(headers) {
  // Read-heavy patient operations
  http.get(`${BASE_URL}/api/patients/me`, headers);
  sleep(randomInt(1, 2));
  
  http.get(`${BASE_URL}/api/appointments/all?page=0&size=10`, headers);
  sleep(randomInt(1, 2));
  
  http.get(`${BASE_URL}/api/medicines?page=0&size=20`, headers);
}

function doctorOps(headers) {
  // Doctor operations
  http.get(`${BASE_URL}/api/appointments/all?page=0&size=10`, headers);
  sleep(randomInt(1, 2));
  
  http.get(`${BASE_URL}/api/patients?page=0&size=20`, headers);
  sleep(randomInt(1, 2));
  
  http.get(`${BASE_URL}/api/exams/all?page=0&size=10`, headers);
}

function nurseOps(headers) {
  // Nurse operations
  http.get(`${BASE_URL}/api/patients?page=0&size=20`, headers);
  sleep(randomInt(1, 2));
  
  http.get(`${BASE_URL}/api/exams/lab-results/all?page=0&size=10`, headers);
  sleep(randomInt(1, 2));
  
  http.get(`${BASE_URL}/api/appointments/all?page=0&size=10`, headers);
}

function receptionistOps(headers) {
  // Receptionist operations
  http.get(`${BASE_URL}/api/patients?page=0&size=20`, headers);
  sleep(randomInt(1, 2));
  
  http.get(`${BASE_URL}/api/appointments/all?page=0&size=20`, headers);
  sleep(randomInt(1, 2));
  
  http.get(`${BASE_URL}/api/invoices?page=0&size=20`, headers);
}

function adminOps(headers) {
  // Admin operations - heavy reads
  http.get(`${BASE_URL}/api/auth/accounts/all?page=0&size=50`, headers);
  sleep(randomInt(1, 2));
  
  http.get(`${BASE_URL}/api/hr/employees/all?page=0&size=50`, headers);
  sleep(randomInt(1, 2));
  
  http.get(`${BASE_URL}/api/hr/departments/all?page=0&size=20`, headers);
}

export function handleSummary(data) {
  // Custom summary for stress test analysis
  const p95 = data.metrics.http_req_duration?.values?.['p(95)'] || 0;
  const failRate = data.metrics.http_req_failed?.values?.rate || 0;
  const loginRate = data.metrics.login_success?.values?.rate || 0;
  
  console.log(`
================================================================================
STRESS TEST RESULTS SUMMARY
================================================================================
Peak VUs: ${MAX_VUS}
Total Requests: ${data.metrics.http_reqs?.values?.count || 0}
Request Rate: ${(data.metrics.http_reqs?.values?.rate || 0).toFixed(2)} req/s

Performance:
  - Response Time p(95): ${p95.toFixed(2)}ms
  - Failure Rate: ${(failRate * 100).toFixed(2)}%
  - Login Success: ${(loginRate * 100).toFixed(2)}%

Errors:
  - 4xx Errors: ${data.metrics.errors_4xx?.values?.count || 0}
  - 5xx Errors: ${data.metrics.errors_5xx?.values?.count || 0}

Recovery Analysis:
  Check if metrics returned to baseline during recovery phase (compare console output)
================================================================================
`);
  
  return {
    'stdout': '',
  };
}
