package life.genny.rules;

import java.util.concurrent.ConcurrentHashMap;

public class RulesLoaderFactory {
    private static final ConcurrentHashMap<String, RulesLoader> tokeRulesLoaderMapping = new ConcurrentHashMap<>();

    public RulesLoaderFactory() {
    }

    public static RulesLoader getRulesLoader(String sessionState) {
        RulesLoader rulesLoader = tokeRulesLoaderMapping.get(sessionState);
        if (rulesLoader == null) {
            rulesLoader = new RulesLoader(sessionState);
            tokeRulesLoaderMapping.put(sessionState, rulesLoader);
        }
        return rulesLoader;
    }
}

