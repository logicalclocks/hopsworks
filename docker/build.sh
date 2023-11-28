#!/bin/bash

set -e

while [ $# -gt 0 ]; do
  case "$1" in
    --version*|-v*)
      if [[ "$1" != *=* ]]; then shift; fi # Value is next arg if no `=`
      VERSION="${1#*=}"
      ;;
    --payara_version*|-pv*)
      if [[ "$1" != *=* ]]; then shift; fi
      PAYARA_VERSION="${1#*=}"
      ;;
    --user*|-u*)
      if [[ "$1" != *=* ]]; then shift; fi
      USERNAME="${1#*=}"
      ;;
    --password*|-p*)
      if [[ "$1" != *=* ]]; then shift; fi
      PASSWORD="${1#*=}"
      ;;
    --no-cache*|-nc*)
      if [[ "$1" != *=* ]]; then shift; fi
      if [ "${1#*=}" == true ]; then
        NO_CACHE="--no-cache"
      fi
      ;;
    --help|-h)
      echo "$0 -v <hopsworks_version> -pv <payara_version> -u <nexus user> -p <nexus password>"
      exit 0
      ;;
    *)
      >&2 printf "Error: Invalid argument\n"
      exit 1
      ;;
  esac
  shift
done

VERSION=${VERSION:-3.7.0-SNAPSHOT}
PAYARA_VERSION=${PAYARA_VERSION:-5.2022.5}
NO_SNAPSHOT_VERSION=${VERSION%"-SNAPSHOT"}

echo "building all images hopsworks:${VERSION} migration:${VERSION} payara:${PAYARA_VERSION} payara-node:${PAYARA_VERSION} hopsworks-deploy:${PAYARA_VERSION} ${NO_CACHE}"

cd payara-server && sudo docker build ${NO_CACHE} -f Dockerfile -t payara:"${PAYARA_VERSION}" .
cd ../payara-node && sudo docker build ${NO_CACHE} -f Dockerfile -t payara-node:"${PAYARA_VERSION}" .
cd ../deploy && sudo docker build ${NO_CACHE} -f Dockerfile -t hopsworks-deploy:"${PAYARA_VERSION}" .

cd ../migration && sudo docker build ${NO_CACHE} --build-arg version="${NO_SNAPSHOT_VERSION}" -f Dockerfile -t migration:"${VERSION}" .
cd .. && sudo docker build ${NO_CACHE} --build-arg version="${VERSION}" --build-arg user="$USERNAME" --build-arg password="$PASSWORD" --build-arg download_url=https://nexus.hops.works/repository/hopsworks -f Dockerfile -t hopsworks:"${VERSION}" .