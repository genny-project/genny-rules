#!/bin/bash
mvn clean install -DskipTests=true
mvn eclipse:eclipse
