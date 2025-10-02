# musebox
MuseBox - an android music app based on Java

# Project Structure

app/\
 └── java/\
      └── com.example.mediaplayer/\
           ├── activities/          → Activities (MainActivity, PlayerActivity)\
           ├── adapters/            → RecyclerView/ListView adapters\
           ├── fragments/           → UI fragments (PlaylistFragment, NowPlayingFragment)\
           ├── models/              → Data classes (Song, Playlist, Album)\
           ├── services/            → Background services (MusicService)\
           ├── utils/               → Helper classes (MediaUtils, PermissionUtils)\
           ├── interfaces/          → Custom listeners/callbacks\
           └── MainApplication.java → App-level initialization


res/\
 ├── layout/          → XML layouts (activity_main.xml, fragment_playlist.xml, item_song.xml)\
 ├── drawable/        → Icons, shapes, backgrounds\
 ├── values/          → strings.xml, colors.xml, dimens.xml, styles.xml\
 ├── raw/             → Test media files (optional)\
 ├── mipmap/          → App launcher icons