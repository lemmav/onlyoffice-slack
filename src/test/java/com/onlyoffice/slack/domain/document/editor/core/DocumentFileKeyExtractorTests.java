package com.onlyoffice.slack.domain.document.editor.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DocumentFileKeyExtractorTests {
  private final DocumentFileKeyExtractorImpl extractor = new DocumentFileKeyExtractorImpl();

  @Test
  void whenTypeIsFile_thenReturnsFileId() {
    var key = "file_team_user_id";

    assertEquals("file", extractor.extract(key, DocumentFileKeyExtractor.Type.FILE));
  }

  @Test
  void whenTypeIsTeam_thenReturnsTeamId() {
    var key = "file_team_user_id";

    assertEquals("team", extractor.extract(key, DocumentFileKeyExtractor.Type.TEAM));
  }

  @Test
  void whenTypeIsUser_thenReturnsUserId() {
    var key = "file_team_user_id";

    assertEquals("user", extractor.extract(key, DocumentFileKeyExtractor.Type.USER));
  }

  @Test
  void whenTypeIsNull_thenThrowsNullPointerException() {
    var key = "file_team_user_id";

    assertThrows(NullPointerException.class, () -> extractor.extract(key, null));
  }

  @Test
  void whenMalformedKey_thenReturnsNull() {
    var key = "file_team_user";

    assertNull(extractor.extract(key, DocumentFileKeyExtractor.Type.FILE));
  }
}
