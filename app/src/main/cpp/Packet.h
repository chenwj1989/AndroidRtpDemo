//
// Created by Lenovo's user on 5/12/2020.
//

#ifndef AUDIORECODER_PACKET_H
#define AUDIORECODER_PACKET_H

#include <stdint.h>
#include <stddef.h>

class Packet
{
public:
    explicit Packet();
    explicit Packet(size_t size);
    explicit Packet(size_t size, const uint8_t* data);

    ~Packet();
    Packet(Packet &&rhs);
    Packet& operator=(Packet &&rhs);

    inline uint8_t* data() {
        return _data;
    }

    inline size_t size() {
        return _size;
    }

//disallow copying
    Packet(const Packet &rhs) = delete;
    Packet& operator=(const Packet &rhs) = delete;
private:
    uint8_t* _data;
    size_t _size;
};

#endif //AUDIORECODER_PACKET_H
