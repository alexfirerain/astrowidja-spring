package ru.swetophor.astrowidjaspring.service;

import org.springframework.stereotype.Service;
import ru.swetophor.astrowidjaspring.model.AspectTable;
import ru.swetophor.astrowidjaspring.model.AstroMatrix;
import ru.swetophor.astrowidjaspring.model.chart.ChartObject;
import ru.swetophor.astrowidjaspring.model.PatternTable;

import java.util.HashMap;
import java.util.Map;

@Service
public class HarmonicService {
    private final Map<ChartObject, AstroMatrix> matrices = new HashMap<>();

    private AstroMatrix getMatrix(ChartObject chartObject) {
        matrices.putIfAbsent(chartObject, new AstroMatrix(chartObject.getData()));
        return matrices.get(chartObject);
    }

    public PatternTable calculatePatternTable(ChartObject chartObject) {
        return getMatrix(chartObject).buildPatternTable();
    }

    public AspectTable calculateAspectTable(ChartObject chartObject) {
        return getMatrix(chartObject).buildAspectTable();
    }

}
