#! /usr/bin/perl

use File::Basename;

$base_dir="res";
$orig_dir=$base_dir."/drawable-xhdpi";

%sizes=("hdpi" => 75, "mdpi" => 50, "ldpi" => 37.5);

@base=("focus*", "switch*", "flash*", "ab_*", "take_*", "stop*", "rec*", "ok_*", "play*", "gallery*", "cancel*", "plus*");

foreach $r (@base) {
	foreach $r (glob "${orig_dir}/${r}.png") {
		push(@files, basename($r));
	}
}


foreach $dir (keys %sizes) {
	foreach $file (@files) {
		$command = "convert $orig_dir/$file -resize ".$sizes{$dir}."% $base_dir/drawable-$dir/$file";
		print $command."\n";
		system($command);
	}
}

