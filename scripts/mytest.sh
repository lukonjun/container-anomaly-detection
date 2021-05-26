#!/bin/bash

# todo suppress output and onyl check error code

echo -n "Do you want to install or uninstall charts?"
read VAR

if [[ "$VAR" == "install" ]]
then
  echo "install charts"
elif [[ "$VAR" == "uninstall" ]]
then
  echo "uninstall charts"
else
  echo "No valid input"
fi