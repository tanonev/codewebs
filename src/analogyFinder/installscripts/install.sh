#! /bin/bash

INSTALLDIR=/usr/local/codewebsindexdaemon
echo "Installing CodewebsIndexDaemon to $INSTALLDIR"

if [ ! -d "$INSTALLDIR" ]; then
    sudo mkdir $INSTALLDIR
fi
echo " ... copying libraries to $INSTALLDIR"
sudo cp -rf ../../../ext/commons-daemon-1.0.15 $INSTALLDIR
sudo cp -rf ../../../ext/rabbitmq $INSTALLDIR
sudo cp -rf ../../../ext/commons-math3-3.2 $INSTALLDIR
sudo cp ./localconfig $INSTALLDIR
echo " ... making err and log directories in $INSTALLDIR"
if [ ! -d "$INSTALLDIR/err" ]; then
    sudo mkdir $INSTALLDIR/err
fi
if [ ! -d "$INSTALLDIR/log" ]; then
    sudo mkdir $INSTALLDIR/log
fi
echo " ... copying runnable jar file to $INSTALLDIR"
sudo cp ../jar/CodewebsIndexDaemon.jar $INSTALLDIR

echo " ... setting permissions"
sudo chown root:root $INSTALLDIR/CodewebsIndexDaemon.jar
sudo chown root:root $INSTALLDIR/localconfig
sudo chown -R root:root $INSTALLDIR/err
sudo chown -R root:root $INSTALLDIR/log
sudo chown -R root:root $INSTALLDIR/commons-daemon-1.0.15
sudo chown -R root:root $INSTALLDIR/rabbitmq

echo " ... copying daemonscript to /etc/init.d"
sudo cp ../src/daemons/daemonscript.sh /etc/init.d/codewebsindexdaemon
sudo chown root:root /etc/init.d/codewebsindexdaemon
echo "Finished with installation."
