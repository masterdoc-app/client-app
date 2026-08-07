#!/usr/bin/env python3
"""Upload signed AAB to RuStore and send for moderation.

Required env:
  RUSTORE_KEY_ID          — numeric key id from RuStore Console → API RuStore
  RUSTORE_PRIVATE_KEY     — base64 private key (single line)
  AAB_PATH                — path to .aab file

Optional env:
  RUSTORE_PACKAGE_NAME    — default pro.masterdoc.client
  RUSTORE_WHATS_NEW       — release notes (default: git / workflow message)
  RUSTORE_PUBLISH_TYPE    — INSTANTLY | MANUAL | DELAYED (default INSTANTLY)
  RUSTORE_API_BASE        — default https://public-api.rustore.ru
"""

from __future__ import annotations

import base64
import datetime
import json
import os
import sys
import urllib.error
import urllib.request
from typing import Any

from Crypto.Hash import SHA512
from Crypto.PublicKey import RSA
from Crypto.Signature import pkcs1_15

API_BASE = os.environ.get("RUSTORE_API_BASE", "https://public-api.rustore.ru").rstrip("/")
PACKAGE = os.environ.get("RUSTORE_PACKAGE_NAME", "pro.masterdoc.client")
AAB_PATH = os.environ.get("AAB_PATH", "")
PUBLISH_TYPE = os.environ.get("RUSTORE_PUBLISH_TYPE", "INSTANTLY")
WHATS_NEW = os.environ.get(
    "RUSTORE_WHATS_NEW",
    "Первый релиз Fixaverse для инженеров: заявки, карта, оборудование и ППР.",
)


def env(name: str) -> str:
    value = os.environ.get(name, "").strip()
    if not value:
        print(f"Missing required env: {name}", file=sys.stderr)
        sys.exit(1)
    return value


def api_json(method: str, path: str, token: str, body: dict | None = None) -> Any:
    url = f"{API_BASE}{path}"
    data = None if body is None else json.dumps(body).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=data,
        method=method,
        headers={
            "Public-Token": token,
            "Accept": "application/json",
            **({"Content-Type": "application/json"} if body is not None else {}),
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            raw = resp.read().decode("utf-8")
            return json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        detail = e.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"{method} {path} -> HTTP {e.code}: {detail}") from e


def content_type_for(file_path: str) -> str:
    lower = file_path.lower()
    if lower.endswith(".png"):
        return "image/png"
    if lower.endswith(".jpg") or lower.endswith(".jpeg"):
        return "image/jpeg"
    if lower.endswith(".aab"):
        return "application/octet-stream"
    return "application/octet-stream"


def api_multipart(path: str, token: str, file_path: str) -> Any:
    boundary = "----FixaverseRuStoreBoundary"
    filename = os.path.basename(file_path)
    with open(file_path, "rb") as f:
        file_bytes = f.read()
    ctype = content_type_for(file_path)

    body = (
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="file"; filename="{filename}"\r\n'
        f"Content-Type: {ctype}\r\n\r\n"
    ).encode("utf-8") + file_bytes + f"\r\n--{boundary}--\r\n".encode("utf-8")

    req = urllib.request.Request(
        f"{API_BASE}{path}",
        data=body,
        method="POST",
        headers={
            "Public-Token": token,
            "Content-Type": f"multipart/form-data; boundary={boundary}",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=600) as resp:
            raw = resp.read().decode("utf-8")
            return json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        detail = e.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"POST {path} -> HTTP {e.code}: {detail}") from e


def fetch_token(key_id: str, private_key_b64: str) -> str:
    private_key = RSA.import_key(base64.b64decode(private_key_b64))
    timestamp = datetime.datetime.now(datetime.timezone.utc).isoformat(timespec="milliseconds")
    message = (key_id + timestamp).encode("utf-8")
    signature = base64.b64encode(pkcs1_15.new(private_key).sign(SHA512.new(message))).decode("utf-8")

    payload = {"keyId": key_id, "timestamp": timestamp, "signature": signature}
    req = urllib.request.Request(
        f"{API_BASE}/public/auth/",
        data=json.dumps(payload).encode("utf-8"),
        method="POST",
        headers={"Content-Type": "application/json", "Accept": "application/json"},
    )
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            data = json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        detail = e.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"Auth failed HTTP {e.code}: {detail}") from e

    if data.get("code") != "OK":
        raise RuntimeError(f"Auth failed: {data}")

    jwe = data.get("body", {}).get("jwe")
    if not jwe:
        raise RuntimeError(f"Auth response missing jwe: {data}")
    return jwe


def find_draft_version_id(token: str) -> int | None:
    query = (
        f"/public/v1/application/{PACKAGE}/version"
        f"?versionStatuses=DRAFT&page=0&size=5"
    )
    data = api_json("GET", query, token)
    content = data.get("body", {}).get("content") or data.get("content") or []
    if isinstance(content, list) and content:
        version_id = content[0].get("versionId") or content[0].get("id")
        if version_id is not None:
            return int(version_id)
    return None


def load_listing_from_copy() -> dict[str, Any]:
    copy_path = os.path.join(os.path.dirname(__file__), "..", "store", "rustore", "copy.md")
    with open(copy_path, encoding="utf-8") as f:
        text = f.read()

    def block(section: str) -> str:
        import re

        pattern = rf"## {re.escape(section)}\s*\n\n```\n(.*?)\n```"
        match = re.search(pattern, text, re.DOTALL)
        return match.group(1).strip() if match else ""

    return {
        "appName": block("Название (≤50 символов, ASO)"),
        "shortDescription": block("Краткое описание"),
        "fullDescription": block("Полное описание"),
        "moderInfo": block("Комментарий модератору"),
        "appType": "MAIN",
        "categories": ["tools"],
        "ageLegal": "18+",
    }


def create_draft(token: str) -> int:
    listing = load_listing_from_copy()
    body = {
        **listing,
        "publishType": PUBLISH_TYPE,
        "whatsNew": WHATS_NEW[:5000],
    }
    data = api_json("POST", f"/public/v1/application/{PACKAGE}/version", token, body)
    if data.get("code") != "OK":
        raise RuntimeError(f"Create draft failed: {data}")
    version_id = data.get("body")
    if version_id is None:
        raise RuntimeError(f"Create draft missing version id: {data}")
    return int(version_id)


def resolve_version_id(token: str) -> int:
    existing = find_draft_version_id(token)
    if existing is not None:
        print(f"Reusing existing draft versionId={existing}")
        return existing
    version_id = create_draft(token)
    print(f"Created draft versionId={version_id}")
    return version_id


def upload_aab(token: str, version_id: int, aab_path: str) -> None:
    data = api_multipart(
        f"/public/v1/application/{PACKAGE}/version/{version_id}/aab",
        token,
        aab_path,
    )
    if data.get("code") != "OK":
        raise RuntimeError(f"AAB upload failed: {data}")
    print(f"AAB uploaded ({os.path.getsize(aab_path) // 1024} KB)")


def upload_icon(token: str, version_id: int, icon_path: str) -> None:
    data = api_multipart(
        f"/public/v1/application/{PACKAGE}/version/{version_id}/image/icon",
        token,
        icon_path,
    )
    if data.get("code") != "OK":
        raise RuntimeError(f"Icon upload failed: {data}")
    print(f"Icon uploaded: {os.path.basename(icon_path)}")


def upload_screenshots(token: str, version_id: int, shots_dir: str) -> None:
    if not os.path.isdir(shots_dir):
        print(f"No screenshots dir: {shots_dir}")
        return
    shots = sorted(
        p
        for p in os.listdir(shots_dir)
        if p.lower().endswith((".png", ".jpg", ".jpeg"))
    )
    if not shots:
        print("No screenshots to upload")
        return
    for ordinal, name in enumerate(shots[:10]):
        path = os.path.join(shots_dir, name)
        data = api_multipart(
            f"/public/v1/application/{PACKAGE}/version/{version_id}/image/screenshot/PORTRAIT/{ordinal}",
            token,
            path,
        )
        if data.get("code") != "OK":
            raise RuntimeError(f"Screenshot upload failed ({name}): {data}")
        print(f"Screenshot uploaded [{ordinal}]: {name}")


def send_for_moderation(token: str, version_id: int) -> None:
    data = api_json(
        "POST",
        f"/public/v1/application/{PACKAGE}/version/{version_id}/commit",
        token,
    )
    if data.get("code") != "OK":
        raise RuntimeError(f"Moderation commit failed: {data}")
    print("Sent to moderation")


def main() -> None:
    key_id = env("RUSTORE_KEY_ID")
    private_key = env("RUSTORE_PRIVATE_KEY")
    aab_path = AAB_PATH or env("AAB_PATH")
    store_dir = os.path.abspath(
        os.path.join(os.path.dirname(__file__), "..", "store", "rustore")
    )
    icon_path = os.environ.get("RUSTORE_ICON_PATH") or os.path.join(
        store_dir, "icon-512-opaque.png"
    )
    if not os.path.isfile(icon_path):
        icon_path = os.path.join(store_dir, "icon-512.png")
    shots_dir = os.environ.get("RUSTORE_SCREENSHOTS_DIR") or os.path.join(
        store_dir, "upload"
    )

    if not os.path.isfile(aab_path):
        print(f"AAB not found: {aab_path}", file=sys.stderr)
        sys.exit(1)
    if not aab_path.endswith(".aab"):
        print("Expected .aab file", file=sys.stderr)
        sys.exit(1)

    print(f"Package: {PACKAGE}")
    print(f"AAB: {aab_path}")

    token = fetch_token(key_id, private_key)
    print("RuStore auth OK")

    version_id = resolve_version_id(token)
    if os.path.isfile(icon_path):
        upload_icon(token, version_id, icon_path)
    else:
        print(f"WARNING: icon missing at {icon_path}", file=sys.stderr)
    upload_screenshots(token, version_id, shots_dir)
    upload_aab(token, version_id, aab_path)
    send_for_moderation(token, version_id)
    print(f"Done. versionId={version_id}")


if __name__ == "__main__":
    main()
