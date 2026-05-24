#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import json
import urllib.request
import urllib.error
import time
import subprocess

# Targeting the API Gateway on 8080.
BASE_URL = "http://localhost:8080"
SUFFIX = int(time.time())
SRC_USER = f"alice-{SUFFIX}"
DST_USER = f"bob-{SUFFIX}"
PASS = "DemoPass123!"

def post_json(path, data, token=None):
    url = f"{BASE_URL}{path}"
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"

    req = urllib.request.Request(
        url,
        data=json.dumps(data).encode("utf-8"),
        headers=headers,
        method="POST"
    )
    try:
        with urllib.request.urlopen(req) as response:
            return json.loads(response.read().decode("utf-8")), response.getcode()
    except urllib.error.HTTPError as e:
        # Return error body as string and the status code
        return e.read().decode("utf-8"), e.code

def get_json(path, token):
    url = f"{BASE_URL}{path}"
    req = urllib.request.Request(url, headers={"Authorization": f"Bearer {token}"}, method="GET")
    with urllib.request.urlopen(req) as response:
        return json.loads(response.read().decode("utf-8"))

def run_demo():
    print(f"==> 10-transfer demo (suffix={SUFFIX}, base={BASE_URL})")

    # 1. Register Users
    print("1/6 Registering users...")
    user_payloads = [
        {"username": SRC_USER, "email": f"{SRC_USER}@demo.local", "password": PASS, "firstName": "Alice", "lastName": "Demo"},
        {"username": DST_USER, "email": f"{DST_USER}@demo.local", "password": PASS, "firstName": "Bob", "lastName": "Demo"}
    ]
    for p in user_payloads:
        res, code = post_json("/api/v1/auth/register", p)
        if code not in [200, 201]:
            print(f"❌ Registration failed ({code}): {res}")
            return

    # 2. Login
    print("2/6 Logging in...")
    src_res, c1 = post_json("/api/v1/auth/login", {"usernameOrEmail": SRC_USER, "password": PASS})
    dst_res, c2 = post_json("/api/v1/auth/login", {"usernameOrEmail": DST_USER, "password": PASS})
    if c1 != 200 or c2 != 200:
        print(f"❌ Login failed: SRC={c1}, DST={c2}. Response: {src_res}")
        return
    SRC_TOKEN, DST_TOKEN = src_res["accessToken"], dst_res["accessToken"]

    # 3. Open Wallets
    print("3/6 Opening wallets...")
    src_w, code1 = post_json("/api/v1/accounts", {"currencyCode": "USD"}, SRC_TOKEN)
    dst_w, code2 = post_json("/api/v1/accounts", {"currencyCode": "USD"}, DST_TOKEN)

    if code1 != 201 or code2 != 201:
        print(f"❌ Wallet creation failed: SRC={code1}, DST={code2}. Resp: {src_w}")
        return

    SRC_UUID, DST_UUID = src_w["uuid"], dst_w["uuid"]
    print(f"   Wallets ready: {SRC_UUID}, {DST_UUID}")

    # 4. DB Seed
    print("4/6 Seeding balance...")
    cmd = f"docker exec fintrack-mysql-accounts mysql -uroot -prootpass fintrack_accounts -e \"UPDATE accounts SET balance=10000.0 WHERE uuid='{SRC_UUID}';\""
    subprocess.run(cmd, shell=True, stdout=subprocess.DEVNULL)

    # 5. Transfers
    print("5/6 Issuing 10 transfers...")
    for i in range(1, 11):
        _, code = post_json("/api/v1/transactions", {"fromAccountUuid": SRC_UUID, "toAccountUuid": DST_UUID, "amount": 50, "currencyCode": "USD", "type": "DOMESTIC_TRANSFER"}, SRC_TOKEN)
        if code != 201:
            print(f"   Transfer {i} failed with status {code}")
        else:
            print(f"   Transfer {i}/10 sent")

    # 6. Verify
    print("6/6 Verification...")
    time.sleep(5)
    src_bal = get_json(f"/api/v1/accounts/{SRC_UUID}/balance", SRC_TOKEN)["balance"]
    print(f"   Final Balance: {src_bal} (Expected: 9500.0)")

if __name__ == "__main__":
    run_demo()