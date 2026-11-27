#!/bin/sh
set -e

SEED_DB="/app/seed/mogumogu.mv.db"
TARGET_DB="/app/data/mogumogu.mv.db"
SEED_MODE="${MOGUMOGU_SEED_DB:-if-empty}"

mkdir -p /app/data

should_seed=false
seed_size=0
target_size=0

if [ -f "$SEED_DB" ]; then
  seed_size=$(wc -c < "$SEED_DB")
fi

if [ -f "$TARGET_DB" ]; then
  target_size=$(wc -c < "$TARGET_DB")
fi

case "$SEED_MODE" in
  force|always)
    should_seed=true
    ;;
  if-empty)
    if [ ! -f "$TARGET_DB" ] || [ "$target_size" -lt "$seed_size" ]; then
      should_seed=true
    fi
    ;;
  if-missing|never)
    if [ "$SEED_MODE" = "if-missing" ] && [ ! -f "$TARGET_DB" ]; then
      should_seed=true
    fi
    ;;
  *)
    echo "Unknown MOGUMOGU_SEED_DB=$SEED_MODE; supported: if-empty, if-missing, force, never"
    exit 1
    ;;
esac

if [ "$should_seed" = true ] && [ -f "$SEED_DB" ]; then
  echo "Seeding H2 database from $SEED_DB to $TARGET_DB (mode=$SEED_MODE, seed=${seed_size}B, target=${target_size}B)"
  cp -f "$SEED_DB" "$TARGET_DB"
elif [ ! -f "$TARGET_DB" ]; then
  echo "No seed database at $SEED_DB; H2 will start with an empty file database."
fi

exec java -jar /app/app.jar "$@"
