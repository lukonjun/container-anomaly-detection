#!/bin/sh

function mytest {
    "$@"
    local status=$?
    if (( status != 0 )); then
        echo "error with $1" >&2
    fi
    return $status
}
mytest "kubectl get pods"