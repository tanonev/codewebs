'''
Created on Nov 29, 2012

@author: piech
'''

import sys
import os.path

ROOT = '../'
PYTHONUTIL = '../src/pythonUtil'
UNITTESTING = '../src/octave_unittesting'
SCRIPTS = '../scripts'
SITE = '../site/cwsite/cwsite'

class PathImporter(object):

    @staticmethod
    def importPythonPaths():
        print 'import all python source paths...'
        pythonUtilPath = os.path.abspath(PYTHONUTIL)
        unitTestingPath = os.path.abspath(UNITTESTING)
        scriptsPath = os.path.abspath(SCRIPTS)
        sitePath = os.path.abspath(SITE)
        PathImporter._importPaths(pythonUtilPath)
        PathImporter._importPaths(unitTestingPath)
        PathImporter._importPaths(scriptsPath)
        PathImporter._importPaths(sitePath)
   
    @staticmethod
    def importAllPaths():
        print 'import all paths...'
        rootPath = os.path.abspath(ROOT)
        print rootPath
        PathImporter._importPaths(rootPath)
        
    @staticmethod
    def _importPaths(dirPath):
        assert os.path.isdir(dirPath)
        sys.path.append(dirPath)
        for thing in os.listdir(dirPath):
            newDirPath = os.path.join(dirPath, thing)
            if os.path.isdir(newDirPath):
                print newDirPath
                PathImporter._importPaths(newDirPath)
