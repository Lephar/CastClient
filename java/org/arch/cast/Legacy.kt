/*package org.arch.cast

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.LinkedBlockingQueue

class MainActivity : AppCompatActivity() {

    internal val ipAddr = "10.0.0.11"
    internal val tcpPort = 1238
    internal val udpPort = 1873

    internal var uuid = ByteArray(16)
    internal var channel = 0
    internal var metaConn = false
    internal var metaInit = false
    internal var dataConn = false
    internal var metaQueue = LinkedBlockingQueue<Int>()
    internal var dataInputQueue = LinkedBlockingQueue<DatagramPacket>()
    internal var dataOutputQueue = LinkedBlockingQueue<DatagramPacket>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        StreamThread().start()
    }

    internal inner class StreamThread : Thread() {
        override fun run() {
            var stream : Socket
            var input : DataInputStream
            var output : DataOutputStream

            while (!metaConn) {
                try {
                    stream = Socket()
                    stream.connect(InetSocketAddress(ipAddr, tcpPort), 2000)
                    input = DataInputStream(stream.getInputStream())
                    output = DataOutputStream(stream.getOutputStream())
                    metaConn = true

                    println("Connected!")
                    runOnUiThread {
                        textChannel.isEnabled = true
                    }
                } catch (exception: Exception) {
                    println("Failed!")
                }
            }
        }
    }

    internal inner class DataInputThread : Thread() {
        override fun run() {}
    }

    internal inner class DataOutputThread : Thread() {
        override fun run() {}
    }
}
*/