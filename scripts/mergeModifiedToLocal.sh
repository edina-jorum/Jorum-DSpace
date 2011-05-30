#!/bin/bash

#/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# *
# *
# *  University Of Edinburgh (EDINA) 
# *  Scotland
# *
# *
# *  File Name           : mergeModifiedToLocal.sh
# *  Author              : gwaller
# *  Approver            : Gareth Waller 
# *
# *~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# *
# */


# Example diff3 command:
# diff3 -m -E -L MINE -L GOLDEN -L SVN patched_src/dspace/config/dspace.cfg golden/dspace-1.5.2/dspace/config/dspace.cfg dspace-1.5.2/dspace/config/dspace.cfg > dspace.cfg.merged
# exit code 1 = conflicts
# exit code 0 = fine

# find . -type f ! -regex '.*/\.svn/.*' -print
# lists all files that aren't subversion files

PWD=pwd
DIR_TO_RUN=${patched.src.dir}
GOLDEN_DSPACE_DIR=${expanded.dspace.golden.dir}
MODIFIED_DIR=${modified.dir}
PATCHED_DIR=${patched.src.dir}
DIRNAME_PATH=${dirname.path}
MKDIR_PATH=${mkdir.path}
DIFF3_PATH=${diff3.path}
DIFF_PATH=${diff.path}
BACKUP_DIR=${backup.dir}
XXDIFF_PATH=${xxdiff.path}

#echo "Changing directory to: ${DIR_TO_RUN}"
cd $DIR_TO_RUN

# Get list of files in the modified dir first - these can then be merged into the working dir
FIND_CMD="(cd ${MODIFIED_DIR}; find . -type f ! -regex '.*/\.svn/.*' -print)"
FILES_TO_MERGE=$(eval $FIND_CMD) # ******* Must use eval here to execute command as arguments are passed correctly to the called command!!! 

FILES_ARR=($FILES_TO_MERGE) # Store the filenames from running the above command into an array
NUM_FILES=${#FILES_ARR[@]}

#echo $FILES_TO_MERGE

echo "******************************************************"
echo "*"
echo "* Merging files .... "
echo "*"
echo "******************************************************"
if [ ${NUM_FILES} -eq 0 ]; then
	echo "* No files found to merge from dir ${MODIFIED_DIR}"
else
	
	CONFLICTS=0
	ERRORS=0
	for i in ${FILES_TO_MERGE}; do
		
		# Ignore Mac .DS_Store metadata files
		FILENAME=`basename $i`
		if [ ${FILENAME} = ".DS_Store" ]; then
			continue
		fi
		
		if [ -f ${PATCHED_DIR}/$i ]; then
			# File exists - now run the merge command
			
			# Now check if the files are different i.e. do we need to do a merge at all!
			DIFF="${DIFF_PATH} ${BACKUP_DIR}/latest/$i ${MODIFIED_DIR}/$i"
			DIFF_OUTPUT=$(eval $DIFF)
			DIFF_EXIT_CODE=$?
			
			if [ ${DIFF_EXIT_CODE} -eq 0 ]; then	
				continue; # No diffs - move onto the next file
			fi
			
			# Use the Graphical xxdiff utility if it is available
			if [ -x ${XXDIFF_PATH} ]; then
			
				# The mac xxdiff app appears to ignore some command line options, but does parse the .xxdiffrc file in the users home directory. 
				# This merge script depends on values in this file so check if it exists first.
				if [ ! -f ~/.xxdiffrc ]; then
					echo ""
					echo "Error: file ~/.xxdiffrc file not found - this must exist for the merge to work correctly as xxdiff ignores some command line arguments."
					echo ""
					echo "Please ensure the file has atleast the following options: "
					echo ""
					cat ../etc/example.xxdiffrc
					echo ""
					exit 1
				fi
			
				# If we got here then the .xxdiffrc file exists - now check the important options
				for param in Show.PaneMergedView ExitOnSame ExitIfNoConflicts ExitWithMergeStatus; do
					GREP="grep -q -i -e '^${param}[[:space:]]*:[[:space:]]*True[[:space:]]*' ~/.xxdiffrc"
					GREP_OUTPUT=$(eval $GREP)
					GREP_EXIT_CODE=$?
					
					# Grep will return 0 if match found
					if [ ${GREP_EXIT_CODE} -ne 0 ]; then
						# Config param either wasn't found in the config file at all or it wasn't set to True
						echo ""
						echo "Error: file ~/.xxdiffrc should set option $param to True - please correct by adding the line '$param: True'"
						echo ""
						exit 1
					fi
				done
						
				MERGE_WORKED=0
				
				while [  $MERGE_WORKED -eq 0 ]; do
					echo "Merging file from modified ${MODIFIED_DIR}/$i into local copy under ${PATCHED_DIR}"
					MERGE="${XXDIFF_PATH} -m -a --exit-on-same --exit-if-no-conflicts --exit-with-merge-status --decision -M ${PATCHED_DIR}/$i ${MODIFIED_DIR}/$i ${BACKUP_DIR}/latest/$i"
					MERGE_OUTPUT=$(eval $MERGE)
					MERGE_EXIT_CODE=$?
				
					# We have forced the user to make a decision on the merge with --decision. xxdiff should output either REJECT, ACCEPT, MERGED, NODECISION
					if [ ${MERGE_OUTPUT} != "NODECISION" ]; then
						# xxdiff exited ok
						MERGE_WORKED=1
					fi
				
				done
				
			else
				# No XXDIFF - use diff3 instead
			
				# The below command will incorporate all the changes from modified to the backup up copy into the patched file (which is the same as the backup copy)
				# in other words merge the modified file into the patched/backup one
				MERGE="${DIFF3_PATH} -m -a -A -L MINE -L SVN -L MINE ${BACKUP_DIR}/latest/$i ${MODIFIED_DIR}/$i ${BACKUP_DIR}/latest/$i > ${PATCHED_DIR}/$i"
				MERGE_OUTPUT=$(eval $MERGE)
				MERGE_EXIT_CODE=$?
			fi
			
			
			if [ ${MERGE_EXIT_CODE} -eq 2 ]; then
				# Error occurred
				echo "* E ${PATCHED_DIR}/$i"
				ERRORS=1
				break; # Stop processing files
			elif [ ${MERGE_EXIT_CODE} -gt 0 ]; then
				# Conflicts found - inform the user
				echo "* C ${PATCHED_DIR}/$i"
				CONFLICTS=1
			else
				echo "* ${PATCHED_DIR}/$i ... merged"
			fi
			
			
		else
			# Just copy the file across - it must be new
			echo "* Copying ${MODIFIED_DIR}/$i -> ${PATCHED_DIR}/$i"
			# Make sure the dir structure exists - ie create it if necessary
			${MKDIR_PATH} -p `${DIRNAME_PATH} ${PATCHED_DIR}/$i`
			cp ${MODIFIED_DIR}/$i ${PATCHED_DIR}/$i
		fi
	done
	
	echo
	echo
	echo "******************************************************"
	echo "*"
	echo "* SUMMARY"
	echo "*"
	
	if [ ${ERRORS} -eq 1 ]; then
		echo "*"
		echo "* !!! ERRORS FOUND - PLEASE MANUALLY FIX THESE !!!"
		echo "*" 
		echo "* Some merges may have occurred, please see above for diff output and below for any notice of conflicts"
		echo "* The merge did not complete and errors must be resolved before attempting again."
		echo "*"
	fi
	
	if [ ${CONFLICTS} -eq 1 ]; then
		echo "* !!! CONFLICTS FOUND - PLEASE MANUALLY FIX THESE !!!"
		echo "* Files with conflicts are listed above with a preceeding 'C'"
	else
		echo "* No conflicts found during merge"
	fi
	
	echo "*"
	echo "******************************************************"
fi

cd $PWD

exit 0