#! /usr/bin/env python

from PathImporter import PathImporter
PathImporter.importPythonPaths()
from FileSystem import FileSystem
import DBSetup
from UnitTester import UnitTester
from RunExternal import RunExternal
from MLClass import MLClass
import sys
import os

ASTCMD = '../bin/astgen'
TARCMD = 'tar -czf'
MAXTIME = 5
dataDir = os.path.join(FileSystem.getDataDir(),'ast','starter')
dataDirParent = os.path.join(FileSystem.getDataDir(),'ast')

def genAST(hwId,partId):
    print('Generating starter code AST for homework ' + str(hwId) + ', part ' + str(partId))
    tester = UnitTester(hwId,partId)
    startercode = tester.unitTestFile
    fname = 'starter_' + str(hwId) + '_' + str(partId)
    fname_json = os.path.join(dataDir, fname + '.json')
    fname_code = os.path.join(dataDir, fname + '.code')
    fname_map = os.path.join(dataDir, fname + '.map')
    astgenRunner = RunExternal([ASTCMD, startercode, fname_json,fname_code,fname_map],MAXTIME)
    astgenRunner.go()

def run():
    if not os.path.exists(dataDir):
        os.makedirs(dataDir)
    for hwId, partId in MLClass.allProblems():
        genAST(hwId,partId)
    print('Compressing results')
    tarfile = 'ast_starter.tar.gz'
    os.system('cd ' + dataDirParent + '; ' + TARCMD + ' ' + tarfile + ' starter')
    os.system('rm -rf ' + dataDir)
    print('Done.')
run()

