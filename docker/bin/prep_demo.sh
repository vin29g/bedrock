#!/bin/bash
set -ex

DEIS_BIN="${DEIS_BIN:-deis}"

echo "Creating the demo app $DEIS_APPLICATION"
$DEIS_BIN apps:create "$DEIS_APPLICATION" --no-remote || true

echo "Configuring the new demo app"

ENV_FILES=(
  "jenkins/branches/demo/default.env"
  "jenkins/regions/${DEIS_PROFILE}.env"
  "jenkins/branches/${BRANCH_NAME}.env"
)

# reads which ever of the above files exist in order and combines values
ENV_VALUES=( $(jenkins/bin/cat_envs.py "${ENV_FILES[@]}") )

if [[ -n "$SENTRY_DEMO_DSN" ]]; then
    ENV_VALUES+=( "SENTRY_DSN=$SENTRY_DEMO_DSN" )
fi

$DEIS_BIN config:set -a "$DEIS_APPLICATION" "${ENV_VALUES[@]}" || true
