package cl.feliminish.model;

// orden e info que leera retrotax de las canciones obtenidas
public class Song {
    private final String title;
    private final String artist;
    private final String album;

    public Song(String title, String artist, String album) {
        this.title = title;
        this.artist = artist;
        this.album = album;
    }

    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
}