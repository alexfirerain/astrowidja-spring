package ru.swetophor.astrowidjaspring.repository;

import ru.swetophor.astrowidjaspring.model.AlbumInfo;
import ru.swetophor.astrowidjaspring.model.chart.ChartList;
import ru.swetophor.astrowidjaspring.model.chart.ChartObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public interface ChartRepository {

    static String newAutosaveName() {
        return "сохранение %s.awb"
                .formatted(new SimpleDateFormat("E d MMMM .yy HH-mm")
                        .format(new Date()));
    }

    List<String> albumNames();

    List<String> getAlbumContents(String albumName);

    ChartList getAlbumSubstance(String filename);

    List<ChartList> getAllAlbums();

    String addChartsToAlbum(ChartList table, String target);

    boolean addChartsToAlbum(String s, ChartObject... chartObject);

    String saveChartsAsAlbum(ChartList desk, String s);

    String deleteAlbum(String groupToDelete);

    AlbumInfo getAlbumSummary(String filename);

    List<AlbumInfo> getLibrarySummery();

    List<AlbumInfo> getLibraryUpdates(long since);
}
