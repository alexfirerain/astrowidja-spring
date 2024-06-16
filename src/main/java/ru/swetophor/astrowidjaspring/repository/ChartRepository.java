package ru.swetophor.astrowidjaspring.repository;

import ru.swetophor.astrowidjaspringshell.model.ChartList;
import ru.swetophor.astrowidjaspringshell.model.ChartObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public interface ChartRepository {

    static String newAutosaveName() {
        return "сохранение %s.awb"
                .formatted(new SimpleDateFormat("E d MMMM .yy HH-mm")
                        .format(new Date()));
    }

    boolean albumExists(String albumName);

    List<String> albumNames();

    List<String> getAlbumContents(String albumName);

    ChartList getAlbumSubstance(String filename);

    List<ChartList> getAllAlbums();

    String addChartsToAlbum(ChartList table, String target);

    boolean addChartsToAlbum(String s, ChartObject... chartObject);

    void saveChartsAsAlbum(ChartList desk, String s);

    String deleteAlbum(String groupToDelete);
}
