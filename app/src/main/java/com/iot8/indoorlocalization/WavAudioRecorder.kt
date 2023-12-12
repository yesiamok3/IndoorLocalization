import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.FileOutputStream
import java.io.IOException

class WavAudioRecorder(private val context: Context, private val outputFile: String) {
    private val RECORD_AUDIO_PERMISSION = android.Manifest.permission.RECORD_AUDIO
    private val WRITE_EXTERNAL_STORAGE_PERMISSION = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    private val REQUEST_PERMISSION_CODE = 123

    private var outputStream: FileOutputStream? = null

    private val bufferSize: Int = AudioRecord.getMinBufferSize(
        48000,
        AudioFormat.CHANNEL_IN_STEREO,
        AudioFormat.ENCODING_PCM_16BIT
    )
    private var audioRecord: AudioRecord? = null

    init {
        if (!checkPermissions()) {
            requestPermissions()
        }
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.UNPROCESSED,
        48000,
        AudioFormat.CHANNEL_IN_STEREO,
        AudioFormat.ENCODING_PCM_16BIT,
        bufferSize)
    }

    private fun checkPermissions(): Boolean {
        val recordAudioPermission = ContextCompat.checkSelfPermission(context, RECORD_AUDIO_PERMISSION)
        val writeExternalStoragePermission =
            ContextCompat.checkSelfPermission(context, WRITE_EXTERNAL_STORAGE_PERMISSION)

        return recordAudioPermission == PackageManager.PERMISSION_GRANTED &&
                writeExternalStoragePermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            context as androidx.fragment.app.FragmentActivity,
            arrayOf(RECORD_AUDIO_PERMISSION, WRITE_EXTERNAL_STORAGE_PERMISSION),
            REQUEST_PERMISSION_CODE
        )
    }

    fun startRecording() {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.UNPROCESSED,
                48000,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize)
            audioRecord?.startRecording()

            // Open the output stream for writing audio data
            try {
                outputStream = FileOutputStream(outputFile)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            // Start a separate thread for writing audio data to the file
            Thread {
                writeAudioDataToFile()
            }.start()
    }

    private fun writeAudioDataToFile() {
        val buffer = ShortArray(bufferSize / 2) // 16-bit audio, so each element is a short

        while (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            val bytesRead = audioRecord?.read(buffer, 0, buffer.size)
            if (bytesRead != null && bytesRead > 0) {
                try {
                    // Write the audio data to the file
                    outputStream?.write(shortArrayToByteArray(buffer))
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        // Close the output stream when recording is stopped
        try {
            outputStream?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun shortArrayToByteArray(shortArray: ShortArray): ByteArray {
        val byteArray = ByteArray(shortArray.size * 2)
        for (i in shortArray.indices) {
            // Little-endian format (change if needed)
            byteArray[i * 2] = shortArray[i].toByte()
            byteArray[i * 2 + 1] = (shortArray[i].toInt() shr 8).toByte()
        }
        return byteArray
    }

    fun stopRecording() {
        audioRecord?.stop()
        audioRecord?.release()
    }

}
