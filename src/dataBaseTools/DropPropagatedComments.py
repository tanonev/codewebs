from PathImporter import PathImporter
PathImporter.importPythonPaths()
import DBSetup
from models.models import *

def drop():
    print('Deleting all propagated comments')
    Comment.objects.filter(human_label=False).delete()

drop()

