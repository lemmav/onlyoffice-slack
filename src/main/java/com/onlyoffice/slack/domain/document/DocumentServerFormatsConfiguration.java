package com.onlyoffice.slack.domain.document;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.manager.document.DefaultDocumentManager;
import com.onlyoffice.model.common.Format;
import java.io.IOException;
import java.util.List;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DocumentServerFormatsConfiguration {
  private static final String DOCS_FORMATS_JSON_PATH =
      "document-formats/onlyoffice-docs-formats.json";

  private static final List<Format> formats;

  static {
    var objectMapper = new ObjectMapper();

    var inputStream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(DOCS_FORMATS_JSON_PATH);

    if (inputStream == null)
      inputStream =
          DefaultDocumentManager.class.getClassLoader().getResourceAsStream(DOCS_FORMATS_JSON_PATH);

    try {
      formats =
          objectMapper
              .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
              .readValue(inputStream, new TypeReference<List<Format>>() {});
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public List<Format> getFormats() {
    return formats;
  }
}
