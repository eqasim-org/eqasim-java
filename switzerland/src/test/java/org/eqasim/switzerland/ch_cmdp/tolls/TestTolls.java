package org.eqasim.switzerland.ch_cmdp.tolls;

import org.junit.Test;
import org.matsim.api.core.v01.network.Link;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class TestTolls {
    @Test
    public void readsNumericAttributeAndReturnsZeroWhenAbsent() {
        var link = mock(Link.class);
        var attributes = new AttributesImpl();
        when(link.getAttributes()).thenReturn(attributes);
        var tolls = new Tolls();
        assertEquals(0.0, tolls.getToll(link), 0.0);
        for (Number price : new Number[]{2.5, 3, 1.25F, 0.0}) {
            attributes.putAttribute("toll", price);
            assertEquals(price.doubleValue(), tolls.getToll(link), 0.0);
        }
        attributes.removeAttribute("toll");
        assertEquals(0.0, tolls.getToll(link), 0.0);
    }
}
