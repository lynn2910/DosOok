#!/bin/bash

if [ ! -e DosSend.java ]
then
  echo "Cannot find ./DosSend.java"
  exit 1
fi

if [ ! -e tests/helloWorld.txt ]
then
  echo "Cannot find tests/helloWorld.txt"
  exit 1
fi

javac DosSend.java && java DosSend < ./tests/helloWorld.txt