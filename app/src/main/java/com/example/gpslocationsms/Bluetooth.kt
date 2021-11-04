package com.example.gpslocationsms

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.os.Handler
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

const val MESSAGE_READ: Int = 0
const val MESSAGE_WRITE: Int = 1
const val MESSAGE_TOAST: Int = 2

class Bluetooth(private val handler: Handler) {

    private val TAG = Bluetooth::class.java.name

    private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @Throws(IOException::class)
    fun connect(dev: BluetoothDevice): BluetoothSocket? {
        var sock: BluetoothSocket? = null
        var sockFallback: BluetoothSocket?
        Log.d(TAG, "Starting Bluetooth connection..")
        try {
            sock = dev.createRfcommSocketToServiceRecord(MY_UUID)
            sock.connect()
        } catch (e1: Exception) {
            Log.e(TAG, "There was an error while establishing Bluetooth connection. Falling back..", e1)
            val clazz: Class<*> = sock!!.remoteDevice.javaClass
            val paramTypes = arrayOf<Class<*>>(Integer.TYPE)
            try {
                val m = clazz.getMethod("createRfcommSocket", *paramTypes)
                val params = arrayOf<Any>(Integer.valueOf(1))
                sockFallback = m.invoke(sock.remoteDevice, *params) as BluetoothSocket
                sockFallback.connect()
                sock = sockFallback
            } catch (e2: Exception) {
                Log.e(TAG, "Couldn't fallback while establishing Bluetooth connection.", e2)
                throw IOException(e2.message)
            }
        }
        return sock
    }

    inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {

        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmOutStream: OutputStream = mmSocket.outputStream
        private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

        override fun run() {
            var numBytes: Int // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                // Read from the InputStream.
                numBytes =
                    try {
                        mmOutStream.write("01 0c\r".toByteArray())
                        mmInStream.read(mmBuffer)
                        Log.d(TAG, mmBuffer.toString())
                        mmOutStream.write("01 0c\r".toByteArray())
                        mmInStream.read(mmBuffer)
                        Log.d(TAG, mmBuffer.toString())
                    } catch (e: IOException) {
                        Log.d(TAG, "Input stream was disconnected", e)
                        break
                    }
                // Send the obtained bytes to the UI activity.
                val readMsg = handler.obtainMessage(MESSAGE_READ, numBytes, -1, mmBuffer)
                readMsg.sendToTarget()
                sleep(3000)
            }
        }

        // Call this from the main activity to send data to the remote device.
        fun write(bytes: ByteArray) {
            try {
                mmOutStream.write(bytes)
            } catch (e: IOException) {
                Log.e(TAG, "Error occurred when sending data", e)

                // Send a failure message back to the activity.
                val writeErrorMsg = handler.obtainMessage(MESSAGE_TOAST)
                val bundle =
                    Bundle().apply {
                        putString("toast", "Couldn't send data to the other device")
                    }
                writeErrorMsg.data = bundle
                handler.sendMessage(writeErrorMsg)
                return
            }

            // Share the sent message with the UI activity.
            val writtenMsg = handler.obtainMessage(MESSAGE_WRITE, -1, -1, mmBuffer)
            writtenMsg.sendToTarget()
        }

        // Call this method from the main activity to shut down the connection.
        fun cancel(): Boolean {
            return try {
                mmSocket.close()
                true
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
                false
            }
        }
    }
}