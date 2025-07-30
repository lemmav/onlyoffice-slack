package com.onlyoffice.slack.service.document.helper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FileKeyExtractorTests {
  private final FileKeyExtractor extractor = new FileKeyExtractor();

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
