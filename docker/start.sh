#!/bin/sh
set -eu

DB_NAME="${DB_NAME:-grading_system}"
DB_USER="${DB_USER:-grading_user}"
DB_PASSWORD="${DB_PASSWORD:-grading_password}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
MYSQL_SOCKET="${MYSQL_SOCKET:-/var/run/mysqld/mysqld.sock}"
MYSQL_DATA_DIR="${MYSQL_DATA_DIR:-/var/lib/mysql}"

escape_sql() {
  printf "%s" "$1" | sed "s/'/''/g"
}

wait_for_mysql() {
  attempts=0
  until mariadb-admin ping --socket="$MYSQL_SOCKET" --silent >/dev/null 2>&1; do
    attempts=$((attempts + 1))
    if [ "$attempts" -ge 60 ]; then
      echo "MySQL did not become ready in time" >&2
      exit 1
    fi
    sleep 2
  done
}

shutdown_mysql() {
  if [ -S "$MYSQL_SOCKET" ]; then
    mariadb-admin --socket="$MYSQL_SOCKET" shutdown >/dev/null 2>&1 || true
  fi
}

trap 'shutdown_mysql' EXIT INT TERM

mkdir -p /var/run/mysqld "$MYSQL_DATA_DIR" /app/uploads /app/assignment-files
chown -R mysql:mysql /var/run/mysqld "$MYSQL_DATA_DIR"

if [ ! -d "$MYSQL_DATA_DIR/mysql" ]; then
  mariadb-install-db --user=mysql --datadir="$MYSQL_DATA_DIR" >/dev/null
fi

mariadbd \
  --user=mysql \
  --datadir="$MYSQL_DATA_DIR" \
  --socket="$MYSQL_SOCKET" \
  --bind-address="$DB_HOST" \
  --port="$DB_PORT" \
  --skip-networking=0 \
  --pid-file=/var/run/mysqld/mysqld.pid &

wait_for_mysql

DB_NAME_ESCAPED="$(escape_sql "$DB_NAME")"
DB_USER_ESCAPED="$(escape_sql "$DB_USER")"
DB_PASSWORD_ESCAPED="$(escape_sql "$DB_PASSWORD")"

mariadb --socket="$MYSQL_SOCKET" -uroot <<SQL
CREATE DATABASE IF NOT EXISTS \`$DB_NAME_ESCAPED\`;
CREATE USER IF NOT EXISTS '$DB_USER_ESCAPED'@'localhost' IDENTIFIED BY '$DB_PASSWORD_ESCAPED';
CREATE USER IF NOT EXISTS '$DB_USER_ESCAPED'@'127.0.0.1' IDENTIFIED BY '$DB_PASSWORD_ESCAPED';
GRANT ALL PRIVILEGES ON \`$DB_NAME_ESCAPED\`.* TO '$DB_USER_ESCAPED'@'localhost';
GRANT ALL PRIVILEGES ON \`$DB_NAME_ESCAPED\`.* TO '$DB_USER_ESCAPED'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL

export SPRING_DATASOURCE_URL="jdbc:mysql://$DB_HOST:$DB_PORT/$DB_NAME?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export SPRING_DATASOURCE_USERNAME="$DB_USER"
export SPRING_DATASOURCE_PASSWORD="$DB_PASSWORD"

exec java -jar /app/app.jar
