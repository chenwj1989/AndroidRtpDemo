//
// Created by Lenovo's user on 5/16/2020.
//

#ifndef AUDIORECODER_LOG_H
#define AUDIORECODER_LOG_H

#endif //AUDIORECODER_LOG_H


#include <android/log.h>
#define LOGI(FORMAT,...) __android_log_print(ANDROID_LOG_INFO,"rtp_demo",FORMAT,##__VA_ARGS__);
#define LOGD(FORMAT,...) __android_log_print(ANDROID_LOG_DEBUG,"rtp_demo",FORMAT,##__VA_ARGS__);
#define LOGE(FORMAT,...) __android_log_print(ANDROID_LOG_ERROR,"rtp_demo",FORMAT,##__VA_ARGS__);
