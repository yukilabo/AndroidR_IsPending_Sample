package com.example.myapplication

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions

@RuntimePermissions
class FirstFragment : Fragment() {
    private lateinit var recorder: MediaRecorder

    private var uri: Uri? = null
    private lateinit var messageTextView: TextView
    private lateinit var button: Button
    private var isRecording = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        messageTextView = view.findViewById(R.id.textview_first)
        button = view.findViewById(R.id.button_first)
        button.setOnClickListener {
            if (isRecording) {
                stopVoiceRecorder()
            } else {
                startVoiceRecorderWithPermissionCheck()
            }
        }
    }

    @NeedsPermission(Manifest.permission.RECORD_AUDIO)
    fun startVoiceRecorder() {
        recorder = MediaRecorder().also {
            it.setAudioSource(MediaRecorder.AudioSource.MIC)
            it.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            it.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            it.setAudioEncodingBitRate(64000) // 64 kbps
            it.setAudioSamplingRate(44100) // 44,100 hz
        }

        getVoiceRecorderPendingUri(requireContext())?.let { uri ->
            requireContext().contentResolver.openFileDescriptor(uri, "w")?.let { file ->
                recorder.setOutputFile(file.fileDescriptor)
                this.uri = uri
            }
        }
        recorder.prepare()
        recorder.start()

        messageTextView.text = "Recording"
        button.text = "Stop"

        isRecording = true
    }

    private fun stopVoiceRecorder() {
        recorder.stop()
        recorder.reset()
        recorder.release()

        messageTextView.text = ""
        button.text = "Start"

        uri?.let {
            finishPendingUri(requireContext(), it)
            if (isExists(requireContext(), it)) {
                Toast.makeText(context, "Recerded", Toast.LENGTH_SHORT).show()
            }
            finishPendingUri(requireContext(), it)
        }

        isRecording = false
    }

    private fun getVoiceRecorderPendingUri(context: Context): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "voice.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/")
            put(MediaStore.Video.Media.IS_PENDING, 1)
            put(
                MediaStore.MediaColumns.DATE_EXPIRES,
                (System.currentTimeMillis() + DateUtils.DAY_IN_MILLIS) / 1000
            )
        }

        return context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
    }

    private fun finishPendingUri(context: Context, uri: Uri) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 0)
            putNull(MediaStore.MediaColumns.DATE_EXPIRES)
        }

        context.contentResolver.update(uri, values, null, null)
    }

    private fun isExists(context: Context, uri: Uri): Boolean {
        context.contentResolver.query(
            uri, null, null, null, null
        ).use {
            return it != null && it.count > 0
        }
    }
}