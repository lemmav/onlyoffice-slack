package com.onlyoffice.slack.domain.document.editor.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DocumentDocumentFileKeyExtractorTests {
  @Test
  void whenValidKeyAndType_thenReturnsCorrectPart() {
    DocumentFileKeyExtractor extractor = (key, type) -> key.split("_")[type.ordinal()];
    var key = "file_team_user_id";

    assertEquals("file", extractor.extract(key, DocumentFileKeyExtractor.Type.FILE));
    assertEquals("team", extractor.extract(key, DocumentFileKeyExtractor.Type.TEAM));
    assertEquals("user", extractor.extract(key, DocumentFileKeyExtractor.Type.USER));
  }

  @Test
  void whenMalformedKey_thenThrowsException() {
    DocumentFileKeyExtractor extractor = (key, type) -> key.split("_")[type.ordinal()];
    var key = "file_team";

    assertThrows(
        ArrayIndexOutOfBoundsException.class,
        () -> extractor.extract(key, DocumentFileKeyExtractor.Type.USER));
  }
}
