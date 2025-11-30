import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';
import { htmlReport } from "https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js";
import { textSummary } from "https://jslib.k6.io/k6-summary/0.0.1/index.js";

// Custom metrics
const errorRate = new Rate('errors');
const tripCreationRate = new Rate('trip_created');

// Stress Test Configuration for Trip Service
export const options = {
  stages: [
    // Ramp up: 0 → 500 VUs trong 30 giây
    { duration: '30s', target: 500 },

    // Hold: Giữ 500 VUs trong 1 phút
    { duration: '1m', target: 500 },

    // Ramp down: Giảm xuống 0 trong 30 giây
    { duration: '30s', target: 0 },
  ],

  thresholds: {
    // 95% requests phải < 2s (trip creation có thể chậm hơn)
    'http_req_duration{expected_response:true}': ['p(95)<2000'],

    // 99% requests phải < 5s
    'http_req_duration{expected_response:true}': ['p(99)<5000'],

    // Error rate phải < 15% (cho phép fail nhiều hơn vì có DB operations)
    'errors': ['rate<0.15'],

    // Failed requests < 15%
    'http_req_failed': ['rate<0.15'],
  },
};

const BASE_URL = 'http://localhost:8080';

// Helper function: Random float trong khoảng [min, max]
function randomFloat(min, max) {
  return Math.random() * (max - min) + min;
}

// Helper function: Random coordinates trong khu vực Đông Nam Á
function getRandomCoordinates() {
  return {
    pickupLongitude: randomFloat(90, 125),    // 90°E - 125°E
    pickupLatitude: randomFloat(0, 25),       // 0°N - 25°N
    destinationLongitude: randomFloat(90, 125),
    destinationLatitude: randomFloat(0, 25),
  };
}

// Main test function
export default function () {
  // Get random coordinates
  const coords = getRandomCoordinates();

  // Tạo trip request payload
  const payload = JSON.stringify({
    pickupLatitude: coords.pickupLatitude,
    pickupLongitude: coords.pickupLongitude,
    destinationLatitude: coords.destinationLatitude,
    destinationLongitude: coords.destinationLongitude,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkMzJkOTc3Zi00MGNkLTQ1YjgtYjczMC03NTYzNWIwMmU3MmYiLCJpYXQiOjE3NjQ1MTQ2NzMsImV4cCI6MTc2NDYwMTA3M30.KQzxoqErKA5GhAv3Xw5Q-3s044ciSxpmRJF9VIqS15s',
    },
  };

  // Gọi API create trip
  const res = http.post(`${BASE_URL}/api/trips/create`, payload, params);

  // Check response
  const success = check(res, {
    'trip creation status is 200 or 201': (r) => r.status === 200 || r.status === 201,
    'response has trip data': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.id !== undefined;
      } catch (e) {
        return false;
      }
    },
  });

  if (!success) {
    errorRate.add(1);
  } else {
    tripCreationRate.add(1);
  }

  // Log một số request để debug (mỗi 100 iterations)
  if (__ITER % 100 === 0) {
    console.log(`[VU ${__VU}] Iteration ${__ITER}: Status ${res.status}, Pickup: (${coords.pickupLatitude.toFixed(4)}, ${coords.pickupLongitude.toFixed(4)})`);
  }

  // Sleep 0.05s = 20 req/s per VU → 500 VUs × 20 = 10,000 RPS
  sleep(0.05);
}

// Export report sau khi test
export function handleSummary(data) {
  return {
    // HTML report
    "load-tests/stress-report.html": htmlReport(data),

    // JSON report
    "load-tests/stress-report.json": JSON.stringify(data, null, 2),

    // Text summary
    "load-tests/stress-summary.txt": textSummary(data, { indent: " ", enableColors: false }),

    // Console output
    stdout: textSummary(data, { indent: " ", enableColors: true }),
  };
}
