import sys
import os.path
sys.path.append(os.path.abspath('../../../'))

from distutils.core import setup, Extension

from src.util.FileSystem import FileSystem

extDir = FileSystem.getExtDir()
jsonPath = os.path.join(extDir, 'SimpleJSON-master/src')
jsonObj1Path = os.path.join(extDir, 'SimpleJSON-master/obj/JSON.o')
jsonObj2Path = os.path.join(extDir, 'SimpleJSON-master/obj/JSONValue.o')

module1 = Extension(
    'PyMatch',
    sources = ['PyMatch.cpp', 'Match.cpp'], 
    extra_objects = [jsonObj1Path, jsonObj2Path], 
    include_dirs = ['.', jsonPath], 
    library_dirs = [], 
    libraries = [],
    extra_compile_args = ['-fPIC']
)

setup (name = 'PyMatch',version = '0.1',description = 'Matches octave code.',ext_modules = [module1], packages = [])
