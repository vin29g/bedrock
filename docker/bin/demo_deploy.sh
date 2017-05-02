#!/bin/bash
set -ex

BIN_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
source $BIN_DIR/set_git_env_vars.sh

DEIS_BIN="${DEIS_BIN:-deis}"
DOCKER_ORIG_IMAGE_TAG="mozorg/bedrock_demo:${GIT_COMMIT}"

if [[ -n "$PRIVATE_REGISTRY" ]]; then
  # used for pulling from deis
  DOCKER_IMAGE_TAG="${DEIS_APP_NAME}:${GIT_COMMIT}"
  # used for pushing to registry
  PRIVATE_IMAGE_TAG="${PRIVATE_REGISTRY}/${DOCKER_IMAGE_TAG}"

  docker tag "$DOCKER_ORIG_IMAGE_TAG" "$PRIVATE_IMAGE_TAG"
  docker push "$PRIVATE_IMAGE_TAG"
else
  DOCKER_IMAGE_TAG="mozorg/bedrock_demo:${DEIS_APP_NAME}-${GIT_COMMIT_SHORT}"
  docker tag "$DOCKER_ORIG_IMAGE_TAG" "$DOCKER_IMAGE_TAG"
  docker push "$DOCKER_IMAGE_TAG"
fi

echo "Creating the demo app $DEIS_APP_NAME"
$DEIS_BIN apps:create "$DEIS_APP_NAME" --no-remote || true

echo "Configuring the new demo app"
$DEIS_BIN config:set -a "$DEIS_APP_NAME" $(< docker/envfiles/demo.env) || true

# Sentry DSN is potentially sensitive. Turn off command echo.
set +x
if [[ -n "$SENTRY_DEMO_DSN" ]]; then
  $DEIS_BIN config:set -a "$DEIS_APP_NAME" "SENTRY_DSN=$SENTRY_DEMO_DSN" || true
fi
set -x

echo "Pulling $DOCKER_IMAGE_TAG into Deis app $DEIS_APP_NAME"
$DEIS_BIN pull "$DOCKER_IMAGE_TAG" -a "$DEIS_APP_NAME"
