#! /usr/bin/env python

import json
import sys
sys.path.append('..')
from CodewebsUnitTestClient import CodewebsUnitTestClient

def loadTextFile(fname):
    with open(fname) as fid:
        return fid.read()

def run():
    codetext = loadTextFile('code')
    inputJSON = codetext
    UTclient = CodewebsUnitTestClient()
    
    print " [x] Requesting!"
    response = UTclient.call(inputJSON)
    print " [.] Got %r" % (response,)

if __name__ == '__main__':
    run()

