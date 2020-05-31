//
// Created by Lenovo's user on 5/12/2020.
//

#include "PacketQueue.h"
#include "log.h"

PacketQueue::PacketQueue(unsigned int size) : queue_size(size) {
    pthread_mutex_init(&mutex_queue, NULL);
    pthread_cond_init(&cond_not_full, NULL);
    pthread_cond_init(&cond_not_empty, NULL);
}

PacketQueue::~PacketQueue() {

}
int PacketQueue::put(Packet& packet) {

    pthread_mutex_lock(&mutex_queue);
    if (packetQueue.size() >= queue_size)
        pthread_cond_wait(&cond_not_full, &mutex_queue);

    LOGD("%s%u", "PacketQueue put(): queue size is ", packetQueue.size());
    packetQueue.push(std::move(packet));
    pthread_cond_signal(&cond_not_empty);
    pthread_mutex_unlock(&mutex_queue);

    return 0;
}

int PacketQueue::put(Packet&& packet) {

    pthread_mutex_lock(&mutex_queue);
    if (packetQueue.size() >= queue_size)
        pthread_cond_wait(&cond_not_full, &mutex_queue);

    LOGD("%s%u", "PacketQueue put(): queue size is ", packetQueue.size());
    packetQueue.push(std::forward<Packet>(packet));
    pthread_cond_signal(&cond_not_empty);
    pthread_mutex_unlock(&mutex_queue);

    return 0;
}

int PacketQueue:: get(Packet& packet) {

    pthread_mutex_lock(&mutex_queue);
    if(packetQueue.size() == 0)
        pthread_cond_wait(&cond_not_empty, &mutex_queue);

    LOGD("%s%u", "PacketQueue get(): queue size is ", packetQueue.size());
    packet =  std::move(packetQueue.front());
    packetQueue.pop();
    pthread_cond_signal(&cond_not_full);
    pthread_mutex_unlock(&mutex_queue);
    return 0;
}
