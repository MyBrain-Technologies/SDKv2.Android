package utils;

import android.content.Context;
import android.content.res.Configuration;

import java.util.Locale;

/**
 * Created by Etienne on 30/12/2016.
 */

public class LanguageUtils {
    private static Locale myLocale;
    public static void setLocale(Context context, String lang) {
        if (lang.equalsIgnoreCase(""))
            return;
        myLocale = Locale.forLanguageTag(lang);
        //Means that lang in parameters is an unknown iso format and need to be parsed manually
        if(myLocale.getLanguage().equalsIgnoreCase("und")){
            myLocale = new Locale(parseLanguageToIso(lang));
        }
        Locale.setDefault(myLocale);
        Configuration config = new Configuration();
        config.locale = myLocale;
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }

    //TODO Proper way using the array resource + completion
    private static String parseLanguageToIso(String language) {
        String isoLanguage;
        switch(language){
            case "Français":
            case "fr_FR":
                isoLanguage = "fr";
                break;
            case "English":
            case "en_GB":
            case "en_US":
            case "en_UK":
                isoLanguage = "en";
                break;
            default:
                isoLanguage = "en";
                break;
        }
        return isoLanguage;
    }
    //TODO Proper way using the array resource + completion
    public static String parseLanguageFromIso(String isoLanguage) {
        String language;
        switch(isoLanguage){
            case "fr":
            case "fr-FR":
                language = "Français";
                break;
            case "en":
            case "en-UK":
            case "en-GB":
            case "en-US":
                language = "English";
                break;
            default:
                language = "English";
                break;
        }
        return language;
    }

}
