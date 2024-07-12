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

    ChartList getAlbumSubstance(String filename);

    String addChartsToAlbum(ChartList table, String target);

    boolean addChartsToAlbum(String s, ChartObject... chartObject);

    String saveChartsAsAlbum(ChartList content, String albumName);

    String deleteAlbum(String groupToDelete);

    List<AlbumInfo> getLibrarySummery();

    List<AlbumInfo> getLibraryUpdates(long since);
}
