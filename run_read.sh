#!/bin/bash

if [ ! -e ./DosRead.java ]
then
  echo "Cannot find ./DosRead.java"
  exit 1
fi

if [ ! -e ./DosOok_message.wav ]
then
  echo "Cannot find DosOok_message.wav"
  exit 1
fi

javac DosRead.java && java DosRead ./DosOok_message.wav