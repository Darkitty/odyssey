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

package org.gateshipone.odyssey.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Slide;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.fragments.AlbumTracksFragment;
import org.gateshipone.odyssey.fragments.ArtistAlbumsFragment;
import org.gateshipone.odyssey.fragments.ArtworkSettingsFragment;
import org.gateshipone.odyssey.fragments.BookmarksFragment;
import org.gateshipone.odyssey.fragments.FilesFragment;
import org.gateshipone.odyssey.fragments.MyMusicFragment;
import org.gateshipone.odyssey.fragments.OdysseyFragment;
import org.gateshipone.odyssey.fragments.PlaylistTracksFragment;
import org.gateshipone.odyssey.dialogs.SaveDialog;
import org.gateshipone.odyssey.fragments.SavedPlaylistsFragment;
import org.gateshipone.odyssey.fragments.SettingsFragment;
import org.gateshipone.odyssey.listener.OnAlbumSelectedListener;
import org.gateshipone.odyssey.listener.OnArtistSelectedListener;
import org.gateshipone.odyssey.listener.OnDirectorySelectedListener;
import org.gateshipone.odyssey.listener.OnPlaylistSelectedListener;
import org.gateshipone.odyssey.listener.OnSaveDialogListener;
import org.gateshipone.odyssey.listener.ToolbarAndFABCallback;
import org.gateshipone.odyssey.playbackservice.PlaybackServiceConnection;
import org.gateshipone.odyssey.playbackservice.managers.PlaybackServiceStatusHelper;
import org.gateshipone.odyssey.utils.FileExplorerHelper;
import org.gateshipone.odyssey.utils.MusicLibraryHelper;
import org.gateshipone.odyssey.utils.PermissionHelper;
import org.gateshipone.odyssey.utils.ThemeUtils;
import org.gateshipone.odyssey.views.CurrentPlaylistView;
import org.gateshipone.odyssey.views.NowPlayingView;

import java.util.ArrayList;
import java.util.List;

public class OdysseyMainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnArtistSelectedListener, OnAlbumSelectedListener, OnPlaylistSelectedListener, OnSaveDialogListener,
        OnDirectorySelectedListener, NowPlayingView.NowPlayingDragStatusReceiver, SettingsFragment.OnArtworkSettingsRequestedCallback, ToolbarAndFABCallback {

    private ActionBarDrawerToggle mDrawerToggle;

    private DRAG_STATUS mNowPlayingDragStatus;
    private DRAG_STATUS mSavedNowPlayingDragStatus = null;

    private VIEW_SWITCHER_STATUS mNowPlayingViewSwitcherStatus;
    private VIEW_SWITCHER_STATUS mSavedNowPlayingViewSwitcherStatus = null;

    private FileExplorerHelper mFileExplorerHelper = null;

    public final static String MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW = "org.gateshipone.odyssey.requestedview";
    public final static String MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW_NOWPLAYINGVIEW = "org.gateshipone.odyssey.requestedview.nowplaying";

    public final static String MAINACTIVITY_SAVED_INSTANCE_NOW_PLAYING_DRAG_STATUS = "OdysseyMainActivity.NowPlayingDragStatus";
    public final static String MAINACTIVITY_SAVED_INSTANCE_NOW_PLAYING_VIEW_SWITCHER_CURRENT_VIEW = "OdysseyMainActivity.NowPlayingViewSwitcherCurrentView";

    public ProgressDialog mProgressDialog;
    private PBSOperationFinishedReceiver mPBSOperationFinishedReceiver = null;

    private PlaybackServiceConnection mServiceConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // restore drag state
        if (savedInstanceState != null) {
            mSavedNowPlayingDragStatus = DRAG_STATUS.values()[savedInstanceState.getInt(MAINACTIVITY_SAVED_INSTANCE_NOW_PLAYING_DRAG_STATUS)];
            mSavedNowPlayingViewSwitcherStatus = VIEW_SWITCHER_STATUS.values()[savedInstanceState.getInt(MAINACTIVITY_SAVED_INSTANCE_NOW_PLAYING_VIEW_SWITCHER_CURRENT_VIEW)];
        }

        // Read theme preference
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String themePref = sharedPref.getString(getString(R.string.pref_theme_key), getString(R.string.pref_theme_default));
        boolean darkTheme = sharedPref.getBoolean(getString(R.string.pref_dark_theme_key), getResources().getBoolean(R.bool.pref_theme_dark_default));
        if (darkTheme) {
            if (themePref.equals(getString(R.string.pref_indigo_key))) {
                setTheme(R.style.AppTheme_indigo);
            } else if (themePref.equals(getString(R.string.pref_orange_key))) {
                setTheme(R.style.AppTheme_orange);
            } else if (themePref.equals(getString(R.string.pref_deeporange_key))) {
                setTheme(R.style.AppTheme_deepOrange);
            } else if (themePref.equals(getString(R.string.pref_blue_key))) {
                setTheme(R.style.AppTheme_blue);
            } else if (themePref.equals(getString(R.string.pref_darkgrey_key))) {
                setTheme(R.style.AppTheme_darkGrey);
            } else if (themePref.equals(getString(R.string.pref_brown_key))) {
                setTheme(R.style.AppTheme_brown);
            } else if (themePref.equals(getString(R.string.pref_lightgreen_key))) {
                setTheme(R.style.AppTheme_lightGreen);
            } else if (themePref.equals(getString(R.string.pref_red_key))) {
                setTheme(R.style.AppTheme_red);
            }
        } else {
            if (themePref.equals(getString(R.string.pref_indigo_key))) {
                setTheme(R.style.AppTheme_indigo_light);
            } else if (themePref.equals(getString(R.string.pref_orange_key))) {
                setTheme(R.style.AppTheme_orange_light);
            } else if (themePref.equals(getString(R.string.pref_deeporange_key))) {
                setTheme(R.style.AppTheme_deepOrange_light);
            } else if (themePref.equals(getString(R.string.pref_blue_key))) {
                setTheme(R.style.AppTheme_blue_light);
            } else if (themePref.equals(getString(R.string.pref_darkgrey_key))) {
                setTheme(R.style.AppTheme_darkGrey_light);
            } else if (themePref.equals(getString(R.string.pref_brown_key))) {
                setTheme(R.style.AppTheme_brown_light);
            } else if (themePref.equals(getString(R.string.pref_lightgreen_key))) {
                setTheme(R.style.AppTheme_lightGreen_light);
            } else if (themePref.equals(getString(R.string.pref_red_key))) {
                setTheme(R.style.AppTheme_red_light);
            }
        }
        if (themePref.equals(getString(R.string.pref_oleddark_key))) {
            setTheme(R.style.AppTheme_oledDark);
        }

        // Read default view preference
        String defaultView = sharedPref.getString(getString(R.string.pref_start_view_key), getString(R.string.pref_view_default));

        // the default tab for mymusic
        MyMusicFragment.DEFAULTTAB defaultTab = MyMusicFragment.DEFAULTTAB.ALBUMS;
        // the nav ressource id to mark the right item in the nav drawer
        int navId = -1;

        if (defaultView.equals(getString(R.string.pref_view_my_music_artists_key))) {
            navId = R.id.nav_my_music;
            defaultTab = MyMusicFragment.DEFAULTTAB.ARTISTS;
        } else if (defaultView.equals(getString(R.string.pref_view_my_music_albums_key))) {
            navId = R.id.nav_my_music;
            defaultTab = MyMusicFragment.DEFAULTTAB.ALBUMS;
        } else if (defaultView.equals(getString(R.string.pref_view_my_music_tracks_key))) {
            defaultTab = MyMusicFragment.DEFAULTTAB.TRACKS;
        } else if (defaultView.equals(getString(R.string.pref_view_playlists_key))) {
            navId = R.id.nav_saved_playlists;
        } else if (defaultView.equals(getString(R.string.pref_view_bookmarks_key))) {
            navId = R.id.nav_bookmarks;
        } else if (defaultView.equals(getString(R.string.pref_view_files_key))) {
            navId = R.id.nav_files;
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_odyssey_main);

        // restore elevation behaviour as pre 24 support lib
        AppBarLayout layout = (AppBarLayout) findViewById(R.id.appbar);
        layout.setStateListAnimator(null);
        ViewCompat.setElevation(layout, 0);

        // get fileexplorerhelper
        mFileExplorerHelper = FileExplorerHelper.getInstance();

        // setup progressdialog
        mProgressDialog = new ProgressDialog(OdysseyMainActivity.this);
        mProgressDialog.setMessage(getString(R.string.playbackservice_working));
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setIndeterminate(true);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // enable back navigation
        final android.support.v7.app.ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null) {
            mDrawerToggle = new ActionBarDrawerToggle(this, drawer, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.addDrawerListener(mDrawerToggle);
            mDrawerToggle.syncState();
        }

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
            navigationView.setCheckedItem(navId);
        }

        // register context menu for currentPlaylistListView
        ListView currentPlaylistListView = (ListView) findViewById(R.id.list_linear_listview);
        registerForContextMenu(currentPlaylistListView);

        if (findViewById(R.id.fragment_container) != null) {
            if (savedInstanceState != null) {
                return;
            }

            Fragment fragment;

            switch (navId) {
                case R.id.nav_my_music:
                    fragment = new MyMusicFragment();

                    Bundle args = new Bundle();
                    args.putInt(MyMusicFragment.MY_MUSIC_REQUESTED_TAB, defaultTab.ordinal());

                    fragment.setArguments(args);
                    break;
                case R.id.nav_saved_playlists:
                    fragment = new SavedPlaylistsFragment();
                    break;
                case R.id.nav_bookmarks:
                    fragment = new BookmarksFragment();
                    break;
                case R.id.nav_files:
                    fragment = new FilesFragment();

                    // open the default directory
                    List<String> storageVolumesList = mFileExplorerHelper.getStorageVolumes(getApplicationContext());

                    String defaultDirectory = "/";

                    if (!storageVolumesList.isEmpty()) {
                        // choose the latest used storage volume as default
                        defaultDirectory = sharedPref.getString(getString(R.string.pref_file_browser_root_dir_key), storageVolumesList.get(0));
                    }

                    args = new Bundle();
                    args.putString(FilesFragment.ARG_DIRECTORYPATH, defaultDirectory);
                    args.putBoolean(FilesFragment.ARG_ISROOTDIRECTORY, true);

                    fragment.setArguments(args);
                    break;
                default:
                    fragment = new MyMusicFragment();

                    args = new Bundle();
                    args.putInt(MyMusicFragment.MY_MUSIC_REQUESTED_TAB, defaultTab.ordinal());

                    fragment.setArguments(args);
            }

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.commit();
        }

        // ask for permissions
        requestPermissionExternalStorage();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mPBSOperationFinishedReceiver != null) {
            unregisterReceiver(mPBSOperationFinishedReceiver);
            mPBSOperationFinishedReceiver = null;
        }
        mPBSOperationFinishedReceiver = new PBSOperationFinishedReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(PlaybackServiceStatusHelper.MESSAGE_IDLE);
        filter.addAction(PlaybackServiceStatusHelper.MESSAGE_WORKING);
        registerReceiver(mPBSOperationFinishedReceiver, filter);

        // if progress dialog is still active close it
        if (mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }

        NowPlayingView nowPlayingView = (NowPlayingView) findViewById(R.id.now_playing_layout);
        if (nowPlayingView != null) {
            nowPlayingView.registerDragStatusReceiver(this);

            // ask for permissions
            requestPermissionExternalStorage();

            /*
             * Check if the activity got an extra in its intend to show the nowplayingview directly.
             * If yes then pre set the dragoffset of the draggable helper.
             */
            Intent resumeIntent = getIntent();
            if (resumeIntent != null && resumeIntent.getExtras() != null && resumeIntent.getExtras().getString(MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW) != null &&
                    resumeIntent.getExtras().getString(MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW).equals(MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW_NOWPLAYINGVIEW)) {
                nowPlayingView.setDragOffset(0.0f);
                getIntent().removeExtra(MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW);
            } else {
                // set drag status
                if (mSavedNowPlayingDragStatus == DRAG_STATUS.DRAGGED_UP) {
                    nowPlayingView.setDragOffset(0.0f);
                } else if (mSavedNowPlayingDragStatus == DRAG_STATUS.DRAGGED_DOWN) {
                    nowPlayingView.setDragOffset(1.0f);
                }
                mSavedNowPlayingDragStatus = null;

                // set view switcher status
                if (mSavedNowPlayingViewSwitcherStatus != null) {
                    nowPlayingView.setViewSwitcherStatus(mSavedNowPlayingViewSwitcherStatus);
                    mNowPlayingViewSwitcherStatus = mSavedNowPlayingViewSwitcherStatus;
                }
                mSavedNowPlayingViewSwitcherStatus = null;
            }
            nowPlayingView.onResume();
        }

        mServiceConnection = new PlaybackServiceConnection(getApplicationContext());
        mServiceConnection.openConnection();
    }

    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        // save drag status of the nowplayingview
        savedInstanceState.putInt(MAINACTIVITY_SAVED_INSTANCE_NOW_PLAYING_DRAG_STATUS, mNowPlayingDragStatus.ordinal());

        // save the cover/playlist view status of the nowplayingview
        savedInstanceState.putInt(MAINACTIVITY_SAVED_INSTANCE_NOW_PLAYING_VIEW_SWITCHER_CURRENT_VIEW, mNowPlayingViewSwitcherStatus.ordinal());
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mPBSOperationFinishedReceiver != null) {
            unregisterReceiver(mPBSOperationFinishedReceiver);
            mPBSOperationFinishedReceiver = null;
        }

        NowPlayingView nowPlayingView = (NowPlayingView) findViewById(R.id.now_playing_layout);
        if (nowPlayingView != null) {
            nowPlayingView.registerDragStatusReceiver(null);

            nowPlayingView.onPause();
        }
    }

    @Override
    public void onBackPressed() {

        FragmentManager fragmentManager = getSupportFragmentManager();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (mNowPlayingDragStatus == DRAG_STATUS.DRAGGED_UP) {
            NowPlayingView nowPlayingView = (NowPlayingView) findViewById(R.id.now_playing_layout);
            if (nowPlayingView != null) {
                View coordinatorLayout = findViewById(R.id.main_coordinator_layout);
                coordinatorLayout.setVisibility(View.VISIBLE);
                nowPlayingView.minimize();
            }
        } else if (fragmentManager.findFragmentById(R.id.fragment_container) instanceof SettingsFragment || fragmentManager.findFragmentById(R.id.fragment_container) instanceof SavedPlaylistsFragment) {
            // If current fragment is the settings or savedplaylists fragment, jump back to myMusicFragment.
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_container, new MyMusicFragment());
            transaction.commit();

            // Reset the navigation view
            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            if (navigationView != null) {
                navigationView.setCheckedItem(R.id.nav_my_music);
            }
        } else {
            super.onBackPressed();

            // enable navigation bar when backstack empty
            if (fragmentManager.getBackStackEntryCount() == 0) {
                mDrawerToggle.setDrawerIndicatorEnabled(true);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        FragmentManager fragmentManager = getSupportFragmentManager();

        switch (item.getItemId()) {
            case android.R.id.home:
                if (fragmentManager.getBackStackEntryCount() > 0) {
                    onBackPressed();
                } else {
                    // back stack empty so enable navigation drawer

                    mDrawerToggle.setDrawerIndicatorEnabled(true);

                    if (mDrawerToggle.onOptionsItemSelected(item)) {
                        return true;
                    }
                }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        if (v.getId() == R.id.list_linear_listview && mNowPlayingDragStatus == DRAG_STATUS.DRAGGED_UP) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.context_menu_current_playlist, menu);

            try {
                if (mServiceConnection.getPBS().getCurrentIndex() == ((AdapterView.AdapterContextMenuInfo) menuInfo).position) {
                    menu.findItem(R.id.view_current_playlist_action_playnext).setVisible(false);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        CurrentPlaylistView currentPlaylistView = (CurrentPlaylistView) findViewById(R.id.now_playing_playlist);

        if (currentPlaylistView != null && mNowPlayingDragStatus == DRAG_STATUS.DRAGGED_UP) {
            switch (item.getItemId()) {
                case R.id.view_current_playlist_action_playnext:
                    currentPlaylistView.enqueueTrackAsNext(info.position);
                    return true;
                case R.id.view_current_playlist_action_remove:
                    currentPlaylistView.removeTrack(info.position);
                    return true;
                case R.id.view_current_playlist_action_showalbum:
                    String albumKey = currentPlaylistView.getAlbumKey(info.position);
                    ArrayList<String> albumInformations = MusicLibraryHelper.getAlbumInformationFromKey(albumKey, this);
                    if (albumInformations.size() == 3) {
                        View coordinatorLayout = findViewById(R.id.main_coordinator_layout);
                        coordinatorLayout.setVisibility(View.VISIBLE);

                        NowPlayingView nowPlayingView = (NowPlayingView) findViewById(R.id.now_playing_layout);
                        if (nowPlayingView != null) {
                            nowPlayingView.minimize();
                        }
                        onAlbumSelected(albumKey, albumInformations.get(0), albumInformations.get(1), albumInformations.get(2));
                    }
                    return true;
                case R.id.view_current_playlist_action_showartist:
                    String artistTitle = currentPlaylistView.getArtistTitle(info.position);
                    long artistID = MusicLibraryHelper.getArtistIDFromName(artistTitle, this);

                    View coordinatorLayout = findViewById(R.id.main_coordinator_layout);
                    coordinatorLayout.setVisibility(View.VISIBLE);

                    NowPlayingView nowPlayingView = (NowPlayingView) findViewById(R.id.now_playing_layout);
                    if (nowPlayingView != null) {
                        nowPlayingView.minimize();
                    }
                    onArtistSelected(artistTitle, artistID);
                    return true;
                default:
                    return super.onContextItemSelected(item);
            }
        }

        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        View coordinatorLayout = findViewById(R.id.main_coordinator_layout);
        coordinatorLayout.setVisibility(View.VISIBLE);

        NowPlayingView nowPlayingView = (NowPlayingView) findViewById(R.id.now_playing_layout);
        if (nowPlayingView != null) {
            nowPlayingView.minimize();
        }

        FragmentManager fragmentManager = getSupportFragmentManager();

        // clear backstack
        fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        Fragment fragment = null;

        if (id == R.id.nav_my_music) {
            fragment = new MyMusicFragment();
        } else if (id == R.id.nav_saved_playlists) {
            fragment = new SavedPlaylistsFragment();
        } else if (id == R.id.nav_bookmarks) {
            fragment = new BookmarksFragment();
        } else if (id == R.id.nav_files) {
            fragment = new FilesFragment();

            // open the default directory
            List<String> storageVolumesList = mFileExplorerHelper.getStorageVolumes(getApplicationContext());

            String defaultDirectory = "/";

            if (!storageVolumesList.isEmpty()) {
                // choose the latest used storage volume as default
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                defaultDirectory = sharedPref.getString(getString(R.string.pref_file_browser_root_dir_key), storageVolumesList.get(0));
            }

            Bundle args = new Bundle();
            args.putString(FilesFragment.ARG_DIRECTORYPATH, defaultDirectory);
            args.putBoolean(FilesFragment.ARG_ISROOTDIRECTORY, true);

            fragment.setArguments(args);

        } else if (id == R.id.nav_settings) {
            fragment = new SettingsFragment();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null) {
            drawer.closeDrawer(GravityCompat.START);
        }

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();

        return true;
    }

    @Override
    public void onArtistSelected(String artist, long artistID) {
        // Create fragment and give it an argument for the selected article
        ArtistAlbumsFragment newFragment = new ArtistAlbumsFragment();
        Bundle args = new Bundle();
        args.putString(ArtistAlbumsFragment.ARG_ARTISTNAME, artist);
        args.putLong(ArtistAlbumsFragment.ARG_ARTISTID, artistID);

        newFragment.setArguments(args);

        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // set enter / exit animation
        newFragment.setEnterTransition(new Slide(Gravity.BOTTOM));
        newFragment.setExitTransition(new Slide(Gravity.TOP));

        // Replace whatever is in the fragment_container view with this
        // fragment,
        // and add the transaction to the back stack so the user can navigate
        // back
        transaction.replace(R.id.fragment_container, newFragment);
        transaction.addToBackStack("ArtistFragment");

        // Commit the transaction
        transaction.commit();
    }

    @Override
    public void onAlbumSelected(String albumKey, String albumTitle, String albumArtURL, String artistName) {
        // Create fragment and give it an argument for the selected article
        AlbumTracksFragment newFragment = new AlbumTracksFragment();
        Bundle args = new Bundle();
        args.putString(AlbumTracksFragment.ARG_ALBUMKEY, albumKey);
        args.putString(AlbumTracksFragment.ARG_ALBUMTITLE, albumTitle);
        args.putString(AlbumTracksFragment.ARG_ALBUMART, albumArtURL);
        args.putString(AlbumTracksFragment.ARG_ALBUMARTIST, artistName);

        newFragment.setArguments(args);

        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // set enter / exit animation
        newFragment.setEnterTransition(new Slide(Gravity.BOTTOM));
        newFragment.setExitTransition(new Slide(Gravity.TOP));

        // Replace whatever is in the fragment_container view with this
        // fragment,
        // and add the transaction to the back stack so the user can navigate
        // back
        transaction.replace(R.id.fragment_container, newFragment);
        transaction.addToBackStack("AlbumTracksFragment");

        // Commit the transaction
        transaction.commit();
    }

    @Override
    public void onDirectorySelected(String dirPath, boolean isRootDirectory) {
        // Create fragment and give it an argument for the selected directory
        FilesFragment newFragment = new FilesFragment();
        Bundle args = new Bundle();
        args.putString(FilesFragment.ARG_DIRECTORYPATH, dirPath);
        args.putBoolean(FilesFragment.ARG_ISROOTDIRECTORY, isRootDirectory);

        newFragment.setArguments(args);

        FragmentManager fragmentManager = getSupportFragmentManager();

        android.support.v4.app.FragmentTransaction transaction = fragmentManager.beginTransaction();

        if (isRootDirectory) {
            // if root directory clear the backstack
            fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

            // save new root directory
            SharedPreferences.Editor sharedPrefEditor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            sharedPrefEditor.putString(getString(R.string.pref_file_browser_root_dir_key), dirPath);
            sharedPrefEditor.apply();
        } else {
            // no root directory so add fragment to the backstack

            // set enter / exit animation
            newFragment.setEnterTransition(new Slide(GravityCompat.getAbsoluteGravity(GravityCompat.START, getResources().getConfiguration().getLayoutDirection())));
            newFragment.setExitTransition(new Slide(GravityCompat.getAbsoluteGravity(GravityCompat.END, getResources().getConfiguration().getLayoutDirection())));

            transaction.addToBackStack("FilesFragment");
        }

        transaction.replace(R.id.fragment_container, newFragment);

        // Commit the transaction
        transaction.commit();
    }

    /**
     * Method to retrieve the height of the statusbar to compensate in non-transparent cases.
     *
     * @return The Dimension of the statusbar. Used to compensate the padding.
     */
    private int getStatusBarHeight() {
        int resHeight = 0;
        int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) {
            resHeight = getResources().getDimensionPixelSize(resId);
        }
        return resHeight;
    }

    private void setToolbarImage(Drawable drawable) {
        ImageView collapsingImage = (ImageView) findViewById(R.id.collapsing_image);
        if (collapsingImage != null) {
            collapsingImage.setImageDrawable(drawable);
        }
    }

    @Override
    public void onStatusChanged(DRAG_STATUS status) {
        mNowPlayingDragStatus = status;
        if (status == DRAG_STATUS.DRAGGED_UP) {
            View coordinatorLayout = findViewById(R.id.main_coordinator_layout);
            /**
             * Use View.GONE instead INVISIBLE to hide view behind NowPlayingView,
             * fixes overlaying Fragments on FragmentTransaction combined with minimizing the NPV in one action
             */
            coordinatorLayout.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onSwitchedViews(VIEW_SWITCHER_STATUS view) {
        mNowPlayingViewSwitcherStatus = view;
    }

    @Override
    public void onStartDrag() {
        View coordinatorLayout = findViewById(R.id.main_coordinator_layout);
        coordinatorLayout.setVisibility(View.VISIBLE);
    }

    /**
     * This method smoothly fades out the alpha value of the statusbar to give
     * a transition if the user pulls up the NowPlayingView.
     *
     * @param pos
     */
    @Override
    public void onDragPositionChanged(float pos) {
        // Get the primary color of the active theme from the helper.
        int newColor = ThemeUtils.getThemeColor(this, R.attr.colorPrimary);

        // Calculate the offset depending on the floating point position (0.0-1.0 of the view)
        // Shift by 24 bit to set it as the A from ARGB and set all remaining 24 bits to 1 to
        int alphaOffset = (((255 - (int) (255.0 * pos)) << 24) | 0xFFFFFF);
        // and with this mask to set the new alpha value.
        newColor &= (alphaOffset);
        getWindow().setStatusBarColor(newColor);
    }

    @Override
    public void onPlaylistSelected(String playlistTitle, long playlistID) {
        // Create fragment and give it an argument for the selected playlist
        PlaylistTracksFragment newFragment = new PlaylistTracksFragment();
        Bundle args = new Bundle();
        args.putString(PlaylistTracksFragment.ARG_PLAYLISTTITLE, playlistTitle);
        args.putLong(PlaylistTracksFragment.ARG_PLAYLISTID, playlistID);

        newFragment.setArguments(args);

        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // set enter / exit animation
        newFragment.setEnterTransition(new Slide(GravityCompat.getAbsoluteGravity(GravityCompat.START, getResources().getConfiguration().getLayoutDirection())));
        newFragment.setExitTransition(new Slide(GravityCompat.getAbsoluteGravity(GravityCompat.END, getResources().getConfiguration().getLayoutDirection())));

        // Replace whatever is in the fragment_container view with this
        // fragment,
        // and add the transaction to the back stack so the user can navigate
        // back
        transaction.replace(R.id.fragment_container, newFragment);
        transaction.addToBackStack("PlaylistTracksFragment");

        // Commit the transaction
        transaction.commit();
    }

    private void requestPermissionExternalStorage() {
        // ask for permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                View layout = findViewById(R.id.drawer_layout);
                if (layout != null) {
                    Snackbar sb = Snackbar.make(layout, R.string.permission_request_snackbar_explanation, Snackbar.LENGTH_INDEFINITE);
                    sb.setAction(R.string.permission_request_snackbar_button, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(OdysseyMainActivity.this,
                                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    PermissionHelper.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                        }
                    });
                    // style the snackbar text
                    TextView sbText = (TextView) sb.getView().findViewById(android.support.design.R.id.snackbar_text);
                    sbText.setTextColor(ThemeUtils.getThemeColor(this, R.attr.odyssey_color_text_accent));
                    sb.show();
                }
            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PermissionHelper.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PermissionHelper.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay!
                    Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

                    if (fragment instanceof MyMusicFragment) {
                        ((MyMusicFragment) fragment).refresh();
                    } else if (fragment instanceof OdysseyFragment) {
                        ((OdysseyFragment) fragment).refreshContent();
                    }
                }
                break;
            }
        }
    }

    @Override
    public void onSaveObject(String title, SaveDialog.OBJECTTYPE type) {
        NowPlayingView nowPlayingView = (NowPlayingView) findViewById(R.id.now_playing_layout);
        if (nowPlayingView != null) {
            // check type to identify which object should be saved
            switch (type) {
                case PLAYLIST:
                    nowPlayingView.savePlaylist(title);
                    break;
                case BOOKMARK:
                    nowPlayingView.createBookmark(title);
                    break;
            }
        }
    }

    @Override
    public void openArtworkSettings() {
        // Create fragment and give it an argument for the selected directory
        ArtworkSettingsFragment newFragment = new ArtworkSettingsFragment();


        FragmentManager fragmentManager = getSupportFragmentManager();

        android.support.v4.app.FragmentTransaction transaction = fragmentManager.beginTransaction();

        // set enter / exit animation
        newFragment.setEnterTransition(new Slide(GravityCompat.getAbsoluteGravity(GravityCompat.START, getResources().getConfiguration().getLayoutDirection())));
        newFragment.setExitTransition(new Slide(GravityCompat.getAbsoluteGravity(GravityCompat.END, getResources().getConfiguration().getLayoutDirection())));

        transaction.addToBackStack("ArtworkSettingsFragment");

        transaction.replace(R.id.fragment_container, newFragment);

        // Commit the transaction
        transaction.commit();
    }

    @Override
    public void setupFAB(View.OnClickListener listener) {
        FloatingActionButton playButton = (FloatingActionButton) findViewById(R.id.odyssey_play_button);

        if (playButton != null) {
            if (listener == null) {
                playButton.hide();
            } else {
                playButton.show();
            }

            playButton.setOnClickListener(listener);
        }
    }

    @Override
    public void setupToolbar(String title, boolean scrollingEnabled, boolean drawerIndicatorEnabled, boolean showImage) {
        // set drawer state
        mDrawerToggle.setDrawerIndicatorEnabled(drawerIndicatorEnabled);

        ImageView collapsingImage = (ImageView) findViewById(R.id.collapsing_image);
        View collapsingImageGradientTop = findViewById(R.id.collapsing_image_gradient_top);
        View collapsingImageGradientBottom = findViewById(R.id.collapsing_image_gradient_bottom);
        if (collapsingImage != null && collapsingImageGradientTop != null && collapsingImageGradientBottom != null) {
            if (showImage) {
                collapsingImage.setVisibility(View.VISIBLE);
                collapsingImageGradientTop.setVisibility(View.VISIBLE);
                collapsingImageGradientBottom.setVisibility(View.VISIBLE);
            } else {
                collapsingImage.setVisibility(View.GONE);
                collapsingImage.setImageDrawable(null);
                collapsingImageGradientTop.setVisibility(View.GONE);
                collapsingImageGradientBottom.setVisibility(View.GONE);
            }
        }
        // set scrolling behaviour
        CollapsingToolbarLayout toolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) toolbar.getLayoutParams();

        if (scrollingEnabled && !showImage) {
            toolbar.setTitleEnabled(false);
            toolbar.setPadding(0, getStatusBarHeight(), 0, 0);
            setTitle(title);

            params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL + AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS_COLLAPSED + AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
        } else if (!scrollingEnabled && showImage && collapsingImage != null) {
            toolbar.setTitleEnabled(true);
            toolbar.setPadding(0, 0, 0, 0);
            toolbar.setTitle(title);

            params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED + AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL);
        } else {
            toolbar.setTitleEnabled(false);
            toolbar.setPadding(0, getStatusBarHeight(), 0, 0);
            setTitle(title);
            params.setScrollFlags(0);
        }
    }

    @Override
    public void setupToolbarImage(Bitmap bm) {
        ImageView collapsingImage = (ImageView) findViewById(R.id.collapsing_image);
        if (collapsingImage != null) {
            collapsingImage.setImageBitmap(bm);
        }
    }

    private class PBSOperationFinishedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PlaybackServiceStatusHelper.MESSAGE_WORKING)) {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        if (mProgressDialog != null) {
                            mProgressDialog.show();
                        }
                    }
                });
            } else if (intent.getAction().equals(PlaybackServiceStatusHelper.MESSAGE_IDLE)) {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        if (mProgressDialog != null) {
                            mProgressDialog.dismiss();
                        }
                    }
                });
            }
        }
    }
}
