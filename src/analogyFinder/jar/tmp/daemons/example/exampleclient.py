#! /usr/bin/env python

import json
import sys
sys.path.append('..')

from CodewebsIndexClient import CodewebsIndexClient



def loadTextFile(fname):
    with open(fname) as fid:
        return fid.read()

def wrap(ast,code,map,codeblockid):
    astjson = json.loads(ast)
    wrappedJSON = {'ast': astjson, 
                    'code': code,
                    'map': map,
                    'codeblockid': codeblockid,
                    'querytype': 3}
    #return json.dumps(wrappedJSON,sort_keys = True,indent=4,separators=(',',': '))
    return json.dumps(wrappedJSON)

def run():
    codeblockid = 30
    asttext = loadTextFile('ast.json')
    codetext = loadTextFile('code')
    maptext = loadTextFile('map')
    inputJSON = wrap(asttext,codetext,maptext,codeblockid)

    asttext_bad = loadTextFile('ast_bad.json')
    codetext_bad = loadTextFile('code_bad')
    maptext_bad = loadTextFile('map_bad')
    inputJSON_bad = wrap(asttext_bad,codetext_bad,maptext_bad,codeblockid)


    cwindex = CodewebsIndexClient()

    print " [x] Requesting!"
    response = cwindex.call(inputJSON)
    print " [.] Got %r" % (response,)

    print " [x] Requesting!"
    response = cwindex.call(inputJSON_bad)
    print " [.] Got %r" % (response,)

if __name__ == '__main__':
    run()



