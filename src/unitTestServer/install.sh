#! /bin/bash

function assertdir {
    if [ ! -d "$1" ]; then
        sudo mkdir $1
    fi
}

# make director in usr/local
INSTALLDIR=/usr/local/codewebsindexdaemon
echo "Installing CodewebsUnitTestServer to $INSTALLDIR"
assertdir $INSTALLDIR

echo " ... copying files to $INSTALLDIR"
assertdir $INSTALLDIR/log
assertdir $INSTALLDIR/log/UnitTestServer
assertdir $INSTALLDIR/UnitTestServer

sudo cp CodewebsUnitTestDaemon.py $INSTALLDIR/UnitTestServer
sudo cp localconfig $INSTALLDIR/UnitTestServer
sudo cp codewebsunittester.conf /etc/init

echo " ... setting permissions"
sudo chown -R root:root $INSTALLDIR/UnitTestServer
sudo chown -R root:root $INSTALLDIR/log
sudo chown root:root /etc/init/codewebsunittester.conf
echo "Finished with installation."



