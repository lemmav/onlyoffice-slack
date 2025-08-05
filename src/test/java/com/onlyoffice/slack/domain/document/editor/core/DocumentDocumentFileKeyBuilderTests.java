package com.onlyoffice.slack.domain.document.editor.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DocumentDocumentFileKeyBuilderTests {
  @Test
  void whenValidInput_thenReturnsConcatenatedKey() {
    DocumentFileKeyBuilder builder =
        (fileId, teamId, userId, uuid) -> fileId + "_" + teamId + "_" + userId + "_" + uuid;

    var result = builder.build("file", "team", "user", "id");

    assertEquals("file_team_user_id", result);
  }

  @Test
  void whenAnyInputIsBlank_thenThrowsException() {
    DocumentFileKeyBuilder builder =
        (fileId, teamId, userId, uuid) -> {
          if (fileId.isBlank() || teamId.isBlank() || userId.isBlank() || uuid.isBlank())
            throw new IllegalArgumentException("Arguments must not be blank");
          return fileId + teamId + userId + uuid;
        };
    assertThrows(IllegalArgumentException.class, () -> builder.build("", "team", "user", "id"));
    assertThrows(IllegalArgumentException.class, () -> builder.build("file", "", "user", "id"));
    assertThrows(IllegalArgumentException.class, () -> builder.build("file", "team", "", "id"));
    assertThrows(IllegalArgumentException.class, () -> builder.build("file", "team", "user", ""));
  }
}
