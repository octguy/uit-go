import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Counter } from 'k6/metrics';
import { htmlReport } from "https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js";
import { textSummary } from "https://jslib.k6.io/k6-summary/0.0.1/index.js";

// Custom metrics
const errorRate = new Rate('errors');
const driverOnlineRate = new Rate('driver_online');
const nearbySearchRate = new Rate('nearby_search');

// Stress Test Configuration for Driver Service
export const options = {
  stages: [
    // Ramp up: 0 → 50 VUs trong 1 phút
    { duration: '1m', target: 50 },

    // Ramp up: 50 → 100 VUs trong 1 phút
    { duration: '1m', target: 100 },

    // Ramp up: 100 → 200 VUs trong 1 phút
    { duration: '1m', target: 200 },

    // Spike: 200 → 400 VUs trong 30s
    { duration: '30s', target: 400 },

    // Hold: Giữ 400 VUs trong 2 phút
    { duration: '2m', target: 400 },

    // Extreme spike: 400 → 600 VUs trong 30s
    { duration: '30s', target: 600 },

    // Hold: Giữ 600 VUs trong 1 phút
    { duration: '1m', target: 600 },

    // Ramp down: Giảm xuống 0 trong 1 phút
    { duration: '1m', target: 0 },
  ],

  thresholds: {
    // 95% requests phải < 1s
    'http_req_duration{expected_response:true}': ['p(95)<1000'],

    // 99% requests phải < 3s
    'http_req_duration{expected_response:true}': ['p(99)<3000'],

    // Error rate phải < 10%
    'errors': ['rate<0.10'],

    // Failed requests < 10%
    'http_req_failed': ['rate<0.10'],
  },
};

const BASE_URL = 'http://localhost:8083';

// Helper function: Random float trong khoảng [min, max]
function randomFloat(min, max) {
  return Math.random() * (max - min) + min;
}

// Helper function: Random coordinates trong khu vực TP.HCM và Bangkok
function getRandomCoordinates() {
  // 50% TP.HCM, 50% Bangkok
  if (Math.random() < 0.5) {
    // TP.HCM area
    return {
      latitude: randomFloat(10.7, 10.9),
      longitude: randomFloat(106.6, 106.8),
    };
  } else {
    // Bangkok area
    return {
      latitude: randomFloat(13.6, 13.9),
      longitude: randomFloat(100.4, 100.7),
    };
  }
}

// Main test function - Simulates real driver behavior
export default function () {
  const coords = getRandomCoordinates();

  // Scenario 1: Set driver online (WRITE to Redis)
  const onlineRes = http.post(`${BASE_URL}/api/drivers/online`);

  const onlineSuccess = check(onlineRes, {
    'set online status is 200': (r) => r.status === 200,
  });

  if (onlineSuccess) {
    driverOnlineRate.add(1);
  } else {
    errorRate.add(1);
  }

  sleep(0.2);

  // Scenario 2: Find nearby drivers (READ from Redis - GEORADIUS + status checks)
  const nearbyRes = http.get(
    `${BASE_URL}/api/internal/drivers/nearby?lat=${coords.latitude}&lng=${coords.longitude}&radiusKm=5&limit=10`
  );

  const nearbySuccess = check(nearbyRes, {
    'nearby search status is 200': (r) => r.status === 200,
    'nearby search returns array': (r) => {
      try {
        const body = JSON.parse(r.body);
        return Array.isArray(body);
      } catch (e) {
        return false;
      }
    },
  });

  if (nearbySuccess) {
    nearbySearchRate.add(1);
  } else {
    errorRate.add(1);
  }

  sleep(0.3);

  // Scenario 3: Get pending trips (READ from Redis - KEYS + GET operations)
  const driverId = 'd32d977f-40cd-45b8-b730-75635b02e72f'; // Test driver ID
  const pendingRes = http.get(`${BASE_URL}/api/drivers/trips/pending?driverId=${driverId}`);

  check(pendingRes, {
    'pending trips status is 200': (r) => r.status === 200,
  }) || errorRate.add(1);

  // Log progress mỗi 100 iterations
  if (__ITER % 100 === 0) {
    console.log(`[VU ${__VU}] Iteration ${__ITER}: Location (${coords.latitude.toFixed(4)}, ${coords.longitude.toFixed(4)})`);
  }

  sleep(0.5); // Think time giữa các requests
}

// Setup function - Reset Redis counters trước khi test
export function setup() {
  console.log('Resetting Redis operation counters...');

  try {
    const res = http.post(`${BASE_URL}/api/driver-service/metrics/redis-ops/reset`);
    if (res.status === 200) {
      console.log('✅ Redis counters reset successfully');
    } else {
      console.log(`⚠️  Warning: Could not reset counters (status ${res.status})`);
    }
  } catch (e) {
    console.log(`⚠️  Warning: Could not reset counters - ${e}`);
  }
}

// Teardown function - Lấy Redis statistics sau khi test
export function teardown(data) {
  console.log('\n========================================');
  console.log('Fetching Redis Operation Statistics...');
  console.log('========================================\n');

  try {
    const res = http.get(`${BASE_URL}/api/driver-service/metrics/redis-ops`);

    if (res.status === 200) {
      const stats = JSON.parse(res.body);

      console.log('📊 Redis Operation Statistics:');
      console.log(`   Total Reads:       ${stats.totalReads}`);
      console.log(`   Total Writes:      ${stats.totalWrites}`);
      console.log(`   Read/Write Ratio:  ${stats.readWriteRatio}`);
      console.log(`   Recommendation:    ${stats.recommendation}`);
      console.log('');

      // Print to driver-service logs
      http.get(`${BASE_URL}/api/driver-service/metrics/redis-ops/print`);
    } else {
      console.log(`⚠️  Could not fetch statistics (status ${res.status})`);
    }
  } catch (e) {
    console.log(`⚠️  Error fetching statistics: ${e}`);
  }
}

// Export report sau khi test
export function handleSummary(data) {
  return {
    // HTML report
    "load-tests/driver-service/driver-stress-report.html": htmlReport(data),

    // JSON report
    "load-tests/driver-service/driver-stress-report.json": JSON.stringify(data, null, 2),

    // Text summary
    "load-tests/driver-service/driver-stress-summary.txt": textSummary(data, { indent: " ", enableColors: false }),

    // Console output
    stdout: textSummary(data, { indent: " ", enableColors: true }),
  };
}
