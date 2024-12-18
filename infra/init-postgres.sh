#!/bin/bash
echo "Inside init-postgres.sh"
set -e

if [ -n "$POSTGRES_DATABASES" ]; then
  echo "Multiple database creation requested: $POSTGRES_DATABASES"
  for db in $(echo $POSTGRES_DATABASES | tr ',' ' '); do
    echo "  Creating database '$db' owned by user 'postgres'"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
        CREATE DATABASE $db;
        GRANT ALL PRIVILEGES ON DATABASE $db TO $POSTGRES_USER;
EOSQL
  done
  echo "Postgres Databases created"
fi