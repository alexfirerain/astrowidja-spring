package ru.swetophor.astrowidjaspring.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
@AllArgsConstructor
public class AlbumInfo {
    private String name;
    private List<String> chartNames;
    long modified;
}
