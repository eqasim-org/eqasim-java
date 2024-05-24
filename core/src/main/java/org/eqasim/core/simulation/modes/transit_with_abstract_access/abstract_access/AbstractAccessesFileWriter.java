package org.eqasim.core.simulation.modes.transit_with_abstract_access.abstract_access;

import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AbstractAccessesFileWriter extends MatsimXmlWriter {
    private final Collection<AbstractAccessItem> accessItems;
    public AbstractAccessesFileWriter(Collection<AbstractAccessItem> accessItems) {
        this.accessItems = accessItems;
    }

    public void write(String file) {
        this.openFile(file);
        this.writeStartTag(AbstractAccessesFileReader.ROOT_TAG_NAME, Collections.emptyList());
        this.accessItems.forEach(this::writeAccessItem);
        this.writeEndTag(AbstractAccessesFileReader.ROOT_TAG_NAME);
        this.close();
    }

    private synchronized void writeAccessItem(AbstractAccessItem accessItem) {
        List<Tuple<String, String>> attributes = new ArrayList<>();
        attributes.add(Tuple.of(AbstractAccessesFileReader.ACCESS_ID_ATTR_NAME, accessItem.getId().toString()));
        attributes.add(Tuple.of(AbstractAccessesFileReader.TRANSIT_STOP_ID_ATTR_NAME, accessItem.getCenterStop().getId().toString()));
        attributes.add(Tuple.of(AbstractAccessesFileReader.RADIUS_ATTR_NAME, String.valueOf(accessItem.getRadius())));
        attributes.add(Tuple.of(AbstractAccessesFileReader.AVG_SPEED_ATTR_NAME, String.valueOf(accessItem.getAvgSpeedToCenterStop())));
        attributes.add(Tuple.of(AbstractAccessesFileReader.USING_ROUTED_DISTANCE_ATTR_NAME, String.valueOf(accessItem.isUsingRoutedDistance())));
        attributes.add(Tuple.of(AbstractAccessesFileReader.ACCESS_TYPE_ATTR_NAME, accessItem.getAccessType()));
        attributes.add(Tuple.of(AbstractAccessesFileReader.FREQUENCY_ATTR_NAME, String.valueOf(accessItem.getFrequency())));
        this.writeStartTag(AbstractAccessesFileReader.ABSTRACT_ACCESS_TAG_NAME, attributes, true);
    }
}
