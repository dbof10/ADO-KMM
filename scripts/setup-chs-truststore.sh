#!/usr/bin/env bash
# Merge CHS VPN / INFOSEC root into a copy of the JDK cacerts store so Gradle
# can complete TLS to Maven Central / Google while on corporate SSL inspection.
#
# Writes: <repo>/gradle/ado-desktop-chs-truststore.jks (gitignored)
# Gradle reads it via gradle.properties (relative to project dir).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
CERT_PATH="${1:-${HOME}/Downloads/CHS_VPN_INFOSEC.cer}"
DEST="${REPO_ROOT}/gradle/ado-desktop-chs-truststore.jks"
ALIAS="chs-vpn-infosec"

if [[ ! -f "$CERT_PATH" ]]; then
  echo "Certificate not found: $CERT_PATH" >&2
  echo "Usage: $0 [/path/to/CHS_VPN_INFOSEC.cer]" >&2
  exit 1
fi

JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home 2>/dev/null || true)}"
if [[ -z "${JAVA_HOME:-}" ]]; then
  echo "JAVA_HOME is not set and /usr/libexec/java_home did not find a JDK." >&2
  exit 1
fi

SRC="${JAVA_HOME}/lib/security/cacerts"
if [[ ! -f "$SRC" ]]; then
  echo "JDK truststore not found: $SRC" >&2
  exit 1
fi

mkdir -p "$(dirname "$DEST")"
cp "$SRC" "$DEST"
chmod u+w "$DEST"

if keytool -list -keystore "$DEST" -storepass changeit -alias "$ALIAS" &>/dev/null; then
  keytool -delete -noprompt -alias "$ALIAS" -keystore "$DEST" -storepass changeit || true
fi

keytool -importcert -noprompt -trustcacerts \
  -alias "$ALIAS" \
  -file "$CERT_PATH" \
  -keystore "$DEST" \
  -storepass changeit

echo "Truststore updated: $DEST"
echo "Run from repo root: ./gradlew --stop && ./gradlew compileAllTargets"
