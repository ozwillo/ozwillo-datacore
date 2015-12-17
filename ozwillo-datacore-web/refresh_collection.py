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
SKIP_ON_ERROR = True  # If unsupported error occur skip the page
SIMULATION = False
SIMULATION_CODE = 201  # If simulation is true is the response code of the false response (201 nominal, 409 retry)

if len(sys.argv) < 3:
    TYPE = sys.argv[1]
    DC_PROJECT = sys.argv[2]
    BEARER = sys.argv[3]
else:
    print "Missing args, please provide type (1, ex: 'org:Organization_0') project (2, ex: 'org_1') and bearer (3) as argument of this script"
    print "Loading default (bearer will be surely wrong)"
    TYPE = "org:Organization_0"
    DC_PROJECT = "org_1"
    BEARER = "eyJpZCI6ImE1M2ViNTg4LTc3NTQtNDI0My05MmNmLTI4OTdlNDZhMjFmYy9CVGJPRTY1UlhCa2RvZExaTXVSVC13IiwiaWF0IjoxNDUwMjc0NjM2ODkzLCJleHAiOjE0NTAyNzgyMzY4OTN9"

LOG = "./logs"


def refresh_collection():
    content_type = "application/json; charset=UTF-8"
    x_datacore_project = DC_PROJECT
    authorization = "Bearer %s" % BEARER

    headers_perso = {'Content-Type': content_type,
                     'X-Datacore-Project': x_datacore_project,
                     'Authorization': authorization}

    page = 1
    go_to_next_page = False
    retry_counter = 0
    retry_limit = 3
    id_cursor = "+"  # for the first request arg @id is "+"
    data = {}
    errors_for_retry = {409, 504}
    while id_cursor != "":

        url_get = DC_URL + "type/" + TYPE + "?limit=" + str(ENTITY_BY_PAGE) + "&%40id=" + urllib.quote_plus(id_cursor)
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
            print>> sys.stderr, "ERROR : Response code not supported (%d)" % response.status_code
            print>> sys.stderr, "ERROR : " + response.text
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

        if SIMULATION:
            print "Simulation !!!"
            response.status_code = SIMULATION_CODE  # Pour le debug
        else:
            response = requests.post(DC_URL, response.text.encode("utf-8"), verify=False, headers=headers_perso)

        if response.status_code in errors_for_retry:
            if retry_counter >= retry_limit:
                print>> sys.stderr, "ERROR : %d retry on same page fails … something wrong … skip …" % retry_limit
                go_to_next_page = True

            print>> sys.stderr, "ERROR : Response code %s, retry the same page !!!" % response.status_code
            print>> sys.stderr, "ERROR : Response body %s" % response.text
            retry_counter += 1

        elif response.status_code == 201:
            print "Success !!!"
            go_to_next_page = True

        else:
            print>> sys.stderr, "ERROR : Response code not supported (%s)" % response.status_code
            print>> sys.stderr, "ERROR : Response body : %s" % response.text

            if SKIP_ON_ERROR:
                print>> sys.stderr, "Skip this error and go to the next page (%d)" % (page + 1)
                go_to_next_page = True
            else:
                exit(3)

        if go_to_next_page:
            go_to_next_page = False
            if len(data) == ENTITY_BY_PAGE:
                id_cursor = ">" + data[ENTITY_BY_PAGE - 1]["@id"] + "+"
                print "next id_cursor = %s" % id_cursor
                retry_counter = 0
                page += 1

            else:
                print "End of list"
                print "Last @id = " + data[len(data) - 1]["@id"]
                id_cursor = ""

    print "Total treat : " + str((ENTITY_BY_PAGE * (page - 1)) + len(data))


if __name__ == "__main__":
    try:
        os.mkdir(LOG)
    except:
        pass

refresh_collection()
