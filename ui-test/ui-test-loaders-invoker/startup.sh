set -eo pipefail

LOADERS_RESPONSE_JSON=$(curl --fail -X POST http://"${RDM_WEB_ENDPOINT}"/actuator/loaders)

if jq -e 'if .fails | length != 0 then true else false end' <<< "$LOADERS_RESPONSE_JSON" > /dev/null; then
  echo ".fails not empty: $LOADERS_RESPONSE_JSON"
  exit 1
fi

if jq -e 'if .aborted | length != 0 then true else false end' <<< "$LOADERS_RESPONSE_JSON" > /dev/null; then
  echo ".aborted not empty: $LOADERS_RESPONSE_JSON"
  exit 1
fi