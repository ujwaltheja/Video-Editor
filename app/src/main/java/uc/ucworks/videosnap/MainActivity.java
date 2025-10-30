package uc.ucworks.videosnap;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Button;
import android.view.View;
import android.util.Log;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.os.AsyncTask;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.os.ParcelFileDescriptor;

import java.io.IOException;
import java.nio.ByteBuffer;

// import uc.ucworks.videosnap.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private TextView sampleText;
    private static final String TAG = "VideoEditor";
    private static final int REQUEST_VIDEO_PICK = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    private String selectedVideoPath = null;
    private Uri selectedVideoUriGlobal = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Initialize views
        sampleText = findViewById(R.id.sample_text);

        // Request permissions
        requestPermissions();

        // Set up button click listeners
        Button pickVideoButton = findViewById(R.id.pick_video_button);
        pickVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickVideoFile();
            }
        });

        Button testButton = findViewById(R.id.test_button);
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testVideoProcessing();
            }
        });
    }

    private void testVideoProcessing() {
        // Check if a video file has been selected
        if (selectedVideoPath == null && selectedVideoUriGlobal == null) {
            sampleText.setText("Please select a video file first using the 'Select Video File' button");
            return;
        }

        // Test video info
        getVideoInfo();
    }

    private void getVideoInfo() {
        // Use Android's MediaExtractor to get basic video information
        MediaExtractor extractor = new MediaExtractor();
        try {
            if (selectedVideoPath != null) {
                extractor.setDataSource(selectedVideoPath);
            } else if (selectedVideoUriGlobal != null) {
                try {
                    ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(selectedVideoUriGlobal, "r");
                    if (pfd != null) {
                        extractor.setDataSource(pfd.getFileDescriptor());
                        pfd.close();
                    } else {
                        throw new IOException("Could not open file descriptor for URI");
                    }
                } catch (Exception e) {
                    throw new IOException("Failed to set data source from URI: " + e.getMessage());
                }
            } else {
                runOnUiThread(() -> {
                    sampleText.setText("No video source available");
                    Log.e(TAG, "No video path or URI available");
                });
                return;
            }

            StringBuilder info = new StringBuilder();
            info.append("Video tracks: ").append(extractor.getTrackCount()).append("\n");

            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);

                if (mime != null && mime.startsWith("video/")) {
                    info.append("Video format: ").append(mime).append("\n");
                    if (format.containsKey(MediaFormat.KEY_WIDTH)) {
                        info.append("Width: ").append(format.getInteger(MediaFormat.KEY_WIDTH)).append("\n");
                    }
                    if (format.containsKey(MediaFormat.KEY_HEIGHT)) {
                        info.append("Height: ").append(format.getInteger(MediaFormat.KEY_HEIGHT)).append("\n");
                    }
                    if (format.containsKey(MediaFormat.KEY_DURATION)) {
                        long durationUs = format.getLong(MediaFormat.KEY_DURATION);
                        info.append("Duration: ").append(durationUs / 1000000).append(" seconds\n");
                    }
                }
            }

            runOnUiThread(() -> {
                sampleText.setText("Video Info:\n" + info.toString());
                Log.d(TAG, "Video info retrieved successfully");
            });

        } catch (IOException e) {
            runOnUiThread(() -> {
                sampleText.setText("Failed to get video info: " + e.getMessage());
                Log.e(TAG, "Failed to get video info", e);
            });
        } finally {
            extractor.release();
        }
    }

    private void trimVideo(String inputPath, String outputPath, double startTime, double duration) {
        // Use Android's MediaExtractor and MediaMuxer for basic video trimming
        AsyncTask.execute(() -> {
            MediaExtractor extractor = new MediaExtractor();
            MediaMuxer muxer = null;

            try {
                extractor.setDataSource(inputPath);

                // Find video track
                int videoTrackIndex = -1;
                MediaFormat videoFormat = null;
                for (int i = 0; i < extractor.getTrackCount(); i++) {
                    MediaFormat format = extractor.getTrackFormat(i);
                    String mime = format.getString(MediaFormat.KEY_MIME);
                    if (mime != null && mime.startsWith("video/")) {
                        videoTrackIndex = i;
                        videoFormat = format;
                        break;
                    }
                }

                if (videoTrackIndex == -1) {
                    runOnUiThread(() -> {
                        sampleText.setText("No video track found");
                        Log.e(TAG, "No video track found in input file");
                    });
                    return;
                }

                extractor.selectTrack(videoTrackIndex);

                // Create muxer
                muxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                int muxerVideoTrackIndex = muxer.addTrack(videoFormat);
                muxer.start();

                // Seek to start time
                long startTimeUs = (long)(startTime * 1000000);
                long endTimeUs = (long)((startTime + duration) * 1000000);
                extractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC);

                // Copy video data
                ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

                boolean done = false;
                while (!done) {
                    int sampleSize = extractor.readSampleData(buffer, 0);
                    if (sampleSize < 0) {
                        done = true;
                        break;
                    }

                    long sampleTime = extractor.getSampleTime();
                    if (sampleTime > endTimeUs) {
                        done = true;
                        break;
                    }

                    info.offset = 0;
                    info.size = sampleSize;
                    info.presentationTimeUs = sampleTime - startTimeUs;

                    // Map MediaExtractor flags to MediaCodec flags
                    int extractorFlags = extractor.getSampleFlags();
                    int bufferFlags = 0;
                    if ((extractorFlags & MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                        bufferFlags |= MediaCodec.BUFFER_FLAG_KEY_FRAME;
                    }
                    info.flags = bufferFlags;

                    muxer.writeSampleData(muxerVideoTrackIndex, buffer, info);
                    extractor.advance();
                }

                runOnUiThread(() -> {
                    sampleText.setText("Video trimmed successfully!");
                    Log.d(TAG, "Video trimming completed");
                });

            } catch (IOException e) {
                runOnUiThread(() -> {
                    sampleText.setText("Failed to trim video: " + e.getMessage());
                    Log.e(TAG, "Video trimming failed", e);
                });
            } finally {
                if (muxer != null) {
                    try {
                        muxer.stop();
                        muxer.release();
                    } catch (Exception e) {
                        Log.e(TAG, "Error releasing muxer", e);
                    }
                }
                extractor.release();
            }
        });
    }

    private void extractAudio(String inputPath, String outputPath) {
        // Use Android's MediaExtractor and MediaMuxer for basic audio extraction
        AsyncTask.execute(() -> {
            MediaExtractor extractor = new MediaExtractor();
            MediaMuxer muxer = null;

            try {
                extractor.setDataSource(inputPath);

                // Find audio track
                int audioTrackIndex = -1;
                MediaFormat audioFormat = null;
                for (int i = 0; i < extractor.getTrackCount(); i++) {
                    MediaFormat format = extractor.getTrackFormat(i);
                    String mime = format.getString(MediaFormat.KEY_MIME);
                    if (mime != null && mime.startsWith("audio/")) {
                        audioTrackIndex = i;
                        audioFormat = format;
                        break;
                    }
                }

                if (audioTrackIndex == -1) {
                    runOnUiThread(() -> {
                        sampleText.setText("No audio track found");
                        Log.e(TAG, "No audio track found in input file");
                    });
                    return;
                }

                extractor.selectTrack(audioTrackIndex);

                // Create muxer for audio-only output
                muxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                int muxerAudioTrackIndex = muxer.addTrack(audioFormat);
                muxer.start();

                // Copy audio data
                ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

                boolean done = false;
                while (!done) {
                    int sampleSize = extractor.readSampleData(buffer, 0);
                    if (sampleSize < 0) {
                        done = true;
                        break;
                    }

                    info.offset = 0;
                    info.size = sampleSize;
                    info.presentationTimeUs = extractor.getSampleTime();

                    // Map MediaExtractor flags to MediaCodec flags
                    int extractorFlags = extractor.getSampleFlags();
                    int bufferFlags = 0;
                    if ((extractorFlags & MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                        bufferFlags |= MediaCodec.BUFFER_FLAG_KEY_FRAME;
                    }
                    info.flags = bufferFlags;

                    muxer.writeSampleData(muxerAudioTrackIndex, buffer, info);
                    extractor.advance();
                }

                runOnUiThread(() -> {
                    sampleText.setText("Audio extracted successfully!");
                    Log.d(TAG, "Audio extraction completed");
                });

            } catch (IOException e) {
                runOnUiThread(() -> {
                    sampleText.setText("Failed to extract audio: " + e.getMessage());
                    Log.e(TAG, "Audio extraction failed", e);
                });
            } finally {
                if (muxer != null) {
                    try {
                        muxer.stop();
                        muxer.release();
                    } catch (Exception e) {
                        Log.e(TAG, "Error releasing muxer", e);
                    }
                }
                extractor.release();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                sampleText.setText("Permissions denied. App may not work properly.");
            }
        }
    }

    private void requestPermissions() {
        String[] permissions = {
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS);
        }
    }

    private void pickVideoFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        startActivityForResult(intent, REQUEST_VIDEO_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_VIDEO_PICK && resultCode == RESULT_OK && data != null) {
            Uri selectedVideoUri = data.getData();
            if (selectedVideoUri != null) {
                selectedVideoUriGlobal = selectedVideoUri;
                selectedVideoPath = getRealPathFromURI(selectedVideoUri);
                if (selectedVideoPath != null) {
                    sampleText.setText("Selected video: " + selectedVideoPath);
                } else {
                    // Fallback: use URI directly for newer Android versions
                    sampleText.setText("Selected video: " + selectedVideoUri.toString());
                    Log.d(TAG, "Using URI directly: " + selectedVideoUri.toString());
                }
            }
        }
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] projection = { MediaStore.Video.Media.DATA };
        android.database.Cursor cursor = getContentResolver().query(contentUri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            cursor.close();
            return path;
        }
        return null;
    }
}