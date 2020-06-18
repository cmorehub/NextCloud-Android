/*
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.ui.preview;

import android.accounts.Account;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.VideoView;

import com.nextcloud.client.media.ErrorFormat;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.BaseActivity;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.ThemeUtils;

import androidx.appcompat.app.AlertDialog;

/**
 *  Activity implementing a basic video player.
 *
 *  Used as an utility to preview video files contained in an ownCloud account.
 *
 *  Currently, it always plays in landscape mode, full screen. When the playback ends,
 *  the activity is finished.
 */
public class PreviewVideoActivity extends BaseActivity implements OnCompletionListener, OnPreparedListener,
    OnErrorListener {

    /** Key to receive a flag signaling if the video should be started immediately */
    public static final String EXTRA_AUTOPLAY = "AUTOPLAY";

    /** Key to receive the position of the playback where the video should be put at start */
    public static final String EXTRA_START_POSITION = "START_POSITION";

    public static final String EXTRA_STREAM_URL = "STREAM_URL";

    private static final String TAG = PreviewVideoActivity.class.getSimpleName();

    private int mSavedPlaybackPosition;         // in the unit time handled by MediaPlayer.getCurrentPosition()
    private boolean mAutoplay;                  // when 'true', the playback starts immediately with the activity
    private VideoView mVideoPlayer;             // view to play the file; both performs and show the playback
    private MediaController mMediaController;   // panel control used by the user to control the playback
    private Uri mStreamUri;
    private OCFile mFile;

    /**
     *  Called when the activity is first created.
     *
     *  Searches for an {@link OCFile} and ownCloud {@link Account} holding it in the starting {@link Intent}.
     *
     *  The {@link Account} is unnecessary if the file is downloaded; else, the {@link Account} is used to
     *  try to stream the remote file - TODO get the streaming works
     *
     *  {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log_OC.v(TAG, "onCreate");

        setContentView(R.layout.video_layout);
        Bundle extras = getIntent().getExtras();
        if (savedInstanceState != null) {
            mFile = savedInstanceState.getParcelable(FileActivity.EXTRA_FILE);
            mSavedPlaybackPosition = savedInstanceState.getInt(EXTRA_START_POSITION);
            mAutoplay = savedInstanceState.getBoolean(EXTRA_AUTOPLAY);
            mStreamUri = (Uri) savedInstanceState.get(EXTRA_STREAM_URL);
        } else if(extras!=null){
            mFile = extras.getParcelable(FileActivity.EXTRA_FILE);
            mSavedPlaybackPosition = extras.getInt(EXTRA_START_POSITION);
            mAutoplay = extras.getBoolean(EXTRA_AUTOPLAY);
            mStreamUri = (Uri) extras.get(EXTRA_STREAM_URL);
        } else{
            finish();
            return;
        }

        mVideoPlayer = findViewById(R.id.videoPlayer);

        // set listeners to get more control on the playback
        mVideoPlayer.setOnPreparedListener(this);
        mVideoPlayer.setOnCompletionListener(this);
        mVideoPlayer.setOnErrorListener(this);

        // keep the screen on while the playback is performed (prevents screen off by battery save)
        mVideoPlayer.setKeepScreenOn(true);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(FileActivity.EXTRA_FILE, mFile);
        outState.putInt(PreviewVideoActivity.EXTRA_START_POSITION, mVideoPlayer.getCurrentPosition());
        outState.putBoolean(PreviewVideoActivity.EXTRA_AUTOPLAY , mVideoPlayer.isPlaying());
        outState.putParcelable(PreviewVideoActivity.EXTRA_STREAM_URL, mStreamUri);
    }


    @Override
    public void onBackPressed() {
        Log_OC.v(TAG, "onBackPressed");
        Intent i = new Intent();
        i.putExtra(EXTRA_AUTOPLAY, mVideoPlayer.isPlaying());
        i.putExtra(EXTRA_START_POSITION, mVideoPlayer.getCurrentPosition());
        setResult(RESULT_OK, i);
        super.onBackPressed();
    }


    /**
     * Called when the file is ready to be played.
     *
     * Just starts the playback.
     *
     * @param   mp    {@link MediaPlayer} instance performing the playback.
     */
    @Override
    public void onPrepared(MediaPlayer mp) {
        Log_OC.v(TAG, "onPrepare");
        mVideoPlayer.seekTo(mSavedPlaybackPosition);
        if (mAutoplay) {
            mVideoPlayer.start();
        }
        mMediaController.show(5000);
        setupMediaController(mMediaController);
    }


    /**
     * Called when the file is finished playing.
     *
     * Rewinds the video
     *
     * @param   mp    {@link MediaPlayer} instance performing the playback.
     */
    @Override
    public void onCompletion(MediaPlayer  mp) {
        mVideoPlayer.seekTo(0);
    }


    /**
     * Called when an error in playback occurs.
     *
     * @param   mp      {@link MediaPlayer} instance performing the playback.
     * @param   what    Type of error
     * @param   extra   Extra code specific to the error
     */
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log_OC.e(TAG, "Error in video playback, what = " + what + ", extra = " + extra);

        if (mMediaController != null) {
            mMediaController.hide();
        }

        if (mVideoPlayer.getWindowToken() != null) {
            String message = ErrorFormat.toString(this, what, extra);
            new AlertDialog.Builder(this)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.VideoView_error_button,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    PreviewVideoActivity.this.onCompletion(null);
                                }
                            })
                    .setCancelable(false)
                    .show();
        }
        return true;
    }

    private float calculateViewHighlightScale(View view){
        return 1.3f;
    }

    private View.OnFocusChangeListener buttonHighlightListener = new View.OnFocusChangeListener(){
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            float scale = hasFocus?calculateViewHighlightScale(v):1.0f;
            v.setScaleX(scale);
            v.setScaleY(scale);
        }
    };

    private int getSeekBarHighlightColor(){
        return Color.GRAY;
    }

    private View.OnFocusChangeListener seekBarHighlightListener = new View.OnFocusChangeListener(){
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if(hasFocus){
                ThemeUtils.colorHorizontalProgressBar((ProgressBar) v, getSeekBarHighlightColor());
            } else if(v instanceof SeekBar){
                ThemeUtils.colorHorizontalSeekBar((SeekBar)v,PreviewVideoActivity.this);
            } else if(v instanceof ProgressBar){
                ThemeUtils.colorHorizontalProgressBar((ProgressBar) v,
                                                      ThemeUtils.primaryAccentColor(PreviewVideoActivity.this));
            }
        }
    };

    /**
     * Add onFocus Effect on MediaController.
     * Seems should be ran after onPrepared or won't made effect.
     * @param controller The MediaController to setup.
     */
    private void setupMediaController(MediaController controller){
        LinearLayout mediaControllerButtonLayout =
            (LinearLayout) ((LinearLayout)controller.getChildAt(0)).getChildAt(0);
        LinearLayout mediaControllerTimeBarLayout =
            (LinearLayout) ((LinearLayout)controller.getChildAt(0)).getChildAt(1);

        for(int i = 0; i<mediaControllerTimeBarLayout.getChildCount(); i++){
            View child = mediaControllerTimeBarLayout.getChildAt(i);
            if(child instanceof SeekBar){
                child.setOnFocusChangeListener(seekBarHighlightListener);
            }
        }

        for(int i = 0; i<mediaControllerButtonLayout.getChildCount(); i++){
            View child = mediaControllerButtonLayout.getChildAt(i);
            if(child instanceof ImageButton){
                child.setOnFocusChangeListener(buttonHighlightListener);
            }
        }
    }

    /**
     * Getter for the main {@link OCFile} handled by the activity.
     *
     * @return  Main {@link OCFile} handled by the activity.
     */
    public OCFile getFile() {
        return mFile;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (getAccount() != null) {
            OCFile file = getFile();
            /// Validate handled file  (first image to preview)
            if (file == null) {
                throw new IllegalStateException("Instanced with a NULL OCFile");
            }
            if (!MimeTypeUtil.isVideo(file)) {
                throw new IllegalArgumentException("Non-video file passed as argument");
            }
            file = getStorageManager().getFileById(file.getFileId());
            if (file != null) {
                if (file.isDown()) {
                    mVideoPlayer.setVideoURI(file.getStorageUri());
                } else {
                    mVideoPlayer.setVideoURI(mStreamUri);
                }

                // create and prepare control panel for the user
                mMediaController = new MediaController(this);
                mMediaController.setMediaPlayer(mVideoPlayer);
                mMediaController.setAnchorView(mVideoPlayer);
                mVideoPlayer.setMediaController(mMediaController);

            } else {
                finish();
            }
        } else {
            finish();
        }
   }


}
