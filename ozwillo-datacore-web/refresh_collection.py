#! /usr/bin/env python2
# -*- encoding: utf-8 -*-

# Script used to "refresh" some collections to add fulltext search into.

__author__ = "OpenWide"
__copyright__ = "Copyright 2015"
__license__ = "GPL"
__version__ = "0.2"

import os
import requests
import sys
import json
import urllib

DC_URL = "https://data.ozwillo-dev.eu/dc/"
ENTITY_BY_PAGE = 1  # Must not be greeter than 100

if len(sys.argv) < 3:
    TYPE = sys.argv[1]
    DC_PROJECT = sys.argv[2]
    BEARER = sys.argv[3]
else:
    print "Missing args, please provide type (1, ex: 'org:Organization_0') project (2, ex: 'org_1') and bearer (3) as argument of this script"
    print "Loading default (bearer will be surely wrong)"
    TYPE = "org:Organization_0"
    DC_PROJECT = "org_1"
    BEARER = "eyJpZCI6IjIzMzgxYmIxLTMwMWItNGU2YS04NzA1LTlkNjBjZmI1ZjM0Yy91bHdYLXNWS0VYN2hwbU0yX3dXdHZRIiwiaWF0IjoxNDUwMjYwMzU5NjAxLCJleHAiOjE0NTAyNjM5NTk2MDF9"

LOG = "./logs"


def refresh_collection():
    content_type = "application/json; charset=UTF-8"
    x_datacore_project = DC_PROJECT
    authorization = "Bearer %s" % BEARER

    headers_perso = {'Content-Type': content_type,
                     'X-Datacore-Project': x_datacore_project,
                     'Authorization': authorization}

    page = 1
    retry_counter = 0
    retry_limit = 3
    id_cursor = "%2B"  # for the first request arg @id is %2B = "+"
    data = {}

    while id_cursor != "":

        url_get = DC_URL + "type/" + TYPE + "?limit=" + str(ENTITY_BY_PAGE) + "&%40id=" + id_cursor
        print
        print "############################### Page : %s (retry %d) ############################################" % (page, retry_counter)
        print "Sending request (GET) on url " + url_get
        print "HEADERS :"
        print headers_perso
        print

        response = requests.get(url_get, verify=False, headers=headers_perso)
        print "Response"
        print response.text

        if response.status_code != 200:
            print>> sys.stderr, "Error !!! " + response.text
            exit(2)

        print "Loading Json …"
        data = json.loads(response.text)
        print str(len(data)) + " entities of model " + DC_PROJECT + " fetch on page " + str(page)
        print

        if len(data) < 1:
            print "No data to work … exit"
            break

        print "Sending request (POST) for update on " + DC_URL
        print "Headers :"
        print headers_perso
        print "Body :"
        print response.text.encode("utf-8")
        response = requests.post(DC_URL, response.text.encode("utf-8"), verify=False, headers=headers_perso)
        #response.status_code = 201 # Pour le debug

        if response.status_code == 409:
            if retry_counter >= retry_limit:
                print>> sys.stderr, "%d retry on same page fails … something wrong … exit …" % retry_limit
                exit(4)
            print>> sys.stderr, "Conflict error (409)!!! Retry the same page !!!"
            retry_counter += 1

        elif response.status_code == 201:
            print "Success !!!"
            if len(data) == ENTITY_BY_PAGE:
                id_cursor = urllib.quote_plus(">" + data[ENTITY_BY_PAGE - 1]["@id"] + "+")
                print "next id_cursor = %s" % id_cursor
                retry_counter = 0
                page += 1

            else:
                print "End of list"
                print "Last @id = " + data[len(data)-1]["@id"]

        else:
            print>> sys.stderr, "Response code not supported (%s) … exit" % response.status_code
            print>> sys.stderr, "Response body : %s" % response.text
            exit(5)



    print "Total updated : " + str((ENTITY_BY_PAGE * (page - 1)) + len(data))

if __name__ == "__main__":
    try:
        os.mkdir(LOG)
    except:
        pass

refresh_collection()
