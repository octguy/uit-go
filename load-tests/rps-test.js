import http from 'k6/http';
import { check } from 'k6';

// Constant Request Rate Test - Tìm max RPS
export const options = {
  scenarios: {
    constant_request_rate: {
      executor: 'constant-arrival-rate',
      rate: 100, // Bắt đầu với 100 requests/giây
      timeUnit: '1s',
      duration: '1m',
      preAllocatedVUs: 50,
      maxVUs: 200,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'], // < 1% errors
    http_req_duration: ['p(95)<1000'], // 95% < 1s
  },
};

const BASE_URL = 'http://localhost:8080';

export default function () {
  const res = http.get(`${BASE_URL}/actuator/health`);
  check(res, {
    'status is 200': (r) => r.status === 200,
  });
}
