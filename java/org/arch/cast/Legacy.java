/*public class MainActivity extends AppCompatActivity {

    private byte[] buffer;
    private DatagramSocket socket = null;
    private DatagramPacket packet;

    private AudioRecord recorder;
    private int sampleRate = 16000;
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_8BIT;
    int size, minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
    boolean recording = false;

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO},0);

        Button voice = findViewById(R.id.transmit);
        voice.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    new Thread(){
                        public void run(){
                            try{
                                if(socket == null) {
                                    socket = new DatagramSocket();
                                    socket.connect(InetAddress.getByName("10.0.0.14"), 1238);
                                }
                                buffer = new byte[minBufSize];
                                recording = true;
                                recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,sampleRate,channelConfig,audioFormat,minBufSize*16);
                                recorder.startRecording();
                                while(recording) {
                                    size = recorder.read(buffer, 0, buffer.length);
                                    packet = new DatagramPacket(buffer, size);
                                    socket.send(packet);
                                }
                            }
                            catch(SocketException se){
                                Log.e("Socket","Socket Exception!");
                            }
                            catch(UnknownHostException uhe){
                                Log.e("Socket","Unknown Host Exception!");
                            }
                            catch(IOException ioe){
                                Log.e("Socket","IO Exception!");
                            }
                        }
                    }.start();
                }
                else if(event.getAction() == MotionEvent.ACTION_UP)
                {
                    new Thread(){
                        public void run(){
                            recording = false;
                            recorder.release();
                        }
                    }.start();
                }
                return false;
            }
        });
    }
}
        new Thread() {
            @Override
            public void run() {
                while (metaConn) {
                    try {
                        op = queue.poll(Long.MAX_VALUE, TimeUnit.SECONDS);
                        stream.writeInt(op);
                        if (op == 0) {
                            stream.writeInt(queue.poll(Long.MAX_VALUE, TimeUnit.SECONDS));
                            stream.writeInt(queue.poll(Long.MAX_VALUE, TimeUnit.SECONDS));
                        }
                    } catch (Exception exception) {
                        metaConn = false;
                        startActivity(new Intent(MainActivity.this, LaunchActivity.class));
                        MainActivity.this.finish();
                    }
                }
            }
        }.start();
    }

*/