import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const HOST = __ENV.HOST || 'http://host.docker.internal';
const USERS = `${HOST}:8080`;
const TICKETS = `${HOST}:8081`;

const USER_COUNT = Number(__ENV.USER_COUNT || 50); // Number of users to create for the test
const VUS = Number(__ENV.VUS || 200); // Number of virtual users to simulate
const ITERATIONS = Number(__ENV.ITERATIONS || 2000); // Number of iterations to run in the test
const HOT_SEAT = (__ENV.HOT_SEAT || 'false') === 'true'; // If true, all users will try to reserve the same seat, creating a "hot seat" scenario.

const seatIds = JSON.parse(open('./seats.json'));

// Hot seat: everyone fights for one seat, so exactly one winner is possible.
const maxWinners = HOT_SEAT ? 1 : seatIds.length;
const minWinners = HOT_SEAT ? 1 : seatIds.length - 1;

const reserved = new Counter('seats_reserved');
const conflicts = new Counter('seats_conflict');
const unexpected = new Counter('unexpected_responses');

export const options = {
    scenarios: {
        rush: {
            executor: 'shared-iterations',
            vus: VUS,
            iterations: ITERATIONS,
            maxDuration: '5m',
        },
    },
    thresholds: {
        seats_reserved: [`count<=${maxWinners}`, `count>=${minWinners}`],
        unexpected_responses: ['count==0'],
    },
};

// Create USER_COUNT real accounts and collect a token for each.
export function setup() {
    const headers = { 'Content-Type': 'application/json' };
    const password = 'loadtest123';
    const tokens = [];

    for (let i = 0; i < USER_COUNT; i++) {
        const email = `load${i}@matchday.test`;

        // Ignored on purpose: the account may already exist from an earlier run.
        http.post(
            `${USERS}/api/v1/auth/register`,
            JSON.stringify({ email, password, displayName: `Load ${i}` }),
            { headers },
        );

        const response = http.post(
            `${USERS}/api/v1/auth/login`,
            JSON.stringify({ email, password }),
            { headers },
        );

        if (response.status !== 200) {
            throw new Error(`login failed for ${email}: ${response.status} ${response.body}`);
        }

        tokens.push(response.json('accessToken'));
    }

    return { tokens };
}

export default function reserve(data) {
    const token = data.tokens[__VU % data.tokens.length];
    const seatId = HOT_SEAT ? seatIds[0] : seatIds[Math.floor(Math.random() * seatIds.length)];

    const response = http.post(`${TICKETS}/api/v1/seats/${seatId}/reserve`, null, {
        headers: { Authorization: `Bearer ${token}` },
    });

    if (response.status === 200) {
        reserved.add(1);
    } else if (response.status === 409) {
        conflicts.add(1);
    } else {
        unexpected.add(1);
    }

    check(response, { 'no server error': (r) => r.status < 500 });
}
