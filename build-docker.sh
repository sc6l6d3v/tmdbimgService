#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: $0 DEV|PROD [PORT]" >&2
  exit 1
}

# --- args / env --------------------------------------------------------------
ENV_IN="${1:-}"; [[ -z "${ENV_IN}" ]] && usage
ENV_UPPER="$(echo "${ENV_IN}" | tr '[:lower:]' '[:upper:]')"
case "${ENV_UPPER}" in DEV|PROD) ;; *) echo "ENV must be DEV or PROD"; exit 2;; esac

PORT="${2:-${PORT:-8080}}"

# load .env if present (ignores comments/blank lines)
if [[ -f .env ]]; then
  export $(grep -vE '^\s*(#|$)' .env | xargs) || true
fi

: "${HUBUSER:?HUBUSER must be set (e.g. export HUBUSER=yourdockerhubuser)}"
: "${REDISKEY:?REDISKEY must be set}"
: "${REDISHOST:?REDISHOST must be set}"
: "${TMDBKEY:?TMDBKEY must be set}"
: "${BINDHOST:?BINDHOST must be set}"
: "${CLIENTPOOL:?CLIENTPOOL must be set}"
: "${SERVERPOOL:?SERVERPOOL must be set}"

IMAGE="${HUBUSER}/tmdbimg"
TS="$(date +%Y%m%d%H%M)"
REST_TAG="rest-${ENV_UPPER}"

echo "==> Building ${IMAGE}:${TS} and ${IMAGE}:latest (PORT=${PORT})"
PORT="${PORT}" docker build \
  --build-arg rediskey="${REDISKEY}" \
  --build-arg redishost="${REDISHOST}" \
  --build-arg tmdbkey="${TMDBKEY}" \
  --build-arg port="${PORT}" \
  --build-arg bindhost="${BINDHOST}" \
  --build-arg clientpool="${CLIENTPOOL}" \
  --build-arg serverpool="${SERVERPOOL}" \
  --build-arg OPT_PKGS="telnet bash" \
  -t "${IMAGE}:${TS}" \
  -t "${IMAGE}:latest" \
  .

echo "==> Tagging ${IMAGE}:${REST_TAG} from ${IMAGE}:${TS}"
docker tag "${IMAGE}:${TS}" "${IMAGE}:${REST_TAG}"

echo "==> Pushing ${IMAGE}:${TS}"
docker push "${IMAGE}:${TS}"

echo "==> Pushing ${IMAGE}:${REST_TAG}"
docker push "${IMAGE}:${REST_TAG}"

# Optional: push :latest if you want it updated too
if [[ "${PUSH_LATEST:-0}" == "1" ]]; then
  echo "==> Pushing ${IMAGE}:latest"
  docker push "${IMAGE}:latest"
fi

echo "Done."
echo "  Built   : ${IMAGE}:${TS}"
echo "  Tagged  : ${IMAGE}:${REST_TAG}"
echo "  Pushed  : ${IMAGE}:${TS}, ${IMAGE}:${REST_TAG}"
[[ "${PUSH_LATEST:-0}" == "1" ]] && echo "  Pushed  : ${IMAGE}:latest"
