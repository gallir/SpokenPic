#include <stdio.h>
#include <stdlib.h>

#include <stdint.h>
#include <faac.h>

/*
-q quality VBR 10-500
-b bitrate AVB
-c freq bandwidth en HZ
-P raw PCM
-R Raw PCM input sample rate in Hz
-B Raw PCM input sample size
-X Raw PCM swap input bytes
-C Raw PCM input channels
*/


main(int argc, char **argv) {

	int quality = atol(argv[1]);
	int type = atol(argv[2]);

	long samplerate = 16000;
	long channels = 1;
	long samples_input;
	long max_bytes_output;
	faacEncHandle hEncoder;
	faacEncConfigurationPtr mformat;

	hEncoder = faacEncOpen(samplerate, channels, &samples_input, &max_bytes_output);

	printf("Samples input: %ld max_bytes: %ld\n", samples_input, max_bytes_output);


	uint16_t big_input[4096];

	uint16_t input[samples_input];
	char output[max_bytes_output];


	mformat = faacEncGetCurrentConfiguration(hEncoder);


	printf("aacObjectType %d, bitRate %ld, inputFormat %d, outputFormat %d, quantqual %ld, useTns %d, bandWidth %d\n", mformat->aacObjectType, mformat->bitRate, mformat->inputFormat, mformat->outputFormat, mformat->quantqual, mformat->useTns, mformat->bandWidth);

	mformat->inputFormat = 	FAAC_INPUT_16BIT;
	mformat->outputFormat = 1; // ADTP format
	mformat->quantqual = quality;
	mformat->aacObjectType = type;

	if (! faacEncSetConfiguration(hEncoder, mformat)) {
		puts("Don't accept");
		exit(1);
	}

	printf("aacObjectType %d, bitRate %ld, inputFormat %d, outputFormat %d, quantqual %ld, useTns %d, bandWidth %d\n", mformat->aacObjectType, mformat->bitRate, mformat->inputFormat, mformat->outputFormat, mformat->quantqual, mformat->useTns, mformat->bandWidth);


	int written, r, file;

	int ofile = creat("out.aac", 00600);

	while( (r = read(0, input, samples_input * sizeof(uint16_t))) > 0   ) {
		written = faacEncEncode(hEncoder, (short *) input, r/sizeof(uint16_t), output, max_bytes_output);
		if (written > 0) {
			write(ofile, output, written);
		}
	}

	// Save last frames
	while ((written = faacEncEncode(hEncoder, (short *) input, 0, output, max_bytes_output)) > 0) {
			write(ofile, output, written);
	}

	faacEncClose(hEncoder);
	close(ofile);

}

/*
faacEncGetCurrentConfiguration(),

    myFormat = faacEncGetCurrentConfiguration(hEncoder);
    myFormat->aacObjectType = objectType;
    myFormat->mpegVersion = mpegVersion;
    myFormat->useTns = useTns;

   if (!faacEncSetConfiguration(hEncoder, myFormat)) {
        fprintf(stderr, "Unsupported output format!\n");
#ifdef HAVE_LIBMP4V2
        if (container == MP4_CONTAINER) MP4Close(MP4hFile);
#endif
        return 1;
    }


           samplesRead = wav_read_float32(infile, pcmbuf, samplesInput, chanmap);


 /* call the actual encoding routine *****
            bytesWritten = faacEncEncode(hEncoder,
                (int32_t *)pcmbuf,
                samplesRead,
                bitbuf,
                maxBytesOutput);


if (bytesWritten > 0)

/* write bitstream to aac file /
                    fwrite(bitbuf, 1, bytesWritten, outfile);

*/
