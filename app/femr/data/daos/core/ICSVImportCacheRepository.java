package femr.data.daos.core;

import femr.common.models.CSVImportRowItem;

import java.util.ArrayList;

public interface ICSVImportCacheRepository {
    void setCache(ArrayList<CSVImportRowItem> csvImportRowItems);
    ArrayList<CSVImportRowItem> getCache();
    void clearCache();
}
