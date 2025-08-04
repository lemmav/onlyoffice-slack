package com.onlyoffice.slack.service.document.helper;

import static org.junit.jupiter.api.Assertions.*;

import com.onlyoffice.slack.domain.document.editor.core.DocumentFileKeyBuilderImpl;
import org.junit.jupiter.api.Test;

class DocumentFileKeyBuilderTests {
  private final DocumentFileKeyBuilderImpl builder = new DocumentFileKeyBuilderImpl();

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
