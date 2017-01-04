#! /usr/bin/env python

from PathImporter import PathImporter
PathImporter.importPythonPaths()
from RunExternal import RunExternal

runner = RunExternal(['sleep','3'],10)
runner2 = RunExternal(['sleep','3'],1)

print('This should take 3 seconds')
runner.go()
print('Done')
print('This should take 1 second')
runner2.go()
print('Done')



