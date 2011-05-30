#!/bin/bash

#/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# *
# *
# *  University Of Edinburgh (EDINA) 
# *  Scotland
# *
# *
# *  File Name           : launch_jetty.sh
# *  Author              : gwaller
# *  Approver            : Gareth Waller 
# *
# *~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# *
# */

PWD=pwd
DIR_TO_RUN=${basedir}

#echo "Changing directory to: ${DIR_TO_RUN}"
cd $DIR_TO_RUN

RET_CODE=0

echo
echo
echo "******************************************************"
echo "*"
echo "* Launching Jetty ...."
echo "*"

# Set the BUILD_ID env var so that if this is run from Hudson it won't kill the spawned process when the build terminates.
# See https://hudson.dev.java.net/issues/show_bug.cgi?id=2729 
BUILD_ID=dontkillmehudson ${mvn.path} exec:exec -Dexec.executable='java' -Dexec.args='-Dnet.sourceforge.cobertura.datafile=${cobertura.data.file} -cp %classpath uk.ac.jorum.JettyWebAppLoader ${filtered.dir}/${jetty.config.name} ${jetty.stop.port} ${jetty.stop.key}' 2>&1 > ${uk.ac.jorum.JettyWebAppLoader.out} &

echo "*"
echo "******************************************************"
	
cd $PWD

exit 0