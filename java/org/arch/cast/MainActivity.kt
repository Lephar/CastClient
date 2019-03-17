package org.arch.cast

import android.Manifest
import android.content.pm.PackageManager
import android.media.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import kotlinx.android.synthetic.main.activity_main.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue

class MainActivity : AppCompatActivity() {

    //internal val ipAddr = "10.0.0.10"
    //internal val ipAddr = "192.168.43.19"
    internal val ipAddr = "24.133.216.119"
    internal val tcpPort = 1238
    internal val udpPort = 1873

    internal var uuid = ByteArray(16)
    internal var channel = 0
    internal val delay = 2000L
    internal var metaConn = false
    internal var dataConn = false
    internal var recording = false
    internal var metaQueue = LinkedBlockingQueue<Int>()
    internal var datagram: DatagramSocket? = null
    internal var dataOutputQueue = LinkedBlockingQueue<DatagramPacket>()

    internal var track: AudioTrack? = null
    internal var recorder: AudioRecord? = null
    internal var sampleRate = 16000
    internal var channelConfig = AudioFormat.CHANNEL_IN_STEREO
    internal var audioFormat = AudioFormat.ENCODING_PCM_16BIT
    internal var minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    internal var buffer = ByteArray(minBufSize)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(this, Array(1) { Manifest.permission.RECORD_AUDIO }, 0)

        StreamThread().start()
        DataInputThread().start()
        DataOutputThread().start()

        track = AudioTrack(
                AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                AudioFormat.Builder().setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build(),
                minBufSize, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE)
        track?.play()

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (metaConn) {
                    metaQueue.put('c'.toInt())
                    metaQueue.put(channel)
                }
                if (dataConn && !recording)
                    dataOutputQueue.put(createPacket('u'.toInt(), ByteArray(0)))
                handler.postDelayed(this, delay)
            }
        }, delay)

        textChannel.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 4) {
                    channel = s.toString().toInt()
                    metaQueue.put('c'.toInt())
                    metaQueue.put(channel)
                }
            }
        })

        buttonTransmit.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                recording = true
                recorder = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, minBufSize * 16)
                recorder?.startRecording()
            } else if (event.action == MotionEvent.ACTION_UP) {
                recording = false
                recorder?.release()
            }
            dataOutputQueue.put(createPacket('u'.toInt(), ByteArray(0)))
            return@setOnTouchListener false
        }
    }

    fun createPacket(op: Int, data: ByteArray): DatagramPacket {
        val array = ByteArray(24 + data.size)
        val buffer = ByteBuffer.wrap(array)
        buffer.putInt(op).putInt(channel).put(uuid).put(data)
        return DatagramPacket(array, array.size, InetSocketAddress(ipAddr, udpPort))
    }

    internal inner class StreamThread : Thread() {

        override fun run() {
            var stream: Socket
            var input: DataInputStream
            var output: DataOutputStream? = null

            while (true) {
                if (!metaConn) {
                    try {
                        stream = Socket()
                        stream.connect(InetSocketAddress(ipAddr, tcpPort), delay.toInt())
                        input = DataInputStream(stream.getInputStream())
                        output = DataOutputStream(stream.getOutputStream())
                        input.readFully(uuid)
                        metaConn = true

                        runOnUiThread {
                            textChannel.isEnabled = true
                            buttonTransmit.isEnabled = true
                        }
                    } catch (exception: Exception) {
                        invalidate()
                    }
                } else {
                    try {
                        output?.writeInt(metaQueue.take())
                    } catch (exception: Exception) {
                        invalidate()
                    }
                }
            }
        }

        private fun invalidate() {
            metaConn = false
            runOnUiThread {
                textChannel.isEnabled = false
                buttonTransmit.isEnabled = false
            }
        }
    }

    internal inner class DataInputThread : Thread() {
        override fun run() {
            val length = 2000
            val buffer = ByteArray(length)
            val packet = DatagramPacket(buffer, length)

            while (true) {
                if (!dataConn) {
                    Thread.sleep(delay)
                } else {
                    try {
                        datagram?.receive(packet)
                        track?.write(packet.data, 0, packet.data.size)
                    } catch (exception: Exception) {
                        dataConn = false
                    }
                }
            }
        }
    }

    internal inner class DataOutputThread : Thread() {
        override fun run() {
            while (true) {
                if (!dataConn) {
                    try {
                        datagram = DatagramSocket()
                        dataConn = true
                    } catch (exception: Exception) {
                        Thread.sleep(delay)
                    }
                } else if (!recording) {
                    try {
                        datagram?.send(dataOutputQueue.take())
                    } catch (exception: Exception) {
                        dataConn = false
                    }
                } else {
                    try {
                        recorder?.read(buffer, 0, buffer.size)
                        datagram?.send(createPacket('b'.toInt(), buffer))
                    } catch (exception: Exception) {
                        recording = false
                        dataConn = false
                    }
                }
            }
        }
    }
}
