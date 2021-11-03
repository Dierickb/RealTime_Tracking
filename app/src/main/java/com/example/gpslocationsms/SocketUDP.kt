package com.example.gpslocationsms

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address

class SocketUDP(host: String, private val port: Int) {
    private var buffer:ByteArray? = null
    private val ip =Inet4Address.getByName(host)
    lateinit var socketUDP: DatagramSocket
    private lateinit var message: String

    fun start(){
        val thread = Thread{}
        thread.run { socketUDP = DatagramSocket() }
    }

    fun send(message: String){
        this.message = message
        this.buffer = this.message.toByteArray()
        val send = buffer?.let { DatagramPacket(buffer, it.size, this.ip, port) }
        val thread = Thread{socketUDP.send(send)}
        thread.start()
    }

    fun close(){
        socketUDP.close()
    }
}