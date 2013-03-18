LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libfaac

LOCAL_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_CFLAGS += -DHAVE_INT32_T


LOCAL_SRC_FILES := \
	./libfaac/huffman.c \
	./libfaac/tns.c \
	./libfaac/fft.c \
	./libfaac/backpred.c \
	./libfaac/kiss_fft/kiss_fftr.c \
	./libfaac/kiss_fft/kiss_fft.c \
	./libfaac/bitstream.c \
	./libfaac/ltp.c \
	./libfaac/aacquant.c \
	./libfaac/filtbank.c \
	./libfaac/util.c \
	./libfaac/frame.c \
	./libfaac/midside.c \
	./libfaac/channels.c \
	./libfaac/psychkni.c \

include $(BUILD_SHARED_LIBRARY)

