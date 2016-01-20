package org.telegram.services;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Localisation
 * @date 25/01/15
 */
public class LocalisationService {
    private static LocalisationService instance = null;
    private final HashMap<String, String> supportedLanguages = new HashMap<>();

    private ResourceBundle english;
    private ResourceBundle spanish;
    private ResourceBundle dutch;
    private ResourceBundle german;
    private ResourceBundle italian;
    private ResourceBundle french;
    private ResourceBundle malayalam;
    private ResourceBundle hindi;
    private ResourceBundle portuguese;
    private ResourceBundle portuguesebr;
    private ResourceBundle russian;
    private ResourceBundle arabic;
    private ResourceBundle catalan;
    private ResourceBundle galician;
    private ResourceBundle persian;
    private ResourceBundle turkish;
    private ResourceBundle esperanto;

    private class CustomClassLoader extends ClassLoader {
        public CustomClassLoader(ClassLoader parent) {
            super(parent);

        }

        public InputStream getResourceAsStream(String name) {
            InputStream utf8in = getParent().getResourceAsStream(name);
            if (utf8in != null) {
                try {
                    byte[] utf8Bytes = new byte[utf8in.available()];
                    utf8in.read(utf8Bytes, 0, utf8Bytes.length);
                    byte[] iso8859Bytes = new String(utf8Bytes, "UTF-8").getBytes("ISO-8859-1");
                    return new ByteArrayInputStream(iso8859Bytes);
                } catch (IOException e) {
                    e.printStackTrace();

                } finally {
                    try {
                        utf8in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }
    }

    /**
     * Singleton
     *
     * @return Instance of localisation service
     */
    public static LocalisationService getInstance() {
        if (instance == null) {
            synchronized (LocalisationService.class) {
                if (instance == null) {
                    instance = new LocalisationService();
                }
            }
        }
        return instance;
    }

    /**
     * Private constructor due to singleton
     */
    private LocalisationService() {
        CustomClassLoader loader = new CustomClassLoader(Thread.currentThread().getContextClassLoader());
        english = ResourceBundle.getBundle("localisation.strings", new Locale("en", "US"), loader);
        supportedLanguages.put("en", "English");
        spanish = ResourceBundle.getBundle("localisation.strings", new Locale("es", "ES"), loader);
        supportedLanguages.put("es", "Español");
        portuguese = ResourceBundle.getBundle("localisation.strings", new Locale("pt", "PT"), loader);
        supportedLanguages.put("pt", "Português");
        dutch = ResourceBundle.getBundle("localisation.strings", new Locale("nl", "NL"), loader);
        supportedLanguages.put("nl", "Nederlands");
        italian = ResourceBundle.getBundle("localisation.strings", new Locale("it", "IT"), loader);
        supportedLanguages.put("it", "Italiano");
        esperanto = ResourceBundle.getBundle("localisation.strings", new Locale("eo", "EO"), loader);
        supportedLanguages.put("eo", "Esperanto");
        /*
        german = ResourceBundle.getBundle("localisation.strings", new Locale("de", "DE"), loader);
        supportedLanguages.put("de", "Deutsch");
        italian = ResourceBundle.getBundle("localisation.strings", new Locale("it", "IT"), loader);
        supportedLanguages.put("it", "Italian");
        french = ResourceBundle.getBundle("localisation.strings", new Locale("fr", "FR"), loader);
        supportedLanguages.put("fr", "French");
        portuguesebr = ResourceBundle.getBundle("localisation.strings", new Locale("pt", "BR"), loader);
        supportedLanguages.put("pt_br", "Portuguese BR");*/
        /**
        malayalam = ResourceBundle.getBundle("localisation.strings", new Locale("ml", "ML"), loader);
        hindi = ResourceBundle.getBundle("localisation.strings", new Locale("hi", "HI"), loader);
        russian = ResourceBundle.getBundle("localisation.strings", new Locale("ru", "RU"), loader);
        arabic = ResourceBundle.getBundle("localisation.strings", new Locale("ar", "AR"), loader);
        catalan = ResourceBundle.getBundle("localisation.strings", new Locale("ca", "CA"), loader);
        galician = ResourceBundle.getBundle("localisation.strings", new Locale("gl", "ES"), loader);
        persian = ResourceBundle.getBundle("localisation.strings", new Locale("fa", "FA"), loader);
        turkish = ResourceBundle.getBundle("localisation.strings", new Locale("tr", "TR"), loader);
         */
    }

    /**
     * Get a string in default language (en)
     *
     * @param key key of the resource to fetch
     * @return fetched string or error message otherwise
     */
    public String getString(String key) {
        String result;
        try {
            result = english.getString(key);
        } catch (MissingResourceException e) {
            result = "String not found";
        }

        return result;
    }

    /**
     * Get a string in default language
     *
     * @param key key of the resource to fetch
     * @return fetched string or error message otherwise
     */
    public String getString(String key, String language) {
        String result;
        try {
            switch (language.toLowerCase()) {
                case "en":
                    result = english.getString(key);
                    break;
                case "es":
                    result = spanish.getString(key);
                    break;
                case "pt":
                    result = portuguese.getString(key);
                    break;
                case "nl":
                    result = dutch.getString(key);
                    break;
                case "it":
                    result = italian.getString(key);
                    break;
                case "eo":
                    result = esperanto.getString(key);
                    break;
                /*case "de":
                    result = german.getString(key);
                    break;
                case "fr":
                    result = french.getString(key);
                    break;
                case "ml":
                    result = malayalam.getString(key);
                    break;
                case "hi":
                    result = hindi.getString(key);
                    break;
                case "pt-BR":
                    result = portuguesebr.getString(key);
                    break;
                case "ru":
                    result = russian.getString(key);
                    break;
                case "ar":
                    result = arabic.getString(key);
                    break;
                case "ca":
                    result = catalan.getString(key);
                    break;
                case "gl":
                    result = galician.getString(key);
                    break;
                case "fa":
                    result = persian.getString(key);
                    break;
                case "tr":
                    result = turkish.getString(key);
                    break;*/
                default:
                    result = english.getString(key);
                    break;
            }
        } catch (MissingResourceException e) {
            result = english.getString(key);
        }

        return result;
    }

    public HashMap<String, String> getSupportedLanguages() {
        return supportedLanguages;
    }

    public String getLanguageCodeByName(String language) {
        return supportedLanguages.entrySet().stream().filter(x -> x.getValue().equals(language)).findFirst().get().getKey();
    }
}
