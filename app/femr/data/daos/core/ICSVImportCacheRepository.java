package femr.data.daos.core;

import femr.common.models.CSVImportRowItem;

public interface ICSVImportCacheRepository {
    void addToCache(CSVImportRowItem csvImportRowItem);
    void clearCache();
}
