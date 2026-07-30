import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
    stages: [
        { duration: "30s", target: 20},
        { duration: "1m", target: 50},
        { duration: "2m", target: 100},
        { duration: "30s", target: 0},
    ],

    thresholds: {
        http_req_failed: ["rate<0.01"],
        http_req_duration: ["avg<6500", "p(95)<19500"],
    },
};

const BASE_URL = __ENV.BASE_URL || "https://api.hackathonplatform.co.za";

export default function() {
    let login = http.post(
        `${BASE_URL}/api/auth/login`,
        JSON.stringify({
            email: __ENV.EMAIL,
            password: __ENV.PASSWORD,
        }),
        {
            headers: {
                "Content-Type": "application/json",
            },
        }
    );

    check(login, {
        "login success": (r) => r.status === 200,
    });

    const token = login.json("token");

    const params = {
        headers: {
            Authorization: `Bearer ${token}`,
        },
    };

    let hackathons = http.get(
        `${BASE_URL}/api/hackathon`,
        params,
    );

    check(hackathons, {
        "hackathons loaded": (r) => r.status === 200,
    });

    sleep(1);
}
