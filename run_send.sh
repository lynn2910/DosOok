#!/bin/bash

if [ ! -e ./DosSend.java ]
then
  echo "Cannot find ./DosSend.java"
  exit 1
fi

javac DosSend.java && java DosSend