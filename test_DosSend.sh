#!/bin/bash

# Define colors
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'  # No Color

if [ ! -e ./loremipsum ]; then
  echo -e "${RED}Erreur:${NC} Le fichier 'loremipsum' n'existe pas"
  exit 1
fi

javac DosSend.java > /dev/null 2>&1

if [ $? -ne 0 ]; then
  echo -e "${RED}Erreur:${NC} La compilation du code Java a échoué"
  exit 1
fi

while IFS= read -r -n 1 char; do
  line="$line$char"

  if [ "$char" == $'\n' ]; then
    # Remove the trailing newline character
    line=$(echo "$line" | tr -d '\n')

    echo -e "Test de la ligne '${GREEN}$line${NC}'"
    java DosSend "$line"
    java_exit_status=$?

    if [ $java_exit_status -ne 0 ]; then
      echo -e "${RED}Erreur:${NC} Le programme Java a échoué pour la ligne '${GREEN}$line${NC}'"
      exit 1
    fi

    line=""
  fi
done < ./loremipsum


echo -e "${GREEN}Tous les tests sont réussis.${NC}"
