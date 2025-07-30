package com.onlyoffice.slack.service.document.helper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FileKeyBuilderTests {
  private final FileKeyBuilder builder = new FileKeyBuilder();

  @Test
  void whenValidInput_thenReturnsFormattedKey() {
    var fileId = "file";
    var teamId = "team";
    var userId = "user";
    var uuid = "id";
    var result = builder.build(fileId, teamId, userId, uuid);

    assertTrue(result.startsWith("file_team_user_"));
    assertEquals(4, result.split("_").length);
  }
}
