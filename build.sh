#!/usr/bin/env bash

mvn -Pnative -DskipTests spring-javaformat:apply clean package &&  ./target/demo

