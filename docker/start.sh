#!/bin/sh
set -eu

mkdir -p /app/uploads /app/assignment-files

if [ -n "${DATABASE_URL:-}" ] && [ -z "${SPRING_DATASOURCE_URL:-}" ]; then
  DATABASE_NO_SCHEME="${DATABASE_URL#postgresql://}"
  DATABASE_HOST_AND_NAME="${DATABASE_NO_SCHEME#*@}"
  DATABASE_HOST_PORT="${DATABASE_HOST_AND_NAME%%/*}"
  DATABASE_NAME="${DATABASE_HOST_AND_NAME#*/}"

  export SPRING_DATASOURCE_URL="jdbc:postgresql://${DATABASE_HOST_PORT}/${DATABASE_NAME}"
fi

if [ -n "${RENDER_PGUSER:-}" ] && [ -z "${SPRING_DATASOURCE_USERNAME:-}" ]; then
  export SPRING_DATASOURCE_USERNAME="${RENDER_PGUSER}"
fi

if [ -n "${RENDER_PGPASSWORD:-}" ] && [ -z "${SPRING_DATASOURCE_PASSWORD:-}" ]; then
  export SPRING_DATASOURCE_PASSWORD="${RENDER_PGPASSWORD}"
fi

if [ -n "${DATABASE_URL:-}" ] && [ -z "${SPRING_DATASOURCE_DRIVER_CLASS_NAME:-}" ]; then
  export SPRING_DATASOURCE_DRIVER_CLASS_NAME="org.postgresql.Driver"
fi

if [ -n "${DATABASE_URL:-}" ] && [ -z "${SPRING_JPA_DATABASE_PLATFORM:-}" ]; then
  export SPRING_JPA_DATABASE_PLATFORM="org.hibernate.dialect.PostgreSQLDialect"
fi

exec java -jar /app/app.jar
