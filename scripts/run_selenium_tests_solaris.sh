#!/bin/bash

#/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# *
# *
# *  University Of Edinburgh (EDINA) 
# *  Scotland
# *
# *
# *  File Name           : run_selenium_test_solaris.sh
# *  Author              : gwaller
# *  Approver            : Gareth Waller 
# *
# *~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# *
# */

BROWSER_PATH=/home/jorum/opt/firefox

PWD=pwd
DIR_TO_RUN=${basedir}

#echo "Changing directory to: ${DIR_TO_RUN}"
cd $DIR_TO_RUN

RET_CODE=0

echo
echo
echo "******************************************************"
echo "*"
echo "* Executing Selenium tests against URL http://${http.auth.login}${dspace.host}:${dspace.port}/${dspace.default.webapp}"
echo "*"
echo "*"
echo "******************************************************"

# Unset LD_PRELOAD_32 as this will conflict with Xvfb
LDPRE=`echo ${LD_PRELOAD_32}`
export LD_PRELOAD_32=

# Now start xvfb
/usr/X/bin/Xvfb &

export DISPLAY=:0

# Make sure browser is on path
OLDPATH=`echo $PATH`
export PATH=$BROWSER_PATH:$PATH

# Now run the tests
cd ${selenium.test.dir}
${mvn.path} ${mvn.integration.test.cmd}
RESULT=$?

# Reset path
export PATH=$OLDPATH

# Now kill Xvfb
PID=`ps -ef | grep "[v]fb" | awk '{print $2}'`
kill -TERM $PID

# Now reset LD_PRELOAD_32
export LD_PRELOAD_32=${LDPRE}

cd $PWD

exit $RESULT