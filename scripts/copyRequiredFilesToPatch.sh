#!/bin/bash

#/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# *
# *
# *  University Of Edinburgh (EDINA) 
# *  Scotland
# *
# *
# *  File Name           : copyRequiredFilesToPatch.sh
# *  Author              : gwaller
# *  Approver            : Gareth Waller 
# *
# *~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# *
# */


PWD=pwd
DIR_TO_RUN=${build.dir}
DSPACE_DIR=${dspace.prefix}${dspace.version}
PATCHED_DIR=${patched.src.dir.name}
MODIFIED_DIR=${modified.dir}
DIFF_PATH=${diff.path}
DIFF_ARGS="${diff.args}"
AWK_PATH=${awk.path}
SED_PATH=${sed.path}
DIRNAME_PATH=${dirname.path}
MKDIR_PATH=${mkdir.path}

#echo "Changing directory to: ${DIR_TO_RUN}"
cd $DIR_TO_RUN

# The below command will run a quiet diff (note -q) pipe this through AWK to pull out the second column, then pipe it through sed to remove the DSpace prefix
# then finally pipe it through Sed again to remove only lines with the text "in". The last sed is necessary as if a file exists in the 
# modified directory but didn't exist in the patched_src directoy the output from the diff command would be along the lines "Only in dspace-1.5.2/....". Awk would
# then take the second column i.e. "in" and this would be piped through sed. Obviously if the file only exists in the modified dir it isn't a locally
# modified file so we can ignore it and not include it in the results. NOTE: this does not happen the other way i.e. if a file is only in the 
# patched_src directory thanks to the --unidirectional-new-file switch on the diff command.
CMD="${DIFF_PATH} ${DIFF_ARGS} -q ${DSPACE_DIR} ${PATCHED_DIR} | ${AWK_PATH} '{ print \$2 }' | ${SED_PATH} 's/^${DSPACE_DIR}\///' | ${SED_PATH} '/^in$/d'"

FILES=$(eval $CMD) # ******* Must use eval here to execute command as areguments are passed correctly to the called command!!! 
#echo $FILES

FILE_ARR=($FILES) # Store the filenames from running the above command into an array
NUM_FILES_CHANGED=${#FILE_ARR[@]}

echo "******************************************************"
echo "*"
if [ ${NUM_FILES_CHANGED} -eq 0 ]; then
	echo " * No locally modified files found"
else
	if [ $1 = 'list' ]; then
		echo "* Found local modifications !"
		echo "*"
		echo "* Modified files are:"
		echo "*"
	fi
	
	z=0
	for i in ${FILES}; do
		if [ $1 = 'list' ]; then
			echo -e "*\t$i"
		fi
		
		if [ -f ${MODIFIED_DIR}/$i ]; then
			#echo "Clearing element at $z"
			# clear the element in the array so we don't copy
			FILE_ARR[$z]=''
		fi
		z=$(( z + 1 ))
	done
	
	echo "*"
	
	for i in ${FILE_ARR[@]}; do
		#echo $i
		if [ ! -z $i ]; then
			# length non-zero - therefore this is a file we need to copy
			
			# Need to determine which file to copy into the modified dir. If it exists in the golden dir
			# then use this one (as the patch will be applied later), if not it is a brand new file so 
			# don't copy anything as the patch command will create this.

			if [ $1 = 'copy' ]; then
				# Make sure the dir structure exists - ie create it if necessary
				${MKDIR_PATH} -p `${DIRNAME_PATH} ${MODIFIED_DIR}/$i`
			
				if [ -f ${DSPACE_DIR}/$i ]; then
					echo "* Copying ${DSPACE_DIR}/$i -> ${MODIFIED_DIR}/$i"
			
					COPY_CMD="cp ${DSPACE_DIR}/$i ${MODIFIED_DIR}/$i"
					eval $COPY_CMD
				fi
			
			fi
		fi
	done
	
fi

echo "*"
echo "******************************************************"

cd $PWD

exit 0