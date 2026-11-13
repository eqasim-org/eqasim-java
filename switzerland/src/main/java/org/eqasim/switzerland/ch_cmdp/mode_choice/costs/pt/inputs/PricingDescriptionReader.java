package org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.DefaultDistanceBasedCalculator;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.PtStageCostCalculator;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.SBBPtStageCostCalculator;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.SwissPtStageCostCalculator;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.ZoneBasedPricing;


public class PricingDescriptionReader {

    public record AuthorityState(boolean inAuthority,
                             boolean inCentralZoneBasedAuthority) {}

    public static SwissPtStageCostCalculator readPriceDescription(File xmlFile) throws Exception {
        return readPriceDescription(xmlFile.toURI().toURL());
    }

    /** The fares of a pricing description, also from inside a jar (a resource). */
    public static SwissPtStageCostCalculator readPriceDescription(URL xmlUrl) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        XMLReader reader = parser.getXMLReader();

        final DefaultDistanceBasedCalculator defaultPricing = new DefaultDistanceBasedCalculator();
        final SBBPtStageCostCalculator sbbPricing = new SBBPtStageCostCalculator();
        final Map<String, ZoneBasedPricing> authorities = new HashMap<>();

        reader.setContentHandler(new DefaultHandler() {
            private boolean inDefault = false;
            private boolean inSBB = false;
            private boolean inAuthority = false;
            private boolean inCentralZoneBasedAuthority = false;
            private List<double[]> distancePriceBuffer = null;
            private ZoneBasedPricing authorityPricing = null;

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) {
                inDefault   = readDefaultPricingDescription(uri, localName, qName, attributes, inDefault, defaultPricing);
                inSBB       = readSbbPricingDescription(uri, localName, qName, attributes, inSBB, sbbPricing);
                AuthorityState authState = readAuthority(uri, localName, qName, attributes, inAuthority, inCentralZoneBasedAuthority); 
                inAuthority = authState.inAuthority;
                inCentralZoneBasedAuthority = authState.inCentralZoneBasedAuthority;               
            }

            @Override
            public void endElement(String uri, String localName, String qName) {
                if (qName.equals("distanceBasedDefault")) {
                    inDefault = false;
                    //System.out.println("    Parsed default pricing");
                }
                if (qName.equals("sbb")) {
                    inSBB = false;
                    //System.out.println("    Parsed SBB");
                }
                if (qName.equals("authority")) {
                    inAuthority = false;
                    inCentralZoneBasedAuthority = false;
                    if (authorityPricing == null) {
                        throw new IllegalStateException("An authority of the pricing description has no name");
                    }
                    authorityPricing.checkPrices();
                    authorities.put(authorityPricing.getAuthority(), authorityPricing);
                    //System.out.println("    Parsed authority: " + authorityPricing.getAuthority());
                    authorityPricing = null;
                }
            }


            private boolean readSbbPricingDescription(String uri, String localName, String qName, Attributes attributes, boolean inSBB, SBBPtStageCostCalculator sbbPricing){
                if (qName.equals("sbb")){
                    inSBB = true;
                }

                if (inSBB){
                    switch (qName) {
                        case "minimumPrice":
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


            private boolean readDefaultPricingDescription(String uri, String localName, String qName, Attributes attributes, boolean inDefault, DefaultDistanceBasedCalculator defaultPricing){
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


            @SuppressWarnings("null")
            private AuthorityState readAuthority(String uri, String localName, String qName, Attributes attributes, boolean inAuthority, boolean inCentralZoneBasedAuthority){
                if (qName.equals("authority") && !inAuthority){
                    inAuthority = true;
                }

                if (inAuthority){
                    // Each case ends with a break: an element of the other kind of network is ignored.
                    boolean halbtax = "true".equals(attributes.getValue("halbtax"));
                    switch (qName){
                        case "name":
                            authorityPricing = new ZoneBasedPricing(attributes.getValue("value"));
                            authorityPricing.setDefaultCalculator(defaultPricing);
                            break;
                        case "zonalBased":
                            inCentralZoneBasedAuthority = "centerbased".equals(attributes.getValue("value"));
                            break;
                        case "priceListByZone":
                            if (!inCentralZoneBasedAuthority){
                                String pricesAttr    = attributes.getValue("prices");
                                double[] prices      = Arrays.stream(pricesAttr.split(",")).map(String::trim).mapToDouble(Double::parseDouble).toArray();
                                if (halbtax){
                                    authorityPricing.setHalbTaxPriceList(prices);
                                }
                                else{
                                    authorityPricing.setFullPriceList(prices);
                                }
                            }
                            break;
                        case "zonesCountingAsTwo":
                            if (!inCentralZoneBasedAuthority){
                                String zones = attributes.getValue("zones");
                                Set<String> parsedZones = Arrays.stream(zones.split(",")).map(String::trim).collect(Collectors.toSet());
                                authorityPricing.setDoubleCountingZones(parsedZones);
                            }
                            break;
                        case "centerZoneID":
                            if (inCentralZoneBasedAuthority){
                                authorityPricing.setCenterZoneID(attributes.getValue("zone"));
                            }
                            break;
                        case "priceCenterZone":
                            // the full price, and the half-fare one (halbtax="true"); without one of them, the other
                            if (inCentralZoneBasedAuthority){
                                double price = Double.parseDouble(attributes.getValue("price"));
                                if (halbtax) authorityPricing.setPriceInCenterZoneHalbtax(price);
                                else authorityPricing.setPriceInCenterZone(price);
                            }
                            break;
                        case "priceAdditionalZone":
                            if (inCentralZoneBasedAuthority){
                                double price = Double.parseDouble(attributes.getValue("price"));
                                if (halbtax) authorityPricing.setPriceInAdditionalZoneHalbtax(price);
                                else authorityPricing.setPriceInAdditionalZone(price);
                            }
                            break;
                        case "targetedPricing":
                            double discount  = Double.parseDouble(attributes.getValue("discount"));
                            String age_range = attributes.getValue("age_range");
                            if (age_range != null && age_range.contains("_")) {
                                String[] parts = age_range.split("_");
                                if (parts.length == 2) {
                                    try {
                                        int min_age = Integer.parseInt(parts[0].trim());
                                        int max_age = Integer.parseInt(parts[1].trim());
                                        authorityPricing.setAgeSpecificDiscount(min_age, max_age, discount);
                                    } catch (NumberFormatException e) {
                                        System.err.println("Invalid age range format: " + age_range);
                                    }
                                }
                            }
                            break;
                        default:
                            break;
                    }
                }

                return new AuthorityState(inAuthority, inCentralZoneBasedAuthority);
            }
        });

        reader.parse(xmlUrl.toString());

        Map<String, PtStageCostCalculator> calculators = new HashMap<>();
        calculators.put("SBB", sbbPricing);
        calculators.put("None", defaultPricing);

        for (Map.Entry<String, ZoneBasedPricing> entry : authorities.entrySet()){
            String authority = entry.getKey();
            ZoneBasedPricing pricing = entry.getValue();
            calculators.put(authority, pricing);
        }

        SwissPtStageCostCalculator newSwissPtStageCostCalculator = new SwissPtStageCostCalculator(calculators);

        return newSwissPtStageCostCalculator;
    }
}
