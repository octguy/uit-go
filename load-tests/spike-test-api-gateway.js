import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';
import { htmlReport } from "https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js";
import { textSummary } from "https://jslib.k6.io/k6-summary/0.0.1/index.js";

// Custom metrics
const errorRate = new Rate('errors');

// EXTREME Spike Test - Tìm breaking point
export const options = {
  stages: [
    // Warm up: 0 → 100 VUs trong 30s
    { duration: '30s', target: 100 },

    // SPIKE 1: Tăng đột ngột lên 500 VUs trong 10s
    { duration: '10s', target: 500 },

    // Hold: Giữ 500 VUs trong 30s
    { duration: '30s', target: 500 },

    // SPIKE 2: Tăng đột ngột lên 1000 VUs trong 10s
    { duration: '10s', target: 1000 },

    // Hold: Giữ 1000 VUs trong 1 phút
    { duration: '1m', target: 1000 },

    // SPIKE 3: EXTREME - Tăng lên 2000 VUs trong 10s
    { duration: '10s', target: 2000 },

    // Hold: Giữ 2000 VUs trong 1 phút (test breaking point)
    { duration: '1m', target: 2000 },

    // SPIKE 4: MAXIMUM - Tăng lên 3000 VUs trong 10s
    { duration: '10s', target: 3000 },

    // Hold: Giữ 3000 VUs trong 1 phút
    { duration: '1m', target: 3000 },

    // Ramp down: Giảm xuống 0 trong 30s
    { duration: '30s', target: 0 },
  ],

  thresholds: {
    // 95% requests phải < 1s (nới lỏng hơn)
    'http_req_duration{expected_response:true}': ['p(95)<1000'],

    // 99% requests phải < 2s
    'http_req_duration{expected_response:true}': ['p(99)<2000'],

    // Error rate phải < 10% (cho phép fail một chút)
    'errors': ['rate<0.10'],

    // Failed requests < 10%
    'http_req_failed': ['rate<0.10'],
  },
};

const BASE_URL = 'http://localhost:8080';

// Simple test - chỉ test health endpoint
export default function () {
  const res = http.get(`${BASE_URL}/actuator/health`);

  check(res, {
    'health check status is 200': (r) => r.status === 200,
  }) || errorRate.add(1);

  sleep(0.05); // Giảm sleep để tăng RPS
}

// Export report sau khi test
export function handleSummary(data) {
  return {
    // HTML report
    "load-tests/spike-api-gateway-report.html": htmlReport(data),

    // JSON report
    "load-tests/spike-api-gateway-report.json": JSON.stringify(data, null, 2),

    // Text summary
    "load-tests/spike-api-gateway-summary.txt": textSummary(data, { indent: " ", enableColors: false }),

    // Console output
    stdout: textSummary(data, { indent: " ", enableColors: true }),
  };
}
