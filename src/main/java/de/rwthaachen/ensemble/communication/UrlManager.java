package de.rwthaachen.ensemble.communication;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by fp on 5/9/16.
 */
public class UrlManager {

    static {
        updateUrlTable();
        System.out.println(printState());
        //System.exit(0);
    }

    public enum Backends {
        WEBQA, YODAQA, RAKE, BLUEMIX, WATSON, OAQA, OPENEPHYRA, QANTA, OPENQA, QANUS
    }

    private static String[][] urlLookUpTable;

    public static String[][] getUrlLookUpTable() {
        return urlLookUpTable;
    }
    public static void setUrlLookUpTable(String[][] newLookUpTable) {
        urlLookUpTable = newLookUpTable;
    }

    private static int currentBackend = 0; //Default is 0: ailao.eu, 1: loopback, different if custom tables loaded
    public static int getCurrentBackend() {
        return currentBackend;
    }
    public static boolean setCurrentBackend(int currentBackend) {
        if (currentBackend < urlLookUpTable.length) {
            UrlManager.currentBackend = currentBackend;
            return true;
        } else {
            return false;
        }
    }

    public static String printState() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Currently selected backend: ");
        stringBuilder.append(Integer.toString(currentBackend));
        stringBuilder.append("\n\nCurrent Lookup Table:\n");
        for (int i=0; i<urlLookUpTable.length; i++){
            for(int j=0; j<urlLookUpTable[i].length; j++) {
                stringBuilder.append(urlLookUpTable[i][j]);
                stringBuilder.append(" ");
            }
            stringBuilder.append("\n");
        }
        stringBuilder.append("\nCurrent properties:");
        stringBuilder.append("System.getProperty(\"de.rwthaachen.ensemble.webqaurl\") ");
        stringBuilder.append(System.getProperty("de.rwthaachen.ensemble.webqaurl"));
        stringBuilder.append("\nSystem.getProperty(\"de.rwthaachen.ensemble.yodaqaurl\") ");
        stringBuilder.append(System.getProperty("de.rwthaachen.ensemble.yodaqaurl"));
        stringBuilder.append("\nSystem.getProperty(\"de.rwthaachen.ensemble.rakeurl\") ");
        stringBuilder.append(System.getProperty("de.rwthaachen.ensemble.rakeurl"));
        stringBuilder.append("\nSystem.getProperty(\"de.rwthaachen.ensemble.bluemixurl\") ");
        stringBuilder.append(System.getProperty("de.rwthaachen.ensemble.bluemixurl"));
        stringBuilder.append("\n\nURL each backend receives:");
        stringBuilder.append("\nWebQA: ");
        stringBuilder.append(lookUpUrl(Backends.WEBQA.ordinal()));
        stringBuilder.append("\nYodaQA: ");
        stringBuilder.append(lookUpUrl(Backends.YODAQA.ordinal()));
        stringBuilder.append("\nRAKE: ");
        stringBuilder.append(lookUpUrl(Backends.RAKE.ordinal()));
        stringBuilder.append("\nBluemix: ");
        stringBuilder.append(lookUpUrl(Backends.BLUEMIX.ordinal()));
        return stringBuilder.toString();
    }

    /**
     * Reads configuration file
     * @return Raw content of configuration file (should be JSON)
     */
    private static String readConfigurationFile() {
        StringBuilder configurationJson = new StringBuilder();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader("conf/backendUrls.json")))
        {
            String currentLine;

            while ((currentLine = bufferedReader.readLine()) != null) {
                configurationJson.append(currentLine);
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return configurationJson.toString();
    }

    /**
     * Parses configuration to two-dimensional String array, where each subarray contains
     * DBpedia, Freebase, Label Service 1, Label Service 2, Solr/enwiki
     * @return Two-dimensional array with URLs
     */
    private static String[][] parseConfiguration(String jsonString) {

        Gson gson = new GsonBuilder().create();

        // Could get it as list or proceed manually...
        // List<String> backends = gson.fromJson(jsonObject.get("offline"), new TypeToken<List<String>>(){}.getType());
        //JsonParser jsonParser  = new JsonParser();
        //JsonElement jsonElement = jsonParser.parse(jsonString);
        // JsonArray  jsonArray = jsonElement.isJsonArray()?jsonElement.getAsJsonArray():null;

        // ...but we can load it directly
        return gson.fromJson(jsonString, String[][].class);
    }

    /**
     * Reads and parses configuration file
     * @return Success state
     */
    public static boolean updateUrlTable() {
        try{
            String[][] result = parseConfiguration(readConfigurationFile());
            if (result != null) {
                urlLookUpTable = result;
                return true;
            }
            return false;
        } catch (JsonParseException jpe) {
            jpe.printStackTrace();
            return false;
        }
    }

    /**
     * Looks up entire URL set for a backend (DBpedia, Freebase, Label Service 1, Label Service 2
     * and then Solr/enwiki)
     * @param id Backend to return all URLs for
     * @return All backends in order
     */
    public static String[] lookUpUrlSet(int id) {
        if(urlLookUpTable != null && id<urlLookUpTable.length) {
            return urlLookUpTable[id];
        }
        return null;
    }

    /**
     * Looks up particular URL in current backend, used by cz.brmlab.yodaqa.provider.rdf.FreebaseLookup,
     * cz.brmlab.yodaqa.provider.rdf.DBpediaTitles.java cz.brmlab.yodaqa.provider.rdf.DBpediaLookup and
     * cz.brmlab.yodaqa.pipeline.YodaQA
     * @param fieldId Field to lookup
     * @return URL for that particular backend
     */
    public static String lookUpUrl(int fieldId) {
        switch (fieldId) {
            case 0:
                return System.getProperty("de.rwthaachen.ensemble.webqaurl")!=null?
                        System.getProperty("de.rwthaachen.ensemble.webqaurl"):lookUpUrl(currentBackend, fieldId);
            case 1:
                return System.getProperty("de.rwthaachen.ensemble.yodaqaurl")!=null?
                        System.getProperty("de.rwthaachen.ensemble.yodaqaurl"):lookUpUrl(currentBackend, fieldId);
            case 2:
                return System.getProperty("de.rwthaachen.ensemble.rakeurl")!=null?
                        System.getProperty("de.rwthaachen.ensemble.rakeurl"):lookUpUrl(currentBackend, fieldId);
            case 3:
                return System.getProperty("de.rwthaachen.ensemble.bluemixurl")!=null?
                        System.getProperty("de.rwthaachen.ensemble.bluemixurl"):lookUpUrl(currentBackend, fieldId);
            default:
                return null;
        }
    }

    /**
     * Looks up URL by set and field
     * @param fieldId Field to lookup
     * @param setId URL set to use, number is position is JSON configuration under conf/backendURLs.json
     * @return URL for that particular set and field
     */
    public static String lookUpUrl(int setId, int fieldId) {
        if(urlLookUpTable != null && setId<urlLookUpTable.length &&
                fieldId<urlLookUpTable[setId].length) {
            return urlLookUpTable[setId][fieldId];
        }
        return null;
    }

}