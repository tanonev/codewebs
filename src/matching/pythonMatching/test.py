import sys
import os.path
sys.path.append(os.path.abspath('../../../'))
sys.path.append(os.path.abspath('../../../site/cwsite/'))
import src.util.DBSetup
from src.util.FileSystem import FileSystem
from cwsite.models.models import *
import PyMatch

astOne = Octave.objects.get(id=1).json
astTwo = Octave.objects.get(id=100).json
keywords = 'ast\ntest\nwhat'

distance = PyMatch.getEditDistance(astOne, astTwo, keywords)
print distance
