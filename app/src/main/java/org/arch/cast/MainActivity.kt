package org.arch.cast

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

    internal val ipAddr = "10.0.0.21"
    internal val tcpPort = 1238
    private val udpPort = 1873

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
    private var sampleRate = 16000
    private var channelConfig = AudioFormat.CHANNEL_IN_STEREO
    private var audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private var minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    internal var buffer = ByteArray(minBufSize)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_DENIED
        )
            ActivityCompat.requestPermissions(
                this,
                Array(1) { Manifest.permission.RECORD_AUDIO },
                0
            )

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
            minBufSize, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE
        )
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
                    val imm =
                        this@MainActivity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(textChannel.windowToken, 0)
                    textChannel.setTextColor(
                        ContextCompat.getColor(
                            this@MainActivity,
                            R.color.colorPrimaryDark
                        )
                    )
                } else
                    textChannel.setTextColor(
                        ContextCompat.getColor(
                            this@MainActivity,
                            R.color.colorAccentDark
                        )
                    )
            }
        })

        buttonTransmit.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                recording = true
                recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    minBufSize * 16
                )
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
                    sleep(delay)
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
                        sleep(delay)
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
