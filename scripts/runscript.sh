#!/bin/bash
USERNAME="admin"
PASSWORD="admin"
URL="http://localhost:8080/alfresco/service/open_t/groovyrunner"

while getopts "h?u:p:l:" opt; do
    case "$opt" in
        h|\?)
            echo "Usage: runscript.sh [-u username] [-p password] [-l url] scriptfile"
            exit 0
            ;;
        u)
            USERNAME=$OPTARG
            ;;
        p)
            PASSWORD=$OPTARG
            ;;
        l)
            URL=$OPTARG
            ;;
    esac
done

shift $((OPTIND-1))
[ "$1" = "--" ] && shift

curl -u $USERNAME:$PASSWORD -H "Content-Type:text/plain; charset=UTF-8"  -X POST "$URL" --data-binary @$@
