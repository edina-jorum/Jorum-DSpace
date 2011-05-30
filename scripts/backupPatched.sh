#!/bin/bash

#/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# *
# *
# *  University Of Edinburgh (EDINA) 
# *  Scotland
# *
# *
# *  File Name           : backupPatched.sh
# *  Author              : gwaller
# *  Approver            : Gareth Waller 
# *
# *~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# *
# */

PWD=pwd
DIR_TO_RUN=${basedir}
PATCHED_DIR=${patched.src.dir}
PATCHED_DIR_NAME=${patched.src.dir.name}
BACKUP_DIR=${backup.dir}

#echo "Changing directory to: ${DIR_TO_RUN}"
cd $DIR_TO_RUN

CP_EXIT_CODE=0

# Backup the patched src dir
while [ 1 ]; do
	# while loop to get a unique file name - uses the date, time (to second). If dir exists, sleep for a sec and try again

	DATESTR=`date '+%d%m%y-%H-%M-%S'`
	BACKUP_NAME="${PATCHED_DIR_NAME}_${DATESTR}"
	
	if [ ! -e  ${BACKUP_DIR}/${BACKUP_NAME} ]; then
		echo "Starting backup of ${PATCHED_DIR} to ${BACKUP_DIR}/${BACKUP_NAME} ... may take a while..."
	
		cp -r ${PATCHED_DIR} ${BACKUP_DIR}/${BACKUP_NAME}
		CP_EXIT_CODE=$?
		# Break the while loop
		break;
	else
		sleep 1
	fi
	
done

echo
echo
echo "******************************************************"
echo "*"
echo "* BACKUP"
echo "*"

if [ ${CP_EXIT_CODE} -eq 0 ]; then
	
	SYM_LINK="${BACKUP_DIR}/latest"
	
	# Make a sym link to the latest backup
	if [ -e ${SYM_LINK} ]; then
		rm ${SYM_LINK}
	fi
	
	ln -s ${BACKUP_NAME} ${SYM_LINK}
	
	echo "* Backup complete at: ${BACKUP_DIR}/${BACKUP_NAME}"
	echo "* Symbolic link created at: ${BACKUP_DIR}/latest"
	echo "*"
	echo "******************************************************"
else
	echo "* ERROR - cp exited with an error code. See above for any cp errors"
	echo "*"
	echo "* Backup failed"
	echo "*"
	echo "******************************************************"
	exit 1
fi

cd $PWD

exit 0