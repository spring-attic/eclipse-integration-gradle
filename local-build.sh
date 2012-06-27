#!/bin/bash

#This is the command that I run when testing the build locally
#mvn -Dmaven.test.skip=true -Dp2.qualifier=M2 -Pe37 clean install
#mvn -Dp2.qualifier=M2 -Pe37 clean install
mvn -Dp2.qualifier=M2 -Pe37 clean install
