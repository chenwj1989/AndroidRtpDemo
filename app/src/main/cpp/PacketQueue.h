//
// Created by Lenovo's user on 5/12/2020.
//
#ifndef AUDIORECODER_PACKETQUEUE_H
#define AUDIORECODER_PACKETQUEUE_H

#include "Packet.h"
#include <queue>
#include "pthread.h"

class PacketQueue {
public:
    PacketQueue(unsigned int size);
    ~PacketQueue();

    int put(Packet& packet);
    int put(Packet&& packet);
    int get(Packet& packet);

    std::queue<Packet> packetQueue;
    pthread_mutex_t mutex_queue;
    pthread_cond_t cond_not_full;
    pthread_cond_t cond_not_empty;
private:
    unsigned int queue_size;
};


#endif //AUDIORECODER_PACKETQUEUE_H
