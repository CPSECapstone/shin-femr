package femr.data.daos.system;

import femr.common.models.CSVImportRowItem;
import femr.data.daos.core.ICSVImportCacheRepository;

import java.util.ArrayList;

public class CSVImportCacheRepository implements ICSVImportCacheRepository {
    private ArrayList<CSVImportRowItem> csvImportRowItemsCache;

    public CSVImportCacheRepository() {
        this.csvImportRowItemsCache = new ArrayList<>();
    }

    @Override
    public void addToCache(CSVImportRowItem csvImportRowItem) {
        this.csvImportRowItemsCache.add(csvImportRowItem);
    }

    @Override
    public void clearCache() {
        this.csvImportRowItemsCache.clear();
    }
}
