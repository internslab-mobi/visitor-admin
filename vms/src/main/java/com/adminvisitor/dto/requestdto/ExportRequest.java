
package com.adminvisitor.dto.requestdto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ExportRequest {

    private String fileName;
    private String format;
    private String title;
    private List<ExportColumn> columns;
    private List<Map<String, Object>> data;

    @Getter
    @Setter
    public static class ExportColumn {

        private String key;
        private String header;
    }
}
