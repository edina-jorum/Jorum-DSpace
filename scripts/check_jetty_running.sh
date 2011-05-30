#!/bin/bash

#/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# *
# *
# *  University Of Edinburgh (EDINA) 
# *  Scotland
# *
# *
# *  File Name           : check_jetty_running.sh
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
echo "* Checking Jetty ...."
echo "*"

	echo "* Checking if something is listening on port ${jetty.port} ...."
	echo "*"
	echo "* Curl Output: "
	echo "*"
	
	# NOTE: Would normally use lsof for this but it requires root privleges and we probably aren't executing this as root
	
	${curl.path} http://${jetty.host}:${jetty.port}
	CURL_EXIT_CODE=$?

	if [ ${CURL_EXIT_CODE} -ne 0 ]; then
		# Curl returned an error code so somtehing isn't right
		echo "* ERROR: Curl returned error code ${CURL_EXIT_CODE} connecting to http://${jetty.host}:${jetty.port}"
		
		echo "*"
		echo "* Jetty may not be running - exiting with failure" 
		echo "*"
		echo "******************************************************"
		RET_CODE=1 
	else
		echo "* Curl returned success, Jetty is running."
		echo "*"
		echo "******************************************************"
	fi

cd $PWD

exit ${RET_CODE}