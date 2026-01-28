package org.eqasim.switzerland.ch_cmdp.utils.pricing.inputs;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.DefaultDistanceBasedCalculator;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.PtStageCostCalculator;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.SBBPtStageCostCalculator;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.SwissPtStageCostCalculator;


public class PricingDescriptionReader {

    public static boolean readDefaultPricingDescription(String uri, String localName, String qName, Attributes attributes, boolean inDefault, DefaultDistanceBasedCalculator defaultPricing){
        if (qName.equals("distanceBasedDefault")){
                    inDefault = true;

                }

        if (inDefault){
            switch (qName) {
                case "flatRatePrice":
                    defaultPricing.setFlatRatePrice(Double.parseDouble(attributes.getValue("value")));
                    break;
                case "pricePerKM":
                    defaultPricing.setPricePerKM(Double.parseDouble(attributes.getValue("value")));
                    break;
                case "power2Factor":
                    defaultPricing.setPower2Term(Double.parseDouble(attributes.getValue("value")));
                    break;
            
                default:
                    break;
            }
        }
        return inDefault;
    }    


    public static SwissPtStageCostCalculator readPriceDescription(File xmlFile) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        XMLReader reader = parser.getXMLReader();

        final DefaultDistanceBasedCalculator defaultPricing = new DefaultDistanceBasedCalculator();
        final SBBPtStageCostCalculator sbbPricing = new SBBPtStageCostCalculator();

        reader.setContentHandler(new DefaultHandler() {
            private boolean inDefault = false;
            private boolean inSBB = false;
            private List<double[]> distancePriceBuffer = null;

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) {
                inDefault = readDefaultPricingDescription(uri, localName, qName, attributes, inDefault, defaultPricing);
                inSBB     = readSbbPricingDescription(uri, localName, qName, attributes, inSBB, sbbPricing);
                
            }

            @Override
            public void endElement(String uri, String localName, String qName) {
                if (qName.equals("distanceBasedDefault")) {
                    inDefault = false;
                    System.out.println("Parsed default pricing");
                }
                if (qName.equals("sbb")) {
                    inSBB = false;
                    System.out.println("Parsed SBB");
                }
            }

            private boolean readSbbPricingDescription(String uri, String localName, String qName, Attributes attributes, 
                boolean inSBB, SBBPtStageCostCalculator sbbPricing){
                if (qName.equals("sbb")){
                    inSBB = true;
                }

                if (inSBB){
                    switch (qName) {
                        case "minimunPrice":
                            sbbPricing.setMinimumPrice(Double.parseDouble(attributes.getValue("value")));
                            break;
                        case "minimumPriceHalbtax":
                            sbbPricing.setMinimumPriceHalbtax(Double.parseDouble(attributes.getValue("value")));
                            break;
                        case "priceByKMByDistanceBin":
                            boolean isFirst = "true".equals(attributes.getValue("first"));
                            boolean isLast  = "true".equals(attributes.getValue("last"));

                            if (isFirst) {
                                distancePriceBuffer = new ArrayList<>();
                            }

                            if (distancePriceBuffer == null) {
                                throw new IllegalStateException("Encountered distance bin before first=true");
                            }
                            double min   = Double.parseDouble(attributes.getValue("distance_min"));
                            double max   = Double.parseDouble(attributes.getValue("distance_max"));
                            double price = Double.parseDouble(attributes.getValue("priceByKM"));
                            distancePriceBuffer.add(new double[]{min, max, price});

                            if (isLast) {
                                sbbPricing.setDistancePrices(distancePriceBuffer.toArray(new double[distancePriceBuffer.size()][3]));
                                distancePriceBuffer = null;
                            }
                    }
                }

                return inSBB;
            }

        });

        reader.parse(xmlFile.toURI().toString());

        Map<String, PtStageCostCalculator> calculators = new HashMap<>();
        calculators.put("SBB", sbbPricing);
        calculators.put("None", defaultPricing);

        SwissPtStageCostCalculator newSwissPtStageCostCalculator = new SwissPtStageCostCalculator(calculators);

        return newSwissPtStageCostCalculator;
    }
}
