//
// Created by Lenovo's user on 5/12/2020.
//

#include "Packet.h"
#include <string.h>

Packet::Packet()
        : _size(0), _data(nullptr) {
}
Packet::Packet(size_t size)
        : _size(size), _data(new uint8_t[size]) {
}

Packet::Packet(size_t size, const uint8_t *data)
        : _size(size), _data(new uint8_t[size])  {
    memcpy(_data, data, size);
}
Packet::~Packet() {

    if (_data != nullptr)
    {
        delete[] _data;
    }

}


Packet::Packet(Packet &&rhs)
        : _size(rhs._size), _data(rhs._data)
{
    rhs._data = nullptr;
    rhs._size = 0;
}

Packet& Packet::operator=(Packet &&rhs) {

    if (this != &rhs) {
        delete[] this->_data;

        this->_size = rhs._size;
        this->_data = rhs._data;
        rhs._data = nullptr;
        rhs._size = 0;
    }
    return *this;
}

