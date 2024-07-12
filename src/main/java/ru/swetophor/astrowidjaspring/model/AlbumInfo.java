package ru.swetophor.astrowidjaspring.model;

import java.util.List;


/**
 * Представляет информацию об альбоме: его название, названия карт в нём
 * и метку времени последнего изменения.
 *
 * @param name          название альбома.
 * @param chartNames    перечень названий карт, находящихся в альбоме.
 * @param modified      временная метка свежайшего изменения альбома.
 */
public record AlbumInfo(
        String name,
        List<String> chartNames,
        long modified
) {
}
