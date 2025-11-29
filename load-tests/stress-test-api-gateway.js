import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';
import { htmlReport } from "https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js";
import { textSummary } from "https://jslib.k6.io/k6-summary/0.0.1/index.js";

// Custom metrics
const errorRate = new Rate('errors');

// Stress Test Configuration - Simple health check only
export const options = {
  stages: [
    // Ramp up: Tăng dần từ 0 → 100 VUs trong 1 phút
    { duration: '1m', target: 100 },

    // Ramp up: Tăng từ 100 → 200 VUs trong 1 phút
    { duration: '1m', target: 200 },

    // Ramp up: Tăng từ 200 → 400 VUs trong 1 phút
    { duration: '1m', target: 400 },

    // Ramp up: Tăng từ 400 → 800 VUs trong 1 phút
    { duration: '1m', target: 800 },

    // Spike: Tăng đột ngột lên 1000 VUs trong 30s
    { duration: '30s', target: 1000 },

    // Hold: Giữ ở 1000 VUs trong 1.5 phút
    { duration: '1m30s', target: 1000 },

    // Ramp down: Giảm xuống 0 trong 1 phút
    { duration: '1m', target: 0 },
  ],

  thresholds: {
    // 95% requests phải < 500ms, 99% < 1s
    'http_req_duration{expected_response:true}': ['p(95)<500', 'p(99)<1000'],

    // Error rate phải < 5%
    errors: ['rate<0.05'],

    // 95% requests phải thành công
    http_req_failed: ['rate<0.05'],
  },
};

const BASE_URL = 'http://localhost:8080';

// Simple test - chỉ test health endpoint
export default function () {
  const res = http.get(`${BASE_URL}/actuator/health`);

  check(res, {
    'health check status is 200': (r) => r.status === 200,
  }) || errorRate.add(1);

  sleep(0.1); // Short sleep between requests
}

// Export report sau khi test
export function handleSummary(data) {
  return {
    // HTML report
    "load-tests/stress-api-gateway-report.html": htmlReport(data),

    // JSON report
    "load-tests/stress-api-gateway-report.json": JSON.stringify(data, null, 2),

    // Text summary
    "load-tests/stress-api-gateway-summary.txt": textSummary(data, { indent: " ", enableColors: false }),

    // Console output
    stdout: textSummary(data, { indent: " ", enableColors: true }),
  };
}
