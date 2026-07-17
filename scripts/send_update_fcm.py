#!/usr/bin/env python3
"""Send a data-only School update trigger through FCM HTTP v1.

The message contains no trusted update metadata. Android clients use it only to fetch and verify the
signed update-manifest.json from GitHub Release.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import urllib.error
import urllib.request

from google.auth.transport.requests import Request
from google.oauth2 import service_account

FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--service-account", required=True, type=Path)
    parser.add_argument("--project-id", required=True)
    parser.add_argument("--topic", required=True)
    parser.add_argument("--version-code", required=True)
    parser.add_argument("--version-name", required=True)
    parser.add_argument("--manifest-url", required=True)
    parser.add_argument("--channel", default="development")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    credentials = service_account.Credentials.from_service_account_file(
        args.service_account,
        scopes=[FCM_SCOPE],
    )
    credentials.refresh(Request())
    if not credentials.token:
        raise RuntimeError("FCM OAuth access token is empty")

    payload = {
        "message": {
            "topic": args.topic,
            "data": {
                "type": "school_update",
                "versionCode": str(args.version_code),
                "versionName": args.version_name,
                "channel": args.channel,
                "manifestUrl": args.manifest_url,
            },
            "android": {
                "priority": "HIGH",
                "ttl": "3600s",
            },
        },
    }
    endpoint = f"https://fcm.googleapis.com/v1/projects/{args.project_id}/messages:send"
    request = urllib.request.Request(
        endpoint,
        data=json.dumps(payload, separators=(",", ":")).encode("utf-8"),
        method="POST",
        headers={
            "Authorization": f"Bearer {credentials.token}",
            "Content-Type": "application/json; charset=UTF-8",
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            response_payload = response.read().decode("utf-8")
    except urllib.error.HTTPError as error:
        body = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"FCM returned HTTP {error.code}: {body}") from error

    result = json.loads(response_payload)
    message_name = result.get("name")
    if not message_name:
        raise RuntimeError(f"FCM response does not contain a message name: {response_payload}")
    print(f"FCM update trigger sent: {message_name}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
