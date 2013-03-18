#! /bin/bash

# Copy icons from "Android design icons"
# execute from the project root directory
# and te first argument is the source directtory
# without the hdpi, mdpi, etc.
# For example:
# ./copy_icons.sh ~Downloads/Android_Design/icons/All_Icons/holo_dark

if [ "$1" = "" ]
then
	echo "Missing source directory"
	exit 1
fi


DIR=$1
SUBDIRS="hdpi mdpi xhdpi"

cat icons.list |
while read line 
do
	if [ "$line" = "" ]; then continue; fi

	original=$(echo $line | awk '{print $1}')
	copy=$(echo $line | awk '{print $2}')

	for subdir in $SUBDIRS
	do
		echo "$original -> $copy"
		cp $DIR/$subdir/$original  res/drawable-$subdir/$copy
	done

done
