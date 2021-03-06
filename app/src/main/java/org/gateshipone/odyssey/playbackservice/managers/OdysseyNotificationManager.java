/*
 * Copyright (C) 2016  Hendrik Borghorst & Frederik Luetkes
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.gateshipone.odyssey.playbackservice.managers;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.DrawFilter;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;

import org.gateshipone.odyssey.activities.OdysseyMainActivity;
import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.playbackservice.PlaybackService;

/*
 * This class manages all the notifications from the main playback service.
 */
public class OdysseyNotificationManager {
    // Context needed for various Notification building
    private final Context mContext;

    // PendingIntent ids
    private static final int NOTIFICATION_INTENT_PREVIOUS = 0;
    private static final int NOTIFICATION_INTENT_PLAYPAUSE = 1;
    private static final int NOTIFICATION_INTENT_NEXT = 2;
    private static final int NOTIFICATION_INTENT_QUIT = 3;
    private static final int NOTIFICATION_INTENT_OPENGUI = 4;

    // Just a constant
    private static final int NOTIFICATION_ID = 42;

    // Notification objects
    private final android.app.NotificationManager mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder = null;

    // Notification itself
    private Notification mNotification;

    // Save last track and last image
    private Bitmap mLastBitmap = null;
    private TrackModel mLastTrack = null;

    public OdysseyNotificationManager(Context context) {
        mContext = context;
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /*
     * Creates a android system notification with two different remoteViews. One
     * for the normal layout and one for the big one. Sets the different
     * attributes of the remoteViews and starts a thread for Cover generation.
     */
    public void updateNotification(TrackModel track, PlaybackService.PLAYSTATE state, MediaSessionCompat.Token mediaSessionToken) {
        if (track != null) {
            mNotificationBuilder = new NotificationCompat.Builder(mContext);

            // Open application intent
            Intent contentIntent = new Intent(mContext, OdysseyMainActivity.class);
            contentIntent.putExtra(OdysseyMainActivity.MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW, OdysseyMainActivity.MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW_NOWPLAYINGVIEW);
            contentIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_NO_HISTORY);
            PendingIntent contentPendingIntent = PendingIntent.getActivity(mContext, NOTIFICATION_INTENT_OPENGUI, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mNotificationBuilder.setContentIntent(contentPendingIntent);

            // Set pendingintents
            // Previous song action
            Intent prevIntent = new Intent(PlaybackService.ACTION_PREVIOUS);
            PendingIntent prevPendingIntent = PendingIntent.getBroadcast(mContext, NOTIFICATION_INTENT_PREVIOUS, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Action prevAction = new NotificationCompat.Action.Builder(R.drawable.ic_skip_previous_48dp, "Previous", prevPendingIntent).build();

            // Pause/Play action
            PendingIntent playPauseIntent;
            int playPauseIcon;
            if (state == PlaybackService.PLAYSTATE.PLAYING) {
                Intent pauseIntent = new Intent(PlaybackService.ACTION_PAUSE);
                playPauseIntent = PendingIntent.getBroadcast(mContext, NOTIFICATION_INTENT_PLAYPAUSE, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                playPauseIcon = R.drawable.ic_pause_48dp;
            } else {
                Intent playIntent = new Intent(PlaybackService.ACTION_PLAY);
                playPauseIntent = PendingIntent.getBroadcast(mContext, NOTIFICATION_INTENT_PLAYPAUSE, playIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                playPauseIcon = R.drawable.ic_play_arrow_48dp;
            }
            NotificationCompat.Action playPauseAction = new NotificationCompat.Action.Builder(playPauseIcon, "PlayPause", playPauseIntent).build();

            // Next song action
            Intent nextIntent = new Intent(PlaybackService.ACTION_NEXT);
            PendingIntent nextPendingIntent = PendingIntent.getBroadcast(mContext, NOTIFICATION_INTENT_NEXT, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Action nextAction = new NotificationCompat.Action.Builder(R.drawable.ic_skip_next_48dp, "Next", nextPendingIntent).build();

            // Quit action
            Intent quitIntent = new Intent(PlaybackService.ACTION_QUIT);
            PendingIntent quitPendingIntent = PendingIntent.getBroadcast(mContext, NOTIFICATION_INTENT_QUIT, quitIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mNotificationBuilder.setDeleteIntent(quitPendingIntent);

            mNotificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            mNotificationBuilder.setSmallIcon(R.drawable.odyssey_notification);
            mNotificationBuilder.addAction(prevAction);
            mNotificationBuilder.addAction(playPauseAction);
            mNotificationBuilder.addAction(nextAction);
            NotificationCompat.MediaStyle notificationStyle = new NotificationCompat.MediaStyle();
            notificationStyle.setShowActionsInCompactView(1, 2);
            notificationStyle.setMediaSession(mediaSessionToken);
            mNotificationBuilder.setStyle(notificationStyle);
            mNotificationBuilder.setContentTitle(track.getTrackName());
            mNotificationBuilder.setContentText(track.getTrackArtistName());

            // Remove unnecessary time info
            mNotificationBuilder.setWhen(0);

            // Cover but only if changed
            if (mLastTrack == null || !track.getTrackAlbumKey().equals(mLastTrack.getTrackAlbumKey())) {
                mLastTrack = track;
                mLastBitmap = null;
            }

            // Only set image if an saved one is available
            if (mLastBitmap != null) {
                mNotificationBuilder.setLargeIcon(mLastBitmap);
            } else {
                /**
                 * Create a dummy placeholder image for versions greater android 7 because it
                 * does not automatically show the application icon anymore in mediastyle notifications.
                 */
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Drawable icon = mContext.getDrawable(R.drawable.notification_placeholder_256dp);

                    Bitmap iconBitmap = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(iconBitmap);
                    DrawFilter filter = new PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG, 1);

                    canvas.setDrawFilter(filter);
                    icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    icon.setFilterBitmap(true);


                    icon.draw(canvas);
                    mNotificationBuilder.setLargeIcon(iconBitmap);

                } else {
                    /**
                     * For older android versions set the null icon which will result in a dummy icon
                     * generated from the application icon.
                     */
                    mNotificationBuilder.setLargeIcon(null);

                }
            }

            // Build the notification
            mNotification = mNotificationBuilder.build();

            // Check if run from service and check if playing or pause.
            // Pause notification should be dismissible.
            if (mContext instanceof Service) {
                if (state == PlaybackService.PLAYSTATE.PLAYING) {
                    ((Service) mContext).startForeground(NOTIFICATION_ID, mNotification);
                } else {
                    ((Service) mContext).stopForeground(false);
                }
            }

            // Send the notification away
            mNotificationManager.notify(NOTIFICATION_ID, mNotification);
        }

    }

    /* Removes the Foreground notification */
    public void clearNotification() {
        if (mNotification != null) {
            if (mContext instanceof Service) {
                ((Service) mContext).stopForeground(true);
                mNotificationBuilder.setOngoing(false);
                mNotificationManager.cancel(NOTIFICATION_ID);
            }
            mNotification = null;
            mLastTrack = null;
            mLastBitmap = null;
            mNotificationBuilder = null;
        }
    }

    /*
     * Receives the generated album picture from the main status helper for the
     * notification controls. Sets it and notifies the system that the
     * notification has changed
     */
    public void setNotificationImage(Bitmap bm) {
        // Check if notification exists and set picture
        mLastBitmap = bm;
        if (mNotification != null && bm != null) {
            mNotificationBuilder.setLargeIcon(bm);
            mNotification = mNotificationBuilder.build();
            mNotificationManager.notify(NOTIFICATION_ID, mNotification);
        }
    }
}
