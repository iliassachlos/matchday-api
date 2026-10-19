import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

// The whole purchase under load: reserve a seat, then pay for it through order-service.
// k6 counts what the clients saw; run.sh then checks the databases themselves, which is
// the real proof that no seat was sold twice.

const HOST = __ENV.HOST || 'http://host.docker.internal';
const USERS = `${HOST}:8080`;
const TICKETS = `${HOST}:8081`;
const ORDERS = `${HOST}:8082`;

const USER_COUNT = Number(__ENV.USER_COUNT || 200);
const VUS = Number(__ENV.VUS || 200);
const ITERATIONS = Number(__ENV.ITERATIONS || 3000);

const seatIds = JSON.parse(open('./seats.json'));

const held = new Counter('seats_held');
const holdConflicts = new Counter('hold_conflicts');
const paid = new Counter('orders_paid');
const notPaid = new Counter('orders_not_paid');
const unexpected = new Counter('unexpected_responses');

export const options = {
    scenarios: {
        rush: {
            executor: 'shared-iterations',
            vus: VUS,
            iterations: ITERATIONS,
            maxDuration: '10m',
        },
    },
    thresholds: {
        // More paid orders than seats would be an oversell
        orders_paid: [`count<=${seatIds.length}`],
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

export default function purchase(data) {
    const token = data.tokens[__VU % data.tokens.length];
    const auth = { Authorization: `Bearer ${token}` };
    const seatId = seatIds[Math.floor(Math.random() * seatIds.length)];

    const hold = http.post(`${TICKETS}/api/v1/seats/${seatId}/reserve`, null, { headers: auth });

    if (hold.status === 409) {
        holdConflicts.add(1);
        return;
    }
    if (hold.status !== 200) {
        unexpected.add(1);
        return;
    }
    held.add(1);

    const order = http.post(
        `${ORDERS}/api/v1/orders`,
        JSON.stringify({ seatId, paymentMethod: 'pm_card_visa' }),
        { headers: { ...auth, 'Content-Type': 'application/json' } },
    );

    // 201 means an order was recorded; its status says whether it was paid or refunded
    if (order.status === 201 && order.json('status') === 'PAID') {
        paid.add(1);
    } else if (order.status === 201 || order.status === 409) {
        notPaid.add(1);
    } else {
        unexpected.add(1);
    }

    check(order, { 'no server error': (r) => r.status < 500 });
}
