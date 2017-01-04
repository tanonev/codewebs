'''
Created on Nov 29, 2012

@author: piech
'''

import sys
import os.path

ROOT = '.'

class PathImporter(object):
    
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
                
PathImporter.importAllPaths()
