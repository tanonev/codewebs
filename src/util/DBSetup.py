from django.core.management import setup_environ
#from django.conf import settings
#sys.path.append(os.path.abspath(''))
import sys
from FileSystem import FileSystem
sys.path.append(FileSystem.getSiteDir())
import settings

#settings.configure(
#    DATABASE_ENGINE = 'django.db.backends.mysql',
#    DATABASE_NAME = 'codewebdb',
#    DATABASE_USER = 'codewebdb',
#    DATABASE_PASSWORD = 'n3gr0n1',
#    DATABASE_HOST = 'evariste.stanford.edu',
#    DATABASE_PORT = '',
#    TIME_ZONE = 'America/Los_Angles',
#)

setup_environ(settings)


