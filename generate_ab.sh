#! /bin/bash

for file in res/drawable-xhdpi/ab_*
do
	basefile=$(basename $file)
	echo "File $file $basefile"
	while read line
	do
		if [[ "$line" != "" && ! "$line" =~ \ *\# ]]
		then
			type=$(echo $line | awk '{print $1}')
			size=$(echo $line | awk '{print $2}')
			echo $type '->' $size
			convert -resize $size $file res/drawable-$type/$basefile
		fi

	done < ActionBarIcons
done
